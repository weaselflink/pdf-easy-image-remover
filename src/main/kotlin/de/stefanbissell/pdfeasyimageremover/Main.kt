package de.stefanbissell.pdfeasyimageremover

fun main() {
    val input = "FAS21001.pdf"
    val output = "FAS21001-mod.pdf"
    val imageFolder = "images"

    ImageExtractor(input).apply {
        extract(9)
            .forEach { it.writeToFile(imageFolder) }
    }

    val toRemove = listOf(
        "54ba66c74a32afa0",
        "a8e9475533d203f6",
    )
    ImageFilter(input, output).filter {
        it.hash in toRemove
    }
}
