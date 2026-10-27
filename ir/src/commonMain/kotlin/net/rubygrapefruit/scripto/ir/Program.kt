package net.rubygrapefruit.scripto.ir

class Program(val name: String, val operations: List<Operation>) {
    val camelCaseName: String
    val snakeCaseName: String

    init {
        val ccName = StringBuilder()
        val scName = StringBuilder()
        var startWord = true
        for (index in name.indices) {
            val ch = name[index]
            if (ch == ' ') {
                scName.append('_')
                startWord = true
            } else if (startWord) {
                ccName.append(name[index].uppercaseChar())
                scName.append(name[index].lowercaseChar())
                startWord = false
            } else {
                val lcc = name[index].lowercaseChar()
                ccName.append(lcc)
                scName.append(lcc)
            }
        }
        camelCaseName = ccName.toString()
        snakeCaseName = scName.toString()
    }
}
