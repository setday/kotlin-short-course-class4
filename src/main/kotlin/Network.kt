import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import io.ktor.utils.io.*

fun possibilityCheckDir(x: Int, y: Int, value: Int, dX: Int, dY: Int): Boolean {
    val op = if (value == 1) {2} else {1}
    var tmpX = x + dX
    var tmpY = y + dY

    if (tmpX in 0..7 && tmpY in 0..7 && State.Desc[tmpY][tmpX] == value)
        return false

    while (tmpX in 0..7 && tmpY in 0..7 && State.Desc[tmpY][tmpX] == op) {
        tmpX += dX
        tmpY += dY
    }

    return (tmpX in 0..7 && tmpY in 0..7 && State.Desc[tmpY][tmpX] == value)
}

fun possibilityCheck(value: Int): Boolean {
    for (x in 0..7) {
        for (y in 0..7) {
            var res = 0

            if (State.Desc[y][x] != 0)
                continue

            for (i in -1..1)
                for (j in -1..1)
                    if ((i != 0 || j != 0) && possibilityCheckDir(x, y, value, i, j))
                        return true
        }
    }

    return false
}

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
        while (false) {
            if (counter == 60) {
                var p1 = 0
                var p2 = 0

                for (i in State.Desc.indices) {
                    for (j in State.Desc[0].indices) {
                        if (State.Desc[i][j] == 1)
                            p1 += 1
                        if (State.Desc[i][j] == 2)
                            p2 += 1
                    }
                }
                when {
                    p1 > p2 -> {
                        output1.writeStringUtf8("win\n")
                        output2.writeStringUtf8("lose\n")
                    }
                    p1 < p2 -> {
                        output1.writeStringUtf8("lose\n")
                        output2.writeStringUtf8("win\n")
                    }
                    else -> {
                        output1.writeStringUtf8("lose\n")
                        output2.writeStringUtf8("lose\n")
                    }
                }

                break
            }

            if (counter % 8 == 0) {
                output1.writeStringUtf8("sync\n")
                output2.writeStringUtf8("sync\n")
                for (i in State.Desc.indices) {
                    for (j in State.Desc[0].indices) {
                        output1.writeInt(getDesc(j, i))
                        output2.writeInt(getDesc(j, i))
                    }
                }
            }

            if (possibilityCheck(1)) {
                counter++
                output2.writeStringUtf8("wait\n")
                output1.writeStringUtf8("turn\n")
                val value = input1.readInt()
                output1.writeStringUtf8("wait\n")

                setDesc(value)

                output2.writeStringUtf8("set\n")
                output2.writeInt(value)
            } else if(!possibilityCheck(2)) {
                counter = 60
            }

            if (possibilityCheck(2)) {
                counter++
                output1.writeStringUtf8("wait\n")
                output2.writeStringUtf8("turn\n")
                val value = input2.readInt()
                output2.writeStringUtf8("wait\n")

                setDesc(value)

                output1.writeStringUtf8("set\n")
                output1.writeInt(value)
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        try {
            output1.writeStringUtf8("exit\n")
        } catch (e: Throwable) {}
        try {
            output2.writeStringUtf8("exit\n")
        } catch (e: Throwable) {}
        socket1.close()
        socket2.close()
    }
}

fun startNetworkingS(hostname: String, port: Int) {
    startAsServer(hostname, port)
}
