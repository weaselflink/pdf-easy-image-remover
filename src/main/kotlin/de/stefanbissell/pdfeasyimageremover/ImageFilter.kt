package de.stefanbissell.pdfeasyimageremover

import com.itextpdf.kernel.exceptions.PdfException
import com.itextpdf.kernel.pdf.PdfArray
import com.itextpdf.kernel.pdf.PdfDictionary
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfIndirectReference
import com.itextpdf.kernel.pdf.PdfLiteral
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.PdfOutputStream
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfResources
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.IContentOperator
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject

class ImageFilter(
    private val inputFile: String,
    private val outputFile: String
) {

    private val imageExtractor = ImageExtractor(inputFile)

    fun filter(filter: (ImageInfo) -> Boolean) {
        val doc = PdfDocument(
            PdfReader(inputFile),
            PdfFilterWriter(outputFile, filter)
        )
        ImageFilteringProcessor(doc, imageExtractor, 9, filter).editPage()
        doc.close()
    }

    private inner class PdfFilterWriter(
        filename: String,
        val filterExpr: (ImageInfo) -> Boolean
    ) : PdfWriter(filename) {

        override fun write(pdfObject: PdfObject?): PdfOutputStream {
            logPage(pdfObject)

            if (pdfObject.isForm) {
                val form = PdfFormXObject(pdfObject as PdfStream)
                val imagesToRemove = form.xObjects
                    .mapNotNull { xObject ->
                        imageExtractor.findById(xObject.second)
                            ?.let { xObject.first to it }
                    }
                    .filter { filterExpr(it.second) }
                    .map { it.first }
                if (imagesToRemove.isNotEmpty()) {
                    // logForm("===", pdfObject)
                    val doRegex = Regex("(${imagesToRemove.joinToString("|")}) Do")
                    val content = String(pdfObject.bytes)
                    pdfObject.setData(
                        content
                            .split("\n")
                            .filterNot { it.matches(doRegex) }
                            .joinToString("\n")
                            .toByteArray()
                    )
                }
            }
            return super.write(pdfObject)
        }
    }
}

class ImageFilteringProcessor(
    private val doc: PdfDocument,
    private val imageExtractor: ImageExtractor,
    private val pageNumber: Int,
    private val filter: (ImageInfo) -> Boolean = { true },
    private val page: PdfPage = doc.getPage(pageNumber),
    private val pdfResources: PdfResources = page.resources
) : PdfCanvasProcessor(NoOpEventListener()) {

    private var canvas: PdfCanvas? = null

    /**
     * This method edits the immediate contents of a page, i.e. its content stream.
     * It explicitly does not descent into form xobjects, patterns, or annotations.
     */
    fun editPage() {
        if (doc.reader == null || doc.writer == null) {
            throw PdfException("PdfDocument must be opened in stamping mode.")
        }
        val pdfCanvas = PdfCanvas(PdfStream(), pdfResources, doc)
        editContent(page.contentBytes, pdfResources, pdfCanvas)
        page.put(PdfName.Contents, pdfCanvas.contentStream)
    }

    /**
     * This method processes the content bytes and outputs to the given canvas.
     * It explicitly does not descent into form xobjects, patterns, or annotations.
     */
    private fun editContent(contentBytes: ByteArray?, resources: PdfResources?, canvas: PdfCanvas?) {
        this.canvas = canvas
        processContent(contentBytes, resources)
        this.canvas = null
    }

    /**
     * This method writes content stream operations to the target canvas. The default
     * implementation writes them as they come, so it essentially generates identical
     * copies of the original instructions the [ContentOperatorWrapper] instances
     * forward to it.
     *
     * Override this method to achieve some fancy editing effect.
     */
    private fun write(operands: List<PdfObject>) {
        val pdfOutputStream = canvas!!.contentStream.outputStream
        for ((index, op) in operands.withIndex()) {
            pdfOutputStream.write(op)
            if (index < operands.size - 1) {
                pdfOutputStream.writeSpace()
            } else {
                pdfOutputStream.writeNewLine()
            }
        }
    }

    override fun registerContentOperator(operatorString: String, operator: IContentOperator): IContentOperator? {
        val wrapper = ContentOperatorWrapper().apply {
            originalOperator = operator
        }
        val formerOperator = super.registerContentOperator(operatorString, wrapper)
        return if (formerOperator != null && formerOperator is ContentOperatorWrapper) {
            formerOperator.originalOperator!!
        } else {
            formerOperator
        }
    }

    private inner class ContentOperatorWrapper : IContentOperator {

        var originalOperator: IContentOperator? = null

        override fun invoke(processor: PdfCanvasProcessor, operator: PdfLiteral, operands: List<PdfObject>) {
            if (originalOperator != null && "Do" != operator.toString()) {
                originalOperator!!.invoke(processor, operator, operands)
            }
            if ("Do" != operator.toString()) {
                write(operands)
            } else {
                val op = operands[0] as PdfName
                val stream = pdfResources.getResource(PdfName.XObject)?.getAsStream(op)
                if (stream == null) {
                    write(operands)
                } else {
                    when {
                        stream.isImage -> {
                            val image = PdfImageXObject(stream)
                            val imageInfo = imageExtractor.findByHash(image.hash())
                            if (imageInfo != null && filter(imageInfo)) {
                                write(operands)
                            }
                            println()
                        }
                        else -> {
                            write(operands)
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun logForm(prefix: String, obj: PdfObject?) {
    if (obj.isForm) {
        val form = PdfFormXObject(obj as PdfStream)
        // println("$prefix ${form.resources.resourceNames}")
        // println("$prefix ${form.resources.pdfObject.values().map { it::class.java.name }}")
        // println("$prefix ${(form.resources.pdfObject[PdfName.XObject] as PdfDictionary?)?.keySet()}")
        println("$prefix ${(form.resources.pdfObject[PdfName.XObject] as PdfDictionary?)?.toString()}")
        println("$prefix ${form.xObjects}")
        // println("$prefix ${form.resources.pdfObject}")
        // val content = String(obj.bytes)
        // println(content.split("\n"))
        // println(content.split("\n").filter { it != "/Im0 Do" })
        println()
    }
}

@Suppress("unused")
private fun logPage(pdfObject: PdfObject?) {
    if (pdfObject.isPage) {
        println((pdfObject as PdfDictionary).get(PdfName.Resources, false))
        val contents = (pdfObject as PdfDictionary).get(PdfName.Contents, false)
        when (contents) {
            is PdfArray -> {
                contents
                    .filterIsInstance<PdfStream>()
                    .forEach { println("a s $it") }
            }
            is PdfStream -> {
                println("s $contents")
            }
            is PdfIndirectReference -> {
                println("r $contents")
            }
            else -> {
                println("x $contents")
            }
        }
    }
}

class NoOpEventListener : IEventListener {
    override fun eventOccurred(data: IEventData, type: EventType) {}
    override fun getSupportedEvents(): Set<EventType> = emptySet()
}
