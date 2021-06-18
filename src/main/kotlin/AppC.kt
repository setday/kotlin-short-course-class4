import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.WindowConstants

fun createWindowC(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseListener(MouseListener)
    window.layer.addMouseMotionListener(MouseMotionListener)

    window.preferredSize = Dimension(800, 800)
    window.minimumSize = Dimension(800,800)
    window.maximumSize = Dimension(800, 800)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}

object State {
    var output: ByteWriteChannel? = null
    var id = 1
    var Status = "wait"
    var Desc = arrayOf(
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 2, 1, 0, 0, 0),
        arrayOf(0, 0, 0, 1, 2, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        arrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    )
}

fun checkDir(x: Int, y: Int, value: Int, dX: Int, dY: Int): Int {
    val op = if (value == 1) {2} else {1}
    var tmpX = x + dX
    var tmpY = y + dY

    if (tmpX in 0..7 && tmpY in 0..7 && State.Desc[tmpY][tmpX] == value)
        return 0

    while (tmpX in 0..7 && tmpY in 0..7 && State.Desc[tmpY][tmpX] == op) {
        tmpX += dX
        tmpY += dY
    }

    if (tmpX !in 0..7 || tmpY !in 0..7 || State.Desc[tmpY][tmpX] != value)
        return 0

    tmpX = x + dX
    tmpY = y + dY
    while (State.Desc[tmpY][tmpX] == op) {
        State.Desc[tmpY][tmpX] = value
        tmpX += dX
        tmpY += dY
    }
    return 1
}

fun setDesc(code: Int): Boolean {
    val x = (code / 10) % 10
    val y = code % 10
    val value = code / 100
    var res = 0

    if (State.Desc[y][x] != 0)
        return false

    for (i in -1..1)
        for (j in -1..1)
            if (i != 0 || j != 0)
                res += checkDir(x, y, value, i, j)

    if (res > 0) {
        State.Desc[y][x] = value
        return true
    }

    return false
}

fun getDesc(x: Int, y: Int): Int {
    return State.Desc[y][x] * 100 + x * 10 + y
}

fun drawDesc(canvas: Canvas, piece: Paint, stroke: Paint, player1: Paint, player2: Paint) {
    for (i in 0..7) {
        for (j in 0..7) {
            val rect = Rect(j * 100f, i * 100f, (j + 1) * 100f, (i + 1) * 100f)
            canvas.drawRect(rect, piece)
            canvas.drawRect(rect, stroke)
            if (State.Desc[i][j] == 1) {
                canvas.drawOval(rect, player1)
                canvas.drawOval(rect, stroke)
            }
            else if (State.Desc[i][j] == 2) {
                canvas.drawOval(rect, player2)
                canvas.drawOval(rect, stroke)
            }
        }
    }
}

class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 20f)

    val paintHover = Paint().apply {
        color = 0x2f000000.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintHoverW = Paint().apply {
        color = 0x2fff0000.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paint = Paint().apply {
        color = 0xff8000ffb.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintPlayer1 = Paint().apply {
        color = 0xffefa94a.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintPlayer2 = Paint().apply {
        color = 0xff26252d.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintPiece = Paint().apply {
        color = 0xff4285b4.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintStroke = Paint().apply {
        color = 0xff1a3547.toInt()
        mode = PaintMode.STROKE
        strokeWidth = 1f
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)

        drawDesc(canvas, paintPiece, paintStroke, paintPlayer1, paintPlayer2)

        when (State.Status) {
            "exit" -> canvas.drawString("Status: Connection closed!", 0f, 50f, font, paint)
            "wait" -> canvas.drawString("Status: Waiting for opponent...", 0f, 50f, font, paint)
            "set" -> canvas.drawString("Status: Getting data...", 0f, 50f, font, paint)
            "turn" -> canvas.drawString("Status: Your turn!", 0f, 50f, font, paint)
            "sync" -> canvas.drawString("Status: Synchronisation data...", 0f, 50f, font, paint)
            "win" -> canvas.drawString("You WIN (game has been ended)", 0f, 50f, font, paint)
            "lose" -> canvas.drawString("You LOSE (game has been ended)", 0f, 50f, font, paint)
        }

        if (Mouse.HoverX in 0..7 && Mouse.HoverY in 0..7) {
            when (State.Desc[Mouse.HoverY][Mouse.HoverX]) {
                0 -> canvas.drawOval(Rect(Mouse.HoverX * 100f,
                    Mouse.HoverY * 100f, (Mouse.HoverX + 1) * 100f, (Mouse.HoverY + 1) * 100f), paintHover)
                else -> canvas.drawOval(Rect(Mouse.HoverX * 100f,
                    Mouse.HoverY * 100f, (Mouse.HoverX + 1) * 100f, (Mouse.HoverY + 1) * 100f), paintHoverW)
            }
        }

        layer.needRedraw()
    }
}

object Mouse {
    var HoverX = 0
    var HoverY = 0
}

object MouseListener : MouseAdapter() {
    override fun mouseClicked(event: MouseEvent?) {
        if(event != null) {
            val mouseX = event.x / 100
            val mouseY = event.y / 100
            if(State.Status == "turn" && mouseX in 0..7 && mouseY in 0..7) {
                if (!setDesc(mouseY + mouseX * 10 + State.id * 100))
                    return
                requestToSet(mouseX, mouseY)
                State.Status = "wait"
            }
        }
    }
}

object MouseMotionListener : MouseMotionAdapter() {
    override fun mouseMoved(event: MouseEvent?) {
        if(event != null) {
            Mouse.HoverX = event.x / 100
            Mouse.HoverY = event.y / 100
        }
    }
}
