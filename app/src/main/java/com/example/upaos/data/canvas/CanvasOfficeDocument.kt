package com.example.upaos.data.canvas

import java.io.File
import java.util.zip.ZipFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser

/** Local DOCX preview; never sends a signed document URL to an online converter. */
internal fun canvasDocxHtml(file: File): String = ZipFile(file).use { zip ->
    val entry = requireNotNull(zip.getEntry("word/document.xml")) { "Documento Word inválido" }
    val limit = 8 * 1024 * 1024
    require(entry.size <= limit) { "Documento demasiado grande para visualizar" }
    val bytes = zip.getInputStream(entry).use { it.readBytesBounded(limit) }
    val doc = Jsoup.parse(bytes.toString(Charsets.UTF_8), "", Parser.xmlParser())
    fun render(element: Element): String {
        val children = { element.children().joinToString("") { render(it) } }
        return when (element.tagName().substringAfter(':')) {
            "t" -> Entities.escape(element.wholeText())
            "tab" -> " &nbsp; "
            "br", "cr" -> "<br>"
            "p" -> "<p>" + children() + "</p>"
            "tbl" -> "<table>" + children() + "</table>"
            "tr" -> "<tr>" + children() + "</tr>"
            "tc" -> "<td>" + children() + "</td>"
            "r" -> {
                val text = children()
                val properties = element.children().firstOrNull { it.tagName() == "w:rPr" }
                if (properties?.children()?.any { it.tagName() == "w:b" && it.attr("w:val") !in setOf("0", "false") } == true) "<b>$text</b>" else text
            }
            "pPr", "rPr", "tblPr", "tcPr", "sectPr", "instrText" -> ""
            else -> children()
        }
    }
    val body = doc.getElementsByTag("w:body").first() ?: error("Documento sin contenido")
    render(body).also { require(Jsoup.parse(it).text().isNotBlank()) { "El documento no contiene texto visible" } }
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "Documento demasiado grande para visualizar" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
