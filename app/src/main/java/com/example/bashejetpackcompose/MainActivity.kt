package com.example.bashejetpackcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.junit.Test
import org.junit.Assert.assertEquals

class Deck(var deckCount: Int)

class Player {
    fun takeFromDeck(deck: Deck, count: Int) {
        deck.deckCount = deck.deckCount - count
    }
}

class Bot(private val generator: Generatable<Int>) {
    fun takeFromDeck(deck: Deck)  {
        while (true) {
            val value = generator.generate()
            if (value <= deck.deckCount) {
                deck.deckCount -= value
                break
            }
        }
    }
}

interface Generatable<T> {
    fun generate() : T
}

class TestIntGenerator: Generatable<Int> {
    override fun generate() : Int {
        return 1
    }
}

class IntGenerator(private val ceil: Int) : Generatable<Int> {
    override fun generate() : Int {
        return Random.nextInt(1, ceil)
    }
}

interface EntityValidationInterface<T> {
    fun validate(entity: T?) : Boolean
}

class ItemsTakenValidator : EntityValidationInterface<Int> {
    override fun validate(entity: Int?) : Boolean =
        entity != null && entity in 1..3
}

class Game(
    val deck: Deck,
    val bot: Bot,
    val player: Player) {

    fun isDeckEnded() : Boolean = deck.deckCount == 0

    // return - 1 if player, 2 if bot and null if no winner
    fun makeAMove(count: Int) : Int? {
        player.takeFromDeck(deck, count)
        if (isDeckEnded()) {
            return 1
        }
        bot.takeFromDeck(deck)
        if (isDeckEnded()) {
            return 2
        }
        return null
    }
}

class GameViewModel(private val game: Game) : ViewModel() {
    private val resultMutableStateFlow = MutableStateFlow<Pair<Int?, Int>>(null to game.deck.deckCount)
    val resultStateFlow = resultMutableStateFlow.asStateFlow()

    fun makeAMove(count: Int) {
        val res = game.makeAMove(count)
        viewModelScope.launch {
            resultMutableStateFlow.emit(res to game.deck.deckCount)
        }
    }
}


class MainActivity: ComponentActivity() {
    private val viewModel by lazy {
        GameViewModel(Game(Deck(15), Bot(TestIntGenerator()), Player()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameScreen(viewModel, ItemsTakenValidator())
        }
    }

    companion object {
        const val button1_tag = "BUTTON"
        const val number_tag = "NUMBER"
        const val result_tag = "RESULT"
    }
}

@Composable
fun DeckCountLabel(num: Int, modifier: Modifier = Modifier) {
    Text(stringResource(id = R.string.items_in_deck, num), modifier = modifier)
}

@Composable
fun WinnerLabel(winner: String, tag: String, modifier: Modifier = Modifier) {
    Text(winner, modifier = modifier.testTag(tag))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerInput(
    numberChanged: (Int?) -> Unit,
    text: String,
    label: String,
    tag: String,
    modifier: Modifier = Modifier) {
    var number by rememberSaveable { mutableStateOf(text) }
    TextField(
        value = number,
        onValueChange = {
            number = if (it.toIntOrNull() != null || it == "-") {
                numberChanged(it.toIntOrNull())
                it
            }
            else ""
        },
        label = {Text(label)},
        modifier = modifier.testTag(tag)
    )
}

@Composable
fun GameScreen(gameViewModel: GameViewModel, itemsTakenValidator: ItemsTakenValidator,
               modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {
        var isEnabled by rememberSaveable { mutableStateOf(true) }
        var num by rememberSaveable { mutableStateOf<Int?> (null) }
        var count by rememberSaveable { mutableStateOf(0) }
        var winnerNumber by rememberSaveable { mutableStateOf<Int?>(null) }

        val res = gameViewModel.resultStateFlow.collectAsState()
        if (res.value.first != null) {
            isEnabled = false
            winnerNumber = res.value.first
        }

        count = res.value.second
        DeckCountLabel(num = count)

        var text = ""

        PlayerInput({num = it}, text, stringResource(id = R.string.edit_hint), MainActivity.number_tag)

        Button(
            onClick = {
                val itemsTaken = num
                if (itemsTakenValidator.validate(itemsTaken) && itemsTaken!! <= count) {
                    gameViewModel.makeAMove(itemsTaken)
                }
                text = ""
            },
            enabled = isEnabled,
            modifier = modifier.testTag(MainActivity.button1_tag)
        ) {
            Text(stringResource(id = R.string.make_a_move))
        }

        if (winnerNumber == 1) WinnerLabel(winner = stringResource(id = R.string.player_win), MainActivity.result_tag)
        else if (winnerNumber == 2) WinnerLabel(winner = stringResource(id = R.string.bot_win), MainActivity.result_tag)
    }
}

class ItemsTakenValidatorTest {
    private val validator: EntityValidationInterface<Int> = ItemsTakenValidator()

    @Test
    fun validationNotValid() {
        val result = validator.validate(4)
        assertEquals(false, result)
    }

    @Test
    fun validationNotValidNull() {
        val result = validator.validate(null)
        assertEquals(false, result)
    }

    @Test
    fun validationValid() {
        val result = validator.validate(2)
        assertEquals(true, result)
    }
}

class GameDeckTests {
    private val game = Game(Deck(3), Bot(IntGenerator(4)), Player())

    @Test
    fun gameDeckNotEnded() {
        val result = game.isDeckEnded()
        assertEquals(false, result)
    }

    @Test
    fun gameDeckEnded() {
        game.deck.deckCount = 0
        val result = game.isDeckEnded()
        assertEquals(true, result)
        game.deck.deckCount = 3
    }
}

class GameTests {
    private val game = Game(Deck(15), Bot(TestIntGenerator()), Player())

    @Test
    fun playerMakeAMoveTest() {
        game.player.takeFromDeck(game.deck, 2)
        assertEquals(13, game.deck.deckCount)
        game.deck.deckCount = 15
    }

    @Test
    fun botMakeAMoveTest() {
        game.bot.takeFromDeck(game.deck)
        assertEquals(14, game.deck.deckCount)
        game.deck.deckCount = 15
    }

    @Test
    fun makeAMoveTest() {
        game.makeAMove(1)
        assertEquals(13, game.deck.deckCount)
        game.deck.deckCount = 15
    }
}

