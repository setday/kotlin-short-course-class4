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
import javax.swing.WindowConstants

fun main(args: Array<String>) {
    createWindow("Klock (${args[0]})")
    startNetworking(args, "127.0.0.1", 2323)
}

fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseListener(MouseListener)

    window.preferredSize = Dimension(800, 600)
    window.minimumSize = Dimension(100,100)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}

data class Point(val x: Float, val y: Float, val isRemote: Boolean = false)

object State {
    var isServer = true
    var input: ByteReadChannel? = null
    var output: ByteWriteChannel? = null
    val points = mutableListOf<Point>()
}

class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 40f)
    val paint = Paint().apply {
        color = 0xff0000ff.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintRemote = Paint().apply {
        color = 0xff00ff00.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        val w = (width / contentScale).toInt()
        val h = (height / contentScale).toInt()

        State.points.forEach { p ->
            canvas.drawCircle(p.x, p.y, 5f, if (p.isRemote) paintRemote else paint)
        }

        layer.needRedraw()
    }
}

object MouseListener : MouseAdapter() {
    override fun mouseClicked(event: MouseEvent?) {
        if(event != null) {
            val mouseX = event.x.toFloat()
            val mouseY = event.y.toFloat()
            State.points.add(Point(mouseX, mouseY))
            if(!State.isServer) {
                sendMouseCoordinates(mouseX, mouseY)
            }
        }
    }

}
