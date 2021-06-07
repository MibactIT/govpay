Feature: Ricerca per filtri sui metadati di paginazione

Background: 

* callonce read('classpath:utils/common-utils.feature')
* callonce read('classpath:configurazione/v1/anagrafica.feature')

* def pathServizioDomini = '/domini'
* def pathServizio = '/tipiPendenza'

Scenario: Ricerca tipiPendenza senza filtri sui metadati di paginazione

Given url backofficeBaseurl
And path pathServizioDomini, idDominio, pathServizio
And headers gpAdminBasicAutenticationHeader
When method get
Then status 200
And match response == 
"""
{
	numRisultati: '#notnull',
	numPagine: '#notnull',
	risultatiPerPagina: '#notnull',
	pagina: '#notnull',
	prossimiRisultati: '##null',
	risultati: '#[]'
}
"""

Scenario: Ricerca tipiPendenza con metadatiPaginazione true

Given url backofficeBaseurl
And path pathServizioDomini, idDominio, pathServizio
And param metadatiPaginazione = true
And headers gpAdminBasicAutenticationHeader
When method get
Then status 200
And match response == 
"""
{
	numRisultati: '#notnull',
	numPagine: '#notnull',
	risultatiPerPagina: '#notnull',
	pagina: '#notnull',
	prossimiRisultati: '##null',
	risultati: '#[]'
}
"""

Scenario: Ricerca tipiPendenza con metadatiPaginazione false

Given url backofficeBaseurl
And path pathServizioDomini, idDominio, pathServizio
And param metadatiPaginazione = false
And headers gpAdminBasicAutenticationHeader
When method get
Then status 200
And match response == 
"""
{
	numRisultati: '#notnull',
	numPagine: '#notnull',
	risultatiPerPagina: '#notnull',
	pagina: '#notnull',
	prossimiRisultati: '##null',
	risultati: '#[]'
}
"""

Scenario: Ricerca tipiPendenza con metadatiPaginazione true e risultatiPerPagina = 0

Given url backofficeBaseurl
And path pathServizioDomini, idDominio, pathServizio
And param metadatiPaginazione = true
And param risultatiPerPagina = 0
And headers gpAdminBasicAutenticationHeader
When method get
Then status 200
And match response == 
"""
{
	numRisultati: '#notnull',
	numPagine: '#notpresent',
	risultatiPerPagina: 0,
	pagina: '#notnull',
	prossimiRisultati: '#notpresent',
	risultati: '#[0]'
}
"""

Scenario: Ricerca tipiPendenza con maxRisultati true

Given url backofficeBaseurl
And path pathServizioDomini, idDominio, pathServizio
And param maxRisultati = true
And headers gpAdminBasicAutenticationHeader
When method get
Then status 200
And match response == 
"""
{
	numRisultati: '#notnull',
	numPagine: '#notnull',
	risultatiPerPagina: '#notnull',
	pagina: '#notnull',
	prossimiRisultati: '##null',
	risultati: '#[]'
}
"""

Scenario: Ricerca tipiPendenza con maxRisultati false

Given url backofficeBaseurl
And path pathServizioDomini, idDominio, pathServizio
And param maxRisultati = false
And headers gpAdminBasicAutenticationHeader
When method get
Then status 200
And match response == 
"""
{
	numRisultati: '#notnull',
	numPagine: '#notnull',
	risultatiPerPagina: '#notnull',
	pagina: '#notnull',
	prossimiRisultati: '##null',
	risultati: '#[]'
}
"""


