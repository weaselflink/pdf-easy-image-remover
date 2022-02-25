package de.stefanbissell.pdfeasyimageremover

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfDocumentContentParser
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

class ImageExtractor(
    inputFile: String
) {

    private val doc = PdfDocument(PdfReader(inputFile))
    private val allImages by lazy { allImages() }

    fun findById(id: Int) =
        allImages
            .firstOrNull { it.id == id }

    fun findByHash(hash: String) =
        allImages
            .firstOrNull { it.hash == hash }

    fun extract(page: Int): List<ImageInfo> {
        val imageCollector = ImageCollector()

        PdfDocumentContentParser(doc)
            .processContent(page, imageCollector)

        return imageCollector
            .images
    }

    private fun allImages(): List<ImageInfo> =
        (0 until doc.numberOfPdfObjects)
            .map { it to doc.getPdfObject(it) }
            .filter { (_, obj) ->
                obj is PdfStream && obj.getAsName(PdfName.Subtype) == PdfName.Image
            }
            .map { (id, obj) ->
                id to PdfImageXObject(obj as PdfStream)
            }
            .map { (id, obj) ->
                ImageInfo(
                    id = id,
                    hash = obj.hash(),
                    image = obj
                )
            }

    private inner class ImageCollector: IEventListener {

        private val imagesOnPage = mutableListOf<PdfImageXObject>()

        val images: List<ImageInfo>
            get() = imagesOnPage
                .distinctBy { it.hash() }
                .mapNotNull { findByHash(it.hash()) }

        override fun eventOccurred(data: IEventData?, type: EventType?) {
            if (data != null && data is ImageRenderInfo) {
                imagesOnPage += data.image
            }
        }

        override fun getSupportedEvents(): MutableSet<EventType> {
            return mutableSetOf(EventType.RENDER_IMAGE)
        }
    }
}

data class ImageInfo(
    val id: Int,
    val hash: String,
    val image: PdfImageXObject
) {

    fun writeToFile(folder: String) {
        makeFolder(folder)
        ImageIO.read(ByteArrayInputStream(image.imageBytes))
            ?.also {
                ImageIO.write(it, "png", File("$folder/$hash.png"))
            }
    }

    private fun makeFolder(folder: String) {
        File(folder).mkdirs()
    }
}
