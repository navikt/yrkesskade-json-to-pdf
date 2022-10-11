package no.nav.yrkesskade.pdfgen.transformers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import no.nav.yrkesskade.pdfgen.transformers.ElementType.*
import no.nav.yrkesskade.pdfgen.transformers.ElementType.FOOTER
import no.nav.yrkesskade.pdfgen.transformers.ElementType.HEADER
import no.nav.yrkesskade.pdfgen.util.getLogger
import no.nav.yrkesskade.pdfgen.util.getSecureLogger
import no.nav.yrkesskade.saksbehandling.model.Brevinnhold
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Suppress("UNCHECKED_CAST")
class HtmlCreator(private val brevinnhold: Brevinnhold) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    private fun getCss(footer: String) =
        """
                                html {
                                    white-space: pre-wrap;
                                    font-family: "Source Sans Pro" !important;
                                    box-sizing: border-box;
                                }
                                *, ::before, ::after {
                                  box-sizing: inherit;
                                }
                                .column {
                                  font-size: 16px;
                                  display: inline-block;
                                  width: 50%;
                                }
                                /* Fjerner spacing mellom inline-blockene */
                                .wrapper {
                                  font-size: 0;
                                }
                                h1 {
                                    font-size: 16pt;
                                }
                                h2 {
                                    font-size: 14pt;
                                }
                                h3 {
                                    font-size: 12pt;
                                }
                                .indent {
                                    padding-left: 24pt;
                                }
                                #header span {
                                    font-size: 10pt;
                                }
                                #navn {
                                    text-align: left;
                                }
                                #dato {
                                    text-align: right;
                                }
                                img {
                                    display: block;
                                    width: 100pt;
                                    float: right;
                                },
                                p, span {
                                    font-size: 12pt;
                                }
                                .tabellUnderLogo {
                                    margin-top: 100px;
                                    margin-bottom: 75px;
                                }
                                .navnOgDato {
                                    width: 100%;
                                }
                                .bold {
                                    font-weight: bold;
                                }
                                .underline {
                                    text-decoration: underline;
                                }
                                .italic {
                                    font-style: italic;
                                }
                                .alignRight {
                                    text-align: right;
                                }
                                .pageBreak {
                                    page-break-after: always;
                                }
                                
                                @page {
                                    margin: 15mm 20mm 30mm 20mm;
                                    @bottom-left {
                                        font-family: "Source Sans Pro" !important;
                                        font-size: 10pt;
                                        content: "$footer";
                                        margin: 100mm 200mm 50mm 20mm;
                                        padding-left: 3mm;
                                        white-space: pre-wrap;
                                        text-align: left;
                                    }
                                    @bottom-right {
                                        font-family: "Source Sans Pro" !important;
                                        font-size: 10pt;
                                        content: "Side " counter(page) " av " counter(pages);
                                        text-align: right;
                                    }
                                }
                            """.trimIndent()


    private val document: Document = createHTMLDocument()
        .html {
            body {
                div {
                    img { src = "nav_logo.png" }
                }
                div("tabellUnderLogo") {
                    table("navnOgDato") {
                        tr {
                            td {
                                div {
                                    id = "navn"
                                    p {
                                        +brevinnhold.navn.uppercase()
                                    }
                                }
                            }
                            td {
                                div {
                                    id = "dato"
                                    p {
                                        +"Dato: ${dateAsText(brevinnhold.dato)}"
                                    }
                                }
                            }
                        }
                    }
                }
                div { id = "div_content_id" }
            }
        }

    private var footer =
        """
            NAV Familie og Pensjonsytelser
            Postadresse: NAV Skanning Postboks 1400, 0109 Oslo. 
            Telefon 55 55 33 33 // nav.no
        """.trimIndent().replace("\n", "\\A")

    private fun addLabelContentElement(map: Map<String, *>) {
        val result = map["result"] ?: return

        val divElement = document.getElementById("div_content_id") as Node
        divElement.append {
            div {
                p { +"$result" }
            }
        }
    }

    private fun addMaltekst(map: Map<String, *>) {
        //unpack content
        val firstContent = map["content"] ?: return
        firstContent as List<Map<String, *>>
        val elementList = firstContent.firstOrNull()?.get("content") ?: return

        elementList as List<Map<String, *>>
        elementList.forEach {
            val div = document.create.div {
                this.addElementWithPossiblyChildren(it)
            }
            val divElement = document.getElementById("div_content_id") as Node
            divElement.appendChild(div)
        }
    }

    private fun addElementWithPossiblyChildren(map: Map<String, *>) {
        val div = document.create.div {
            this.addElementWithPossiblyChildren(map)
        }
        val divElement = document.getElementById("div_content_id") as Node
        divElement.appendChild(div)
    }

    private fun Tag.addElementWithPossiblyChildren(map: Map<String, *>) {
        val elementType = map["type"]
        var children = emptyList<Map<String, *>>()

        val applyClasses = if (map["textAlign"] == "text-align-right") mutableSetOf("alignRight") else mutableSetOf()
        if (elementType == "indent") {
            applyClasses += "indent"
        }

        if (elementType != "page-break") {
            children = map["children"] as List<Map<String, *>>
        } else {
            applyClasses.add("pageBreak")
        }

        val element = when (elementType) {
            "standard-text" -> SPAN(initialAttributes = emptyMap(), consumer = this.consumer)
            "heading-one" -> H1(initialAttributes = emptyMap(), consumer = this.consumer)
            "heading-two" -> H2(initialAttributes = emptyMap(), consumer = this.consumer)
            "heading-three" -> H3(initialAttributes = emptyMap(), consumer = this.consumer)
            "blockquote" -> BLOCKQUOTE(initialAttributes = emptyMap(), consumer = this.consumer)
            "paragraph" -> P(initialAttributes = emptyMap(), consumer = this.consumer)
            "bullet-list" -> UL(initialAttributes = emptyMap(), consumer = this.consumer)
            "numbered-list" -> OL(initialAttributes = emptyMap(), consumer = this.consumer)
            "list-item" -> LI(initialAttributes = emptyMap(), consumer = this.consumer)
            "table" -> TABLE(initialAttributes = emptyMap(), consumer = this.consumer)
            "table-row" -> TR(initialAttributes = emptyMap(), consumer = this.consumer)
            "table-cell" -> TD(initialAttributes = emptyMap(), consumer = this.consumer)
            "page-break", "list-item-container", "indent" -> DIV(initialAttributes = emptyMap(), consumer = this.consumer)
            else -> {
                logger.warn("unknown element type: $elementType")
                return
            }
        }

        element.visit {
            classes = applyClasses
            children.forEach {
                when (it.getType()) {
                    LEAF -> this.addLeafElement(it)
                    ELEMENT -> this.addElementWithPossiblyChildren(it)
                    else -> {}
                }
            }
        }
    }

    private fun addDocumentList(map: Map<String, *>) {
        val children = map["documents"] as List<Map<String, String>>
        val dElement = document.create.div {
            ul {
                children.forEach {
                    li { +it["title"].toString() }
                }
            }
        }
        val divElement = document.getElementById("div_content_id") as Node
        divElement.appendChild(dElement)
    }

    private fun addSignatureElement(map: Map<String, *>) {
        val dElement = document.create.div {
            classes = setOf("wrapper")
            if (map.containsKey("medunderskriver")) {
                val medunderskriver = map["medunderskriver"] as Map<String, Map<String, *>>
                div {
                    classes = setOf("column")
                    div { +medunderskriver["name"].toString() }
                    div { +medunderskriver["title"].toString() }
                }
            }
            if (map.containsKey("saksbehandler")) {
                val saksbehandler = map["saksbehandler"] as Map<String, Map<String, *>>
                div {
                    classes = setOf("column")
                    div { +saksbehandler["name"].toString() }
                    div { +saksbehandler["title"].toString() }
                }
            }
        }
        val divElement = document.getElementById("div_content_id") as Node
        divElement.appendChild(dElement)
    }

    private fun Tag.addLeafElement(map: Map<String, *>) {
        val text = map["text"] ?: throw RuntimeException("no content here")

        val classesToAdd = mutableSetOf<String>()
        if (map["bold"] == true) {
            classesToAdd += "bold"
        }
        if (map["underline"] == true) {
            classesToAdd += "underline"
        }
        if (map["italic"] == true) {
            classesToAdd += "italic"
        }

        this.consumer.span {
            classes = classesToAdd
            +text.toString()
        }
    }

    private fun addCurrentDate() {
        val div = document.create.div {
            classes = setOf("alignRight")
            +"Dato: ${dateAsText(null)}"
        }
        val divElement = document.getElementById("div_content_id") as Node
        divElement.appendChild(div)
    }

    private fun dateAsText(date: LocalDate?): String? {
        val formatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.forLanguageTag("nb"))
        return date?.format(formatter) ?: ZonedDateTime.now(ZoneId.of("Europe/Oslo")).format(formatter)
    }

    fun getDoc(): Document {
        val brevinnholdAsJson = jacksonObjectMapper().writeValueAsString(brevinnhold.tekst)
        val dataList = jacksonObjectMapper().readValue(brevinnholdAsJson, List::class.java) as List<Map<String, *>>
        dataList.forEach {
            processElement(it)
        }

        //defaults for now
//        if (!headerAndFooterExists(dataList)) {
//            val span = document.getElementById("header_text")
//            span.textContent = "Returadresse,\nNAV Klageinstans Midt-Norge, Postboks 2914 Torgarden, 7438 Trondheim"
//
//            footer = "Postadresse: NAV Klageinstans Midt-Norge // Postboks 2914 Torgarden // 7438 Trondheim\\ATelefon: 21 07 17 30\\Anav.no"
//        }


        //add css when we have a footer set
        val head = document.create.head {
            style {
                unsafe {
                    raw(
                        getCss(footer)
                    )
                }
            }
        }

        document.childNodes.item(0).appendChild(head)

        secureLogger.debug(document.serialize())
        return document
    }

    private fun headerAndFooterExists(list: List<Map<String, *>>) =
        list.any { it["type"] == "header" } && list.any { it["type"] == "footer" }

    private fun processElement(map: Map<String, *>) {
        when (map.getType()) {
            LABEL_CONTENT_ELEMENT -> addLabelContentElement(map)
            SIGNATURE_ELEMENT -> addSignatureElement(map)
            ELEMENT, INDENT -> addElementWithPossiblyChildren(map)
            DOCUMENT_LIST -> addDocumentList(map)
            MALTEKST -> addMaltekst(map)
            CURRENT_DATE -> addCurrentDate()
            HEADER -> addHeader(map)
            FOOTER -> setFooter(map)
            LEAF -> {}
            IGNORED -> {}
        }
    }

    private fun addHeader(map: Map<String, *>) {
        val span = document.getElementById("header_text")
        span.textContent = map["content"].toString()
    }

    private fun setFooter(map: Map<String, *>) {
        footer = map["content"].toString().replace("\n", "\\A")
    }

    private fun Map<String, *>.getType(): ElementType {
        val type = this["type"]
        if (type != null) {
            return when (type) {
                "label-content" -> LABEL_CONTENT_ELEMENT
                "signature" -> SIGNATURE_ELEMENT
                "document-list" -> DOCUMENT_LIST
                "maltekst" -> MALTEKST
                "current-date" -> CURRENT_DATE
                "header" -> HEADER
                "footer" -> FOOTER
                "redigerbar-maltekst", "regelverkstekst" -> IGNORED
                else -> ELEMENT
            }
        }
        return LEAF
    }
}

enum class ElementType {
    LABEL_CONTENT_ELEMENT,
    SIGNATURE_ELEMENT,
    ELEMENT,
    LEAF,
    DOCUMENT_LIST,
    MALTEKST,
    CURRENT_DATE,
    HEADER,
    FOOTER,
    INDENT,
    IGNORED,
}
