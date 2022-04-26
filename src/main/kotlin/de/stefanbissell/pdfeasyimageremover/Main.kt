package de.stefanbissell.pdfeasyimageremover

fun main(args: Array<String>) {
    args
        .parseCommand()
        .execute()
}

fun Array<String>.parseCommand(): Command {
    return when (getOrNull(0)) {
        "extract", "x" -> ExtractCommand(get(1), get(2).toInt())
        "remove", "r" -> RemoveCommand(get(1), get(2), toList().subList(3, size))
        else -> UsageCommand()
    }
}

interface Command {

    fun execute()
}

class ExtractCommand(
    private val inputFile: String,
    private val pageNumber: Int,
    private val imageFolder: String = "images"
) : Command {

    override fun execute() {
        ImageExtractor(inputFile)
            .extract(pageNumber)
            .forEach { it.writeToFile(imageFolder) }
    }
}

class RemoveCommand(
    private val inputFile: String,
    private val outputFile: String,
    private val hashes: List<String>
) : Command {

    override fun execute() {
        ImageFilter(inputFile, outputFile)
            .filter {
                it.hash in hashes
            }
    }
}

class UsageCommand : Command {

    override fun execute() {
        println("Usage:")
        println("java -jar pdf-easy-image-remover-1.0-all.jar <extract|x> <input-file> <page-number>")
        println("java -jar pdf-easy-image-remover-1.0-all.jar <remove|r> <input-file> <output-file> <image-hash ...>")
    }
}
