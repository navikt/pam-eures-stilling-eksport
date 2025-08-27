package no.nav.pam.euresstillingeksport.euresapi

import no.nav.pam.euresstillingeksport.model.Location


fun Location.toPositionLocation(): PositionLocation {
    return PositionLocation(Address(
            cityName = city,
            addressLine = address,
            countryCode = landskode ?: "NO",
            countrySubDivisionCode = countyToCountrySubDivisionCode(county),
            postalCode = postalCode
    ))
}

fun countyToCountrySubDivisionCode(county: String?): String? {
    return when (county?.uppercase()) {
        "TROMS" -> "NO072"
        "FINNMARK" -> "NO073"
        "OSLO" -> "NO081"
        "ØSTFOLD" -> "NO083"
        "AKERSHUS" -> "NO084"
        "BUSKERUD" -> "NO085"
        "AGDER" -> "NO092"
        "VESTFOLD" -> "NO093"
        "TELEMARK" -> "NO094"
        "ROGALAND" -> "NO0A1"
        "VESTLAND" -> "NO0A2"
        "MØRE OG ROMSDAL" -> "NO0A3"
        "JAN MAYEN" -> "NO0B1"
        "SVALBARD" -> "NO0B2"
        else -> null
    }
}
