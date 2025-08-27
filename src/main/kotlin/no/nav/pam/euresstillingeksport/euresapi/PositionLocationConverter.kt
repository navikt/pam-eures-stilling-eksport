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
        "TRØNDELAG" -> "NO060"
        "NORDLAND" -> "NO071"
        "TROMS", "FINNMARK" -> "NO074"
        "OSLO" -> "NO081"
        "ØSTFOLD", "AKERSHUS", "BUSKERUD" -> "NO082"
        "AGDER" -> "NO092"
        "VESTFOLD", "TELEMARK" -> "NO091"
        "ROGALAND" -> "NO0A1"
        "VESTLAND" -> "NO0A2"
        "MØRE OG ROMSDAL" -> "NO0A3"
        "JAN MAYEN" -> "NO0B1"
        "SVALBARD" -> "NO0B2"
        else -> null
    }
}
