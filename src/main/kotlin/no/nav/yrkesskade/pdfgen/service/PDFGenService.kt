package no.nav.yrkesskade.pdfgen.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import no.nav.yrkesskade.pdfgen.Application
import no.nav.yrkesskade.pdfgen.transformers.HtmlCreator
import org.apache.pdfbox.io.IOUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

val objectMapper: ObjectMapper = ObjectMapper()
    .registerKotlinModule()

val colorProfile: ByteArray = IOUtils.toByteArray(Application::class.java.getResourceAsStream("/sRGB2014.icc"))

val fonts: Array<FontMetadata> =
    objectMapper.readValue(ClassPathResource("/fonts/config.json").inputStream)

data class FontMetadata(
    val family: String,
    val path: String,
    val weight: Int,
    val style: BaseRendererBuilder.FontStyle,
    val subset: Boolean
)

@Service
class PDFGenService {

    fun getPDFAsByteArray(json: String): ByteArray {
        val doc = getHTMLDocument(jacksonObjectMapper().readValue(json, List::class.java) as List<Map<String, *>>)
        val outputStream = ByteArrayOutputStream()
        createPDFA(doc, outputStream)
        return outputStream.toByteArray()
    }

    private fun getHTMLDocument(list: List<Map<String, *>>): Document {
        validateHeaderFooter(list)
        val creator = HtmlCreator(list)
        return creator.getDoc()
    }

    private fun validateHeaderFooter(list: List<Map<String, *>>) {
        if (list.any { it["type"] == "header" }.xor(list.any { it["type"] == "footer" })) {
            throw RuntimeException("Both a header and a footer must be defined.")
        }
    }

    private fun createPDFA(w3doc: Document, outputStream: OutputStream) = PdfRendererBuilder()
        .apply {
            for (font in fonts) {
                useFont(FSSupplier(getIs(font.path)), font.family, font.weight, font.style, font.subset)
            }
        }
        // .useFastMode() wait with fast mode until it doesn't print a bunch of errors
        .useColorProfile(colorProfile)
        .useSVGDrawer(BatikSVGDrawer())
        .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
        .withW3cDocument(w3doc, PDFGenService::javaClass.javaClass.getResource("/dummy.html").toExternalForm())
        .toStream(outputStream)
        .buildPdfRenderer()
        .createPDF()

    private fun getIs(path: String): () -> InputStream { return { ClassPathResource("/fonts/$path").inputStream } }

}