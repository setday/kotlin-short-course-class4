import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import io.ktor.utils.io.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun startAsClient(hostname: String, port: Int) = runBlocking {
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress(hostname, port))
    val input = socket.openReadChannel()
    State.output = socket.openWriteChannel(autoFlush = true)

    State.id = input.readInt()

    while (true) {
        when (input.readUTF8Line()) {
            "exit" -> {
                State.Status = "exit"
                break
            }
            "wait" -> State.Status = "wait"
            "set" -> {
                State.Status = "set"
                setDesc(input.readUTF8Line().toString().toInt())
            }
            "sync" -> {
                State.Status = "sync"
                repeat (64) {
                    setDesc(input.readUTF8Line().toString().toInt())
                }
            }
            "turn" -> State.Status = "turn"
        }
    }

    State.output?.writeStringUtf8("c0000")
    socket.close()
}

fun requestToSet(x: Int, y: Int) {
    GlobalScope.launch(Dispatchers.IO) {
        State.output?.writeInt(getDesc(x, y))
    }
}


fun startNetworkingC(hostname: String, port: Int) {
    startAsClient(hostname, port)
}