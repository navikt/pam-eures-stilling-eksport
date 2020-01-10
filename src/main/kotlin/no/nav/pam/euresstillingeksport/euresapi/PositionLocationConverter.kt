package no.nav.pam.euresstillingeksport.euresapi

import no.nav.pam.euresstillingeksport.model.Location
import no.nav.pam.geography.CountryDAO


fun Location.toPositionLocation(): PositionLocation {
    return PositionLocation(Address(
            cityName = city,
            addressLine = address,
            countryCode = countryToCountryCode(country),
            countrySubDivisionCode = null,
            postalCode = postalCode
    ))
}

private val countries: CountryDAO = CountryDAO()

private fun countryToCountryCode(country: String?): String {

    return countries.findCountry(country)
            .map { it.code }
            .map { if( it == "GB") "UK" else it } // Spesial-håndtering av Storbritannia i EUs kodeverk
            .map { if( it == "GR") "EL" else it } // Spesial-håndtering av Hellas i EUs kodeverk
            .orElse("NO") // TODO Burde filtrere bort stillinger utenfor EU
}

