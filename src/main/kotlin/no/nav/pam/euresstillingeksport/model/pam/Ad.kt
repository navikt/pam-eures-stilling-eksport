package no.nav.pam.euresstillingeksport.model.pam

import java.time.LocalDateTime

data class Ad(
        private val id: Long,
        private val uuid: String,
        private val created: LocalDateTime,
        private val createdBy: String?,
        private val updated: LocalDateTime,
        private val updatedBy: String?,
        //private val mediaList: [],
        //private val contactList [],
        private val locationList: List<Location>,
        private val properties: Map<String, String>,
        private val title: String?,
        private val status: String?,
        private val privacy: String?,
        private val source: String?,
        private val medium: String?,
        private val reference: String?,
        private val published: LocalDateTime?,
        private val expires: LocalDateTime?,
        private val employer: Employer,
        private val categoryList: List<Category>,
        private val administration: Administration,
        private val publishedByAdmin: String,
        private val businessName: String,
        private val firstPublished: Boolean,
        private val deactivatedByExpiry: Boolean,
        private val activationOnPublishingDate: Boolean
)


data class Location(
        private val address: String?,
        private val postalCode: String?,
        private val county: String?,
        private val municipal: String?,
        private val municipalCode: String?,
        private val city: String?,
        private val country: String?,
        private val latitude: String?,
        private val longitude: String?
)

data class Category(
        private val id: Long,
        private val code: String?,
        private val categoryType: String?,
        private val name: String?,
        private val description: String?,
        private val parentId: Long?
)

data class Administration(
        private val id: Long,
        private val status: String,
        private val comments: String,
        private val reportee: String,
        private val remarks: List<String>,
        private val navIdent: String
)

data class Employer(
        private val id: Long,
        private val uuid: String,
        private val created: String?,
        private val createdBy: String?,
        private val updated: String?,
        private val updatedBy: String?,
        // private val mediaList: [],
        // private val contactList: [],
        private val locationList: List<Location>,
        private val properties: List<String>,

        private val name: String?,
        private val orgnr: String?,
        private val status: String?,
        private val parentOrgnr: String?,
        private val publicName: String?,
        private val deactivated: Boolean,
        private val orgform: String?,
        private val employees: Int
)