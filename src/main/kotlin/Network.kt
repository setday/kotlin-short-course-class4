import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import io.ktor.utils.io.*

fun startAsServer(hostname: String, port: Int) = runBlocking {
    val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress(hostname, port))
    println("Started summation server at ${server.localAddress}")

    val socket1 = server.accept()
    val input1 = socket1.openReadChannel()
    val output1 = socket1.openWriteChannel(autoFlush = true)
    output1.writeInt(1)
    output1.writeStringUtf8("waitn\n")
    println("Socket1 accepted: ${socket1.remoteAddress}")

    val socket2 = server.accept()
    val input2 = socket2.openReadChannel()
    val output2 = socket2.openWriteChannel(autoFlush = true)
    output2.writeInt(2)
    output2.writeStringUtf8("wait\n")
    println("Socket2 accepted: ${socket2.remoteAddress}")

    var counter = 0

    try {
        while (true) {
            counter++

            if (counter > 8) {
                output1.writeStringUtf8("sync\n")
                output2.writeStringUtf8("sync\n")
                for (i in State.Desc.indices) {
                    for (j in State.Desc[0].indices) {
                        output1.writeInt(getDesc(j, i))
                        output2.writeInt(getDesc(j, i))
                    }
                }
            }

            output1.writeStringUtf8("turn\n")
            var value = input1.readInt()
            output1.writeStringUtf8("wait\n")

            setDesc(value)

            output2.writeStringUtf8("set\n")
            output2.writeInt(value)

            output2.writeStringUtf8("turn\n")
            value = input2.readInt()
            output2.writeStringUtf8("wait\n")

            setDesc(value)

            output1.writeStringUtf8("set\n")
            output1.writeInt(value)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        socket1.close()
        socket2.close()
    }
}

fun startNetworkingS(hostname: String, port: Int) {
    startAsServer(hostname, port)
}
