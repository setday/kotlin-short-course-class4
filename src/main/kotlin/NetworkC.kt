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
                setDesc(input.readInt())
            }
            "sync" -> {
                State.Status = "sync"
                repeat (64) {
                    val code = input.readInt()
                    State.Desc[code % 10][(code / 10) % 10] = code / 100
                }
            }
            "turn" -> State.Status = "turn"
            "lose" ->{
                State.Status = "lose"
                break
            }
            "win" -> {
                State.Status = "win"
                break
            }
        }
    }

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