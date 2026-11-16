package com.example.upaos

import com.example.upaos.data.canvas.CanvasDocuments
import com.example.upaos.data.canvas.canvasDocumentExtension
import com.example.upaos.data.model.CanvasFileDetail
import org.junit.Test
import org.junit.Assert.*

class CanvasDocumentTransportTest {
    @Test fun permitsOfficialCanvasFileStorageAndRejectsSpoofedDestinations() {
        assertNotNull(CanvasDocuments.safeUrl("https://inst-fs-iad-prod.inscloudgate.net/synthetic/file.pdf"))
        assertNotNull(CanvasDocuments.safeUrl("https://cdn.inst-fs-iad-prod.inscloudgate.net/synthetic/file.pdf"))
        assertNull(CanvasDocuments.safeUrl("https://inst-fs-iad-prod.inscloudgate.net.evil.test/file.pdf"))
        assertNull(CanvasDocuments.safeUrl("https://evilinscloudgate.net/file.pdf"))
        assertNull(CanvasDocuments.safeUrl("http://inst-fs-iad-prod.inscloudgate.net/file.pdf"))
        assertNull(CanvasDocuments.safeUrl("https://user:secret@inst-fs-iad-prod.inscloudgate.net/file.pdf"))
        assertNull(CanvasDocuments.safeUrl("https://inst-fs-iad-prod.inscloudgate.net:8443/file.pdf"))
    }
    @Test fun keepsTheActualFileTypeWhenTheTeacherRenamesItsDisplayTitle() {
        assertEquals("pdf", canvasDocumentExtension(CanvasFileDetail(1, displayName="Material de clase", filename="clase.PDF")))
        assertEquals("docx", canvasDocumentExtension(CanvasFileDetail(1, displayName="Lectura", filename="clase.docx")))
        assertEquals("pdf", canvasDocumentExtension(CanvasFileDetail(1, displayName="Lectura", contentType="application/pdf")))
        assertEquals("bin", canvasDocumentExtension(CanvasFileDetail(1, filename="invalid.extension-is-too-long")))
    }
}
