package no.nav.pam.euresstillingeksport.model.pam

import no.nav.pam.euresstillingeksport.model.eures.Address
import no.nav.pam.euresstillingeksport.model.eures.PositionLocation


fun Location.toPositionLocation(): PositionLocation {
    return PositionLocation(Address(
            cityName = city,
            addressLine = address,
            countryCode = countryToCountryCode(country),
            countrySubDivisionCode = null,
            postalCode = postalCode

    ))
}

fun countryToCountryCode(country: String?): String { // TODO kodeverk-ting
    return when(country){
        "NORGE" -> "NO"
        else -> "NO"
    }
}
