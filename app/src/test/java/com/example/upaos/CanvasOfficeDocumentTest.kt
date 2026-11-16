package com.example.upaos

import com.example.upaos.data.canvas.canvasDocxHtml
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CanvasOfficeDocumentTest {
    private fun docx(xml: String, check: (File) -> Unit) {
        val file = File.createTempFile("canvas-synthetic-", ".docx")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("word/document.xml")); zip.write(xml.toByteArray()); zip.closeEntry()
            }
            check(file)
        } finally { file.delete() }
    }
    @Test fun preservesParagraphsTablesAndEscapesDocumentText() {
        docx("""<w:document xmlns:w="urn:word"><w:body><w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Title &amp; content</w:t></w:r></w:p><w:tbl><w:tr><w:tc><w:p><w:r><w:t>&lt;script&gt;bad&lt;/script&gt;</w:t></w:r></w:p></w:tc></w:tr></w:tbl></w:body></w:document>""") { file ->
            val html = canvasDocxHtml(file)
            assertTrue(html.contains("<b>Title &amp; content</b>"))
            assertTrue(html.contains("<table><tr><td>"))
            assertTrue(html.contains("&lt;script&gt;")); assertFalse(html.contains("<script>"))
            assertFalse(html.contains("w:rPr"))
        }
    }
    @Test fun rejectsAnEmptyOrNonWordArchive() {
        docx("""<w:document xmlns:w="urn:word"><w:body/></w:document>""") { file ->
            assertTrue(runCatching { canvasDocxHtml(file) }.isFailure)
        }
    }
}
