fun main(args: Array<String>) {
    if (args[0] == "c") {
        createWindowC("Klock (${args[0]})")
        startNetworkingC("127.0.0.1", 8080)
    }
    if (args[0] == "s") {
        startNetworkingS("127.0.0.1", 8080)
    }
}
