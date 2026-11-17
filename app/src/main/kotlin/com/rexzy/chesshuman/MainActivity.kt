package com.rexzy.chesshuman

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.MoveGenerator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFF1A1A1A)) { GameScreen() }
            }
        }
    }
}

const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

val PIECE_TO_CHAR = mapOf(
    'r' to "\u265C", 'n' to "\u265E", 'b' to "\u265D",
    'q' to "\u265B", 'k' to "\u265A", 'p' to "\u265F",
    'R' to "\u2656", 'N' to "\u2658", 'B' to "\u2657",
    'Q' to "\u2655", 'K' to "\u2654", 'P' to "\u2659"
)

fun fenToBoard(fen: String): Array<CharArray> {
    val board = Array(8) { CharArray(8) { ' ' } }
    val rows = fen.split(" ").first().split("/")
    for (r in 0..7) {
        var c = 0
        for (ch in rows[r]) {
            if (ch.isDigit()) c += ch.digitToInt()
            else { if (c < 8) board[r][c] = ch; c++ }
        }
    }
    return board
}

data class HistoryEntry(val fen: String, val move: String)

@Composable
fun GameScreen() {
    var board by remember { mutableStateOf(Board()) }
    var flipped by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var legalTargets by remember { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }

    fun refreshLegal(from: Pair<Int, Int>?) {
        if (from == null) { legalTargets = emptyList(); return }
        val (r, c) = from
        val fromSquare = try { Square.valueOf(squareName(r, c)) } catch (e: Exception) { return }
        val moves = try {
            MoveGenerator.generateLegalMoves(board).filter { it.from == fromSquare }
        } catch (e: Exception) { emptyList() }
        legalTargets = moves.mapNotNull { mv ->
            val to = mv.to.name
            (8 - to[1].digitToInt()) to (to[0] - 'a')
        }
    }

    fun attemptMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        try {
            val fromSq = Square.valueOf(squareName(from.first, from.second))
            val toSq = Square.valueOf(squareName(to.first, to.second))
            val candidate = board.legalMoves().firstOrNull { it.from == fromSq && it.to == toSq } ?: return
            val fenBefore = board.fen
            board.doMove(candidate)
            history = history + HistoryEntry(fenBefore, candidate.toString())
        } catch (e: Exception) {
            // abaikan langkah tidak valid, jangan crash
        } finally {
            selected = null
            legalTargets = emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        ChessBoard(
            fen = board.fen,
            flipped = flipped,
            selected = selected,
            legalTargets = legalTargets,
            onTap = { r, c ->
                val piece = fenToBoard(board.fen)[r][c]
                if (piece == ' ') {
                    selected?.let { from ->
                        if (legalTargets.contains(r to c)) attemptMove(from, r to c)
                    }
                    selected = null; legalTargets = emptyList()
                } else {
                    val turn = board.sideToMove
                    val isWhitePiece = piece.isUpperCase()
                    val correctTurn = (turn == Side.WHITE && isWhitePiece) || (turn == Side.BLACK && !isWhitePiece)
                    if (correctTurn) {
                        selected = r to c
                        refreshLegal(r to c)
                    } else {
                        selected?.let { from ->
                            if (legalTargets.contains(r to c)) attemptMove(from, r to c)
                        }
                        selected = null; legalTargets = emptyList()
                    }
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { flipped = !flipped }) { Text("Flip") }
            Button(onClick = {
                board = Board(); selected = null; legalTargets = emptyList(); history = emptyList()
            }) { Text("Reset") }
            Button(enabled = history.isNotEmpty(), onClick = {
                val last = history.last()
                history = history.dropLast(1)
                board = Board()
                board.loadFromFen(last.fen)
                selected = null; legalTargets = emptyList()
            }) { Text("Undo") }
        }
    }
}

fun squareName(r: Int, c: Int): String {
    // chesslib pakai HURUF BESAR: E2, bukan e2
    return "${('A' + c)}${8 - r}"
}

@Composable
fun ChessBoard(
    fen: String,
    flipped: Boolean,
    selected: Pair<Int, Int>?,
    legalTargets: List<Pair<Int, Int>>,
    onTap: (Int, Int) -> Unit
) {
    val board = fenToBoard(fen)
    val lightColor = Color(0xFFF0D9B5)
    val darkColor = Color(0xFFB58863)
    val selectColor = Color(0x6600A0FF)
    val targetColor = Color(0x5500FF00)
    var boardSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.Transparent)
            .pointerInput(flipped, boardSize) {
                detectTapGestures { offset ->
                    if (boardSize.width <= 0f) return@detectTapGestures
                    val cell = boardSize.width / 8f
                    var col = (offset.x / cell).toInt().coerceIn(0, 7)
                    var row = (offset.y / cell).toInt().coerceIn(0, 7)
                    if (flipped) { row = 7 - row; col = 7 - col }
                    onTap(row, col)
                }
            }
    ) {
        boardSize = size
        val cell = size.width / 8f
        val paint = Paint().apply {
            textSize = cell * 0.78f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        for (sr in 0..7) for (sc in 0..7) {
            val (r, c) = if (flipped) (7 - sr) to (7 - sc) else sr to sc
            val isLight = (r + c) % 2 == 0
            var color = if (isLight) lightColor else darkColor
            if (selected == (r to c)) color = selectColor
            else if (legalTargets.contains(r to c)) color = targetColor
            drawRect(color, Offset(sc * cell, sr * cell), Size(cell, cell))
        }

        for (sr in 0..7) for (sc in 0..7) {
            val (r, c) = if (flipped) (7 - sr) to (7 - sc) else sr to sc
            val piece = board[r][c]
            if (piece != ' ') {
                val glyph = PIECE_TO_CHAR[piece] ?: continue
                val isWhite = piece.isUpperCase()

                // Trik: gambar glyph dengan STROKE tebal dulu (outline),
                // lalu FILL solid di atasnya. Glyph putih pun jadi punya isi.
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cell * 0.06f
                paint.color = if (isWhite) 0xFF000000.toInt() else 0xFF888888.toInt()

                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val x = sc * cell + cell / 2f
                    val y = sr * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f
                    native.drawText(glyph, x, y, paint)
                }

                paint.style = Paint.Style.FILL
                paint.color = if (isWhite) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val x = sc * cell + cell / 2f
                    val y = sr * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f
                    native.drawText(glyph, x, y, paint)
                }
            }
        }
    }
}
