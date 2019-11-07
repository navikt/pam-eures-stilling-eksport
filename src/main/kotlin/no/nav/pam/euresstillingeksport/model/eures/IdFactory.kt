package no.nav.pam.euresstillingeksport.model.eures

// <DocumentID schemeID=\"NAV-001\" schemeAgencyID=\"NAV\" schemeAgencyName=\"NAV PES\" schemeVersionID=\"1.3\">07101911000020</DocumentID>
// <DocumentID schemeID=\"NAV-001\" schemeAgencyID=\"FINN1\" schemeAgencyName=\"Finn.no\" schemeVersionID=\"1.3\">10011911000020</DocumentID>
// <DocumentID schemeID=\"NAV-001\" schemeAgencyID=\"NAV\" schemeAgencyName=\"Nav public employment services\" schemeVersionID=\"1.3\">03161911000059</DocumentID>

object IdFactory {

    fun id(id: String, schemeId: String, schemeAgencyID: String, schemeAgencyName: String): Id {
        return Id(documentId(id, schemeId, schemeAgencyID, schemeAgencyName))
    }

    fun documentId(id: String, schemeId: String, schemeAgencyID: String, schemeAgencyName: String) : DocumentId {
        return DocumentId(
                uuid = id,
                schemeID = schemeId,
                schemeAgencyID = schemeAgencyID,
                schemeAgencyName = schemeAgencyName,
                schemeVersionID = "1.3")
    }

}