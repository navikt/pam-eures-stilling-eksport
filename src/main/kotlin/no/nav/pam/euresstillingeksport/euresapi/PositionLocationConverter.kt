package no.nav.pam.euresstillingeksport.euresapi

import no.nav.pam.euresstillingeksport.model.Location


fun Location.toPositionLocation(): PositionLocation {
    return PositionLocation(Address(
            cityName = city,
            addressLine = address,
            countryCode = landskode ?: "NO",
            countrySubDivisionCode = null,
            postalCode = postalCode
    ))
}
