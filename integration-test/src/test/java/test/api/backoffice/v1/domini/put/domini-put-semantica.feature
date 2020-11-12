Feature: Validazione semantica domini

Background:

* callonce read('classpath:utils/common-utils.feature')
* callonce read('classpath:configurazione/v1/anagrafica.feature')
* def basicAutenticationHeader = getBasicAuthenticationHeader( { username: govpay_backoffice_user, password: govpay_backoffice_password } )
* def backofficeBaseurl = getGovPayApiBaseUrl({api: 'backoffice', versione: 'v1', autenticazione: 'basic'})
* def dominio = read('classpath:test/api/backoffice/v1/domini/put/msg/dominio.json')

Given url backofficeBaseurl
And path 'domini', idDominio
And headers basicAutenticationHeader
When method get
Then status 200


Scenario: Dominio pluri-intermediato senza codice segregazione

* set dominio.auxDigit = 3
* set dominio.segregationCode = null

Given url backofficeBaseurl
And path 'domini', idDominio
And headers basicAutenticationHeader
And request dominio
When method put
Then status 422

* match response == { categoria: 'RICHIESTA', codice: 'SEMANTICA', descrizione: 'Richiesta non valida', dettaglio: '#notnull' }
* match response.dettaglio contains 'segregationCode'

Scenario Outline: Prefissi IUV non validi

* set dominio.iuvPrefix = <prefix>

Given url backofficeBaseurl
And path 'domini', idDominio
And headers basicAutenticationHeader
And request dominio
When method put
Then status 422

* match response == { categoria: 'RICHIESTA', codice: 'SEMANTICA', descrizione: 'Richiesta non valida', dettaglio: '#notnull' }

Examples:
| prefix |
| '%(Y)%(y)%(t)12345678901234567890' |

Scenario: Dominio associato ad una stazione non esistente

* set dominio.stazione = '12233221113_01'

Given url backofficeBaseurl
And path 'domini', idDominio
And headers basicAutenticationHeader
And request dominio
When method put
Then status 422

* match response == { categoria: 'RICHIESTA', codice: 'SEMANTICA', descrizione: 'Richiesta non valida', dettaglio: '#notnull' }
* match response.dettaglio contains '#("La stazione intermediaria " + dominio.stazione + " indicata non esiste.")' 

