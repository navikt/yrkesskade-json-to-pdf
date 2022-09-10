package no.nav.yrkesskade.pdfgen.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.yrkesskade.pdfgen.service.PDFGenService
import no.nav.yrkesskade.pdfgen.util.getLogger
import no.nav.yrkesskade.pdfgen.util.getSecureLogger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "yrkesskade-json-to-pdf", description = "Create PDF from JSON")
class PDFGenController(
    private val pdfGenService: PDFGenService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Operation(
        summary = "Generate pdf from json",
        description = "Generate pdf from json"
    )
    @ResponseBody
    @PostMapping("/generer-pdf")
    fun toPDF(@RequestBody json: String): ResponseEntity<ByteArray> {
        logger.debug("toPDF() called. See body in secure logs")
        secureLogger.debug("toPDF() called. Received json: {}", json)

        val data = pdfGenService.getPDFAsByteArray(json)

        val responseHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_PDF
            add("Content-Disposition", "inline; filename=file.pdf")
        }
        return ResponseEntity(
            data,
            responseHeaders,
            HttpStatus.OK
        )
    }

}