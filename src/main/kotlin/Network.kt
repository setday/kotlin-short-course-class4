import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import io.ktor.utils.io.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun startAsServer(hostname: String, port: Int) = runBlocking {
    State.isServer = true
    val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(hostname, port))
    val socket = server.accept()
    State.input = socket.openReadChannel()
    try {
        while(true) {
            val line = State.input?.readUTF8Line() ?: break
            val coords = line.split(" ").map { it.toFloat() }
            if(coords.size == 2) {
                State.points.add(Point(coords[0], coords[1], true))
            }
        }
    } finally {
        socket.close()
        server.close()
    }
}

fun startAsClient(hostname: String, port: Int) = runBlocking {
    State.isServer = false
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).
    tcp().connect(InetSocketAddress(hostname, port))
    State.output = socket.openWriteChannel(autoFlush = true)
}

fun sendMouseCoordinates(mouseX: Float, mouseY: Float) {
    GlobalScope.launch(Dispatchers.IO) {
        State.output?.writeStringUtf8("$mouseX $mouseY\n")
    }
}

fun startNetworking(args: Array<String>, hostname: String, port: Int) {
    if (args.size == 1) {
        when (args[0]) {
            "client" -> startAsClient(hostname, port)
            "server" -> startAsServer(hostname, port)
        }
    }
}