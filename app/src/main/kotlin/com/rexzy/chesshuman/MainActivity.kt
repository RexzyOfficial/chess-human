package com.rexzy.chesshuman

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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
        val fromSquare = Square.valueOf(squareName(r, c))
        val moves = try {
            MoveGenerator.generateLegalMoves(board).filter { it.from == fromSquare }
        } catch (e: Exception) { emptyList() }
        legalTargets = moves.mapNotNull { mv ->
            val to = mv.to.name
            (8 - to[1].digitToInt()) to (to[0] - 'a')
        }
    }

    fun attemptMove(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val fromSq = Square.valueOf(squareName(from.first, from.second))
        val toSq = Square.valueOf(squareName(to.first, to.second))
        val candidate = board.legalMoves().firstOrNull { it.from == fromSq && it.to == toSq } ?: return
        val fenBefore = board.fen
        board.doMove(candidate)
        history = history + HistoryEntry(fenBefore, candidate.toString())
        selected = null
        legalTargets = emptyList()
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
    return "${('a' + c)}${8 - r}"
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
                drawChessPiece(piece, Offset(sc * cell + cell / 2f, sr * cell + cell / 2f), cell * 0.85f)
            }
        }
    }
}

fun DrawScope.drawChessPiece(piece: Char, center: Offset, size: Float) {
    val isWhite = piece.isUpperCase()
    val fill = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF000000)
    val outline = if (isWhite) Color(0xFF000000) else Color(0xFF666666)
    val strokeW = size * 0.04f
    val h = size
    val w = size
    val cx = center.x
    val cy = center.y

    val body = Path().apply {
        when (piece.lowercaseChar()) {
            'p' -> {
                // Bola atas
                addOval(androidx.compose.ui.geometry.Rect(
                    cx - w * 0.18f, cy - h * 0.40f,
                    cx + w * 0.18f, cy - h * 0.04f
                ))
                // Leher
                moveTo(cx - w * 0.20f, cy - h * 0.05f)
                lineTo(cx + w * 0.20f, cy - h * 0.05f)
                lineTo(cx + w * 0.28f, cy + h * 0.15f)
                lineTo(cx - w * 0.28f, cy + h * 0.15f)
                close()
                // Badan bawah
                moveTo(cx - w * 0.35f, cy + h * 0.15f)
                lineTo(cx + w * 0.35f, cy + h * 0.15f)
                lineTo(cx + w * 0.40f, cy + h * 0.32f)
                lineTo(cx - w * 0.40f, cy + h * 0.32f)
                close()
                // Alas
                addRoundRect(androidx.compose.ui.geometry.RoundRect(
                    left = cx - w * 0.42f, top = cy + h * 0.32f,
                    right = cx + w * 0.42f, bottom = cy + h * 0.42f,
                    radiusX = w * 0.05f, radiusY = h * 0.05f
                ))
            }
            'r' -> {
                // Menara: kotak dengan gigi
                moveTo(cx - w * 0.30f, cy + h * 0.42f)
                lineTo(cx - w * 0.25f, cy - h * 0.20f)
                lineTo(cx - w * 0.25f, cy - h * 0.35f)
                lineTo(cx - w * 0.15f, cy - h * 0.35f)
                lineTo(cx - w * 0.15f, cy - h * 0.25f)
                lineTo(cx - w * 0.05f, cy - h * 0.25f)
                lineTo(cx - w * 0.05f, cy - h * 0.35f)
                lineTo(cx + w * 0.05f, cy - h * 0.35f)
                lineTo(cx + w * 0.05f, cy - h * 0.25f)
                lineTo(cx + w * 0.15f, cy - h * 0.25f)
                lineTo(cx + w * 0.15f, cy - h * 0.35f)
                lineTo(cx + w * 0.25f, cy - h * 0.35f)
                lineTo(cx + w * 0.25f, cy - h * 0.20f)
                lineTo(cx + w * 0.30f, cy + h * 0.42f)
                close()
            }
            'n' -> {
                // Kuda: bentuk kepala
                moveTo(cx - w * 0.30f, cy + h * 0.42f)
                lineTo(cx - w * 0.30f, cy + h * 0.10f)
                lineTo(cx - w * 0.15f, cy - h * 0.10f)
                lineTo(cx - w * 0.35f, cy - h * 0.10f)
                lineTo(cx - w * 0.25f, cy - h * 0.30f)
                lineTo(cx - w * 0.05f, cy - h * 0.42f)
                lineTo(cx + w * 0.15f, cy - h * 0.38f)
                lineTo(cx + w * 0.30f, cy - h * 0.20f)
                lineTo(cx + w * 0.30f, cy + h * 0.10f)
                lineTo(cx + w * 0.30f, cy + h * 0.42f)
                close()
            }
            'b' -> {
                // Uskup: tetes + badan
                addOval(androidx.compose.ui.geometry.Rect(
                    cx - w * 0.08f, cy - h * 0.45f,
                    cx + w * 0.08f, cy - h * 0.29f
                ))
                moveTo(cx, cy - h * 0.30f)
                lineTo(cx + w * 0.25f, cy + h * 0.05f)
                lineTo(cx + w * 0.20f, cy + h * 0.32f)
                lineTo(cx - w * 0.20f, cy + h * 0.32f)
                lineTo(cx - w * 0.25f, cy + h * 0.05f)
                close()
                addRoundRect(androidx.compose.ui.geometry.RoundRect(
                    left = cx - w * 0.30f, top = cy + h * 0.32f,
                    right = cx + w * 0.30f, bottom = cy + h * 0.42f,
                    radiusX = w * 0.05f, radiusY = h * 0.05f
                ))
            }
            'q' -> {
                // Ratu: mahkota
                moveTo(cx - w * 0.35f, cy + h * 0.42f)
                lineTo(cx - w * 0.30f, cy - h * 0.05f)
                lineTo(cx - w * 0.15f, cy - h * 0.35f)
                lineTo(cx - w * 0.05f, cy - h * 0.15f)
                lineTo(cx, cy - h * 0.40f)
                lineTo(cx + w * 0.05f, cy - h * 0.15f)
                lineTo(cx + w * 0.15f, cy - h * 0.35f)
                lineTo(cx + w * 0.30f, cy - h * 0.05f)
                lineTo(cx + w * 0.35f, cy + h * 0.42f)
                close()
            }
            'k' -> {
                // Raja: salib + badan
                moveTo(cx - w * 0.05f, cy - h * 0.48f)
                lineTo(cx + w * 0.05f, cy - h * 0.48f)
                lineTo(cx + w * 0.05f, cy - h * 0.38f)
                lineTo(cx + w * 0.15f, cy - h * 0.38f)
                lineTo(cx + w * 0.15f, cy - h * 0.28f)
                lineTo(cx + w * 0.05f, cy - h * 0.28f)
                lineTo(cx + w * 0.05f, cy - h * 0.18f)
                lineTo(cx - w * 0.05f, cy - h * 0.18f)
                lineTo(cx - w * 0.05f, cy - h * 0.28f)
                lineTo(cx - w * 0.15f, cy - h * 0.28f)
                lineTo(cx - w * 0.15f, cy - h * 0.38f)
                lineTo(cx - w * 0.05f, cy - h * 0.38f)
                close()
                moveTo(cx - w * 0.30f, cy + h * 0.42f)
                lineTo(cx - w * 0.25f, cy - h * 0.05f)
                lineTo(cx - w * 0.10f, cy - h * 0.15f)
                lineTo(cx + w * 0.10f, cy - h * 0.15f)
                lineTo(cx + w * 0.25f, cy - h * 0.05f)
                lineTo(cx + w * 0.30f, cy + h * 0.42f)
                close()
            }
        }
    }

    drawPath(body, fill)
    drawPath(body, outline, style = Stroke(width = strokeW))
}
