package de.stefanbissell.pdfeasyimageremover

import com.itextpdf.kernel.pdf.PdfName
import com.itextpdf.kernel.pdf.PdfObject
import com.itextpdf.kernel.pdf.PdfStream
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.util.encoders.Hex

fun PdfImageXObject.hash() = imageBytes.hash().substring(0, 16)

private fun ByteArray.hash() =
    SHA256Digest().let { hash ->
        val result = ByteArray(hash.digestSize)
        hash.update(this, 0, this.size)
        hash.doFinal(result, 0)
        String(Hex.encode(result))
    }

val PdfObject?.isImage
    get() = this is PdfStream &&
        getAsName(PdfName.Subtype) == PdfName.Image

val PdfObject?.isForm
    get() = this is PdfStream &&
        getAsName(PdfName.Subtype) == PdfName.Form

private val xObjectRegex = Regex("(/.*?) ([0-9]+)")

val PdfFormXObject.xObjects: List<Pair<String, Int>>
    get() = resources
        .pdfObject[PdfName.XObject]
        ?.toString()
        ?.let { xObjectRegex.findAll(it) }
        ?.map { it.groupValues[1] to it.groupValues[2].toInt() }
        ?.toList()
        ?: emptyList()
