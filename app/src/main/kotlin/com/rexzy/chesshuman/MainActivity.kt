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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    var hints by remember { mutableStateOf<List<EngineHint>>(emptyList()) }
    var hintLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refreshLegal(from: Pair<Int, Int>?) {
        if (from == null) { legalTargets = emptyList(); return }
        val (r, c) = from
        val fromSquare = try { Square.valueOf(squareName(r, c)) } catch (e: Exception) { return }
        val moves = try {
            MoveGenerator.generateLegalMoves(board).filter { it.from == fromSquare }
        } catch (e: Exception) { emptyList() }
        legalTargets = moves.mapNotNull { mv ->
            val toName = mv.to.name
            (8 - toName[1].digitToInt()) to (toName[0].lowercaseChar() - 'a')
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
            hints = emptyList() // panah lama hilang setelah langkah baru
        } catch (e: Exception) {
        } finally {
            selected = null
            legalTargets = emptyList()
        }
    }

    fun toggleHints() {
        if (hints.isNotEmpty()) {
            hints = emptyList()
            return
        }
        if (hintLoading) return
        hintLoading = true
        scope.launch {
            try {
                hints = HumanEngine.findHints(board.fen)
            } finally {
                hintLoading = false
            }
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
            hints = hints,
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
                board = Board(); selected = null; legalTargets = emptyList(); history = emptyList(); hints = emptyList()
            }) { Text("Reset") }
            Button(enabled = history.isNotEmpty(), onClick = {
                val last = history.last()
                history = history.dropLast(1)
                board = Board()
                board.loadFromFen(last.fen)
                selected = null; legalTargets = emptyList(); hints = emptyList()
            }) { Text("Undo") }
            Button(enabled = !hintLoading, onClick = { toggleHints() }) {
                Text(
                    when {
                        hintLoading -> "..."
                        hints.isNotEmpty() -> "Hide Hints"
                        else -> "Show Hints"
                    }
                )
            }
        }
    }
}

fun squareName(r: Int, c: Int): String {
    return "${('A' + c)}${8 - r}"
}

@Composable
fun ChessBoard(
    fen: String,
    flipped: Boolean,
    selected: Pair<Int, Int>?,
    legalTargets: List<Pair<Int, Int>>,
    hints: List<EngineHint>,
    onTap: (Int, Int) -> Unit
) {
    val board = fenToBoard(fen)
    val lightColor = Color(0xFFF0D9B5)
    val darkColor = Color(0xFFB58863)
    val selectColor = Color(0x6600A0FF)
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

        // 1. Papan
        for (sr in 0..7) for (sc in 0..7) {
            val (r, c) = if (flipped) (7 - sr) to (7 - sc) else sr to sc
            val isLight = (r + c) % 2 == 0
            var color = if (isLight) lightColor else darkColor
            if (selected == (r to c)) color = selectColor
            drawRect(color, Offset(sc * cell, sr * cell), Size(cell, cell))
        }

        // 2. Dot / ring target legal
        for (sr in 0..7) for (sc in 0..7) {
            val (r, c) = if (flipped) (7 - sr) to (7 - sc) else sr to sc
            if (legalTargets.contains(r to c)) {
                val hasPiece = board[r][c] != ' '
                val cx = sc * cell + cell / 2f
                val cy = sr * cell + cell / 2f
                if (hasPiece) {
                    drawCircle(Color(0x44000000), cell * 0.45f, Offset(cx, cy), style = Stroke(width = cell * 0.08f))
                } else {
                    drawCircle(Color(0x44000000), cell * 0.15f, Offset(cx, cy))
                }
            }
        }

        // 3. Panah hint (di bawah bidak supaya bidak tetap terlihat)
        for (h in hints) {
            val (sr1, sc1) = toScreen(h.fromRow, h.fromCol, flipped)
            val (sr2, sc2) = toScreen(h.toRow, h.toCol, flipped)
            val start = Offset(sc1 * cell + cell / 2f, sr1 * cell + cell / 2f)
            val end = Offset(sc2 * cell + cell / 2f, sr2 * cell + cell / 2f)
            val color = when (h.rank) {
                0 -> Color(0xAA00AAFF) // biru pekat
                1 -> Color(0x8800CC66) // hijau
                else -> Color(0x77FF8800) // oranye
            }
            val thickness = if (h.rank == 0) cell * 0.14f else cell * 0.10f
            drawArrow(start, end, color, thickness, cell)
        }

        // 4. Bidak
        for (sr in 0..7) for (sc in 0..7) {
            val (r, c) = if (flipped) (7 - sr) to (7 - sc) else sr to sc
            val piece = board[r][c]
            if (piece != ' ') {
                val glyph = PIECE_TO_CHAR[piece] ?: continue
                val isWhite = piece.isUpperCase()

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = cell * 0.06f
                paint.color = if (isWhite) 0xFF000000.toInt() else 0xFF888888.toInt()
                drawIntoCanvas {
                    it.nativeCanvas.drawText(glyph, sc * cell + cell / 2f, sr * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f, paint)
                }

                paint.style = Paint.Style.FILL
                paint.color = if (isWhite) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                drawIntoCanvas {
                    it.nativeCanvas.drawText(glyph, sc * cell + cell / 2f, sr * cell + cell / 2f - (paint.ascent() + paint.descent()) / 2f, paint)
                }
            }
        }
    }
}

fun toScreen(r: Int, c: Int, flipped: Boolean): Pair<Int, Int> {
    return if (flipped) (7 - r) to (7 - c) else r to c
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    thickness: Float,
    cell: Float
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val ux = dx / len
    val uy = dy / len

    // Potong ujung panah supaya tidak menutupi pusat bidak tujuan sepenuhnya
    val headLen = cell * 0.35f
    val shaftEnd = Offset(end.x - ux * headLen, end.y - uy * headLen)

    drawLine(
        color = color,
        start = start,
        end = shaftEnd,
        strokeWidth = thickness,
        cap = StrokeCap.Round
    )

    // Segitiga ujung panah
    val perpX = -uy
    val perpY = ux
    val headWidth = cell * 0.22f
    val tip = end
    val left = Offset(
        end.x - ux * headLen + perpX * headWidth,
        end.y - uy * headLen + perpY * headWidth
    )
    val right = Offset(
        end.x - ux * headLen - perpX * headWidth,
        end.y - uy * headLen - perpY * headWidth
    )
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color)
}
