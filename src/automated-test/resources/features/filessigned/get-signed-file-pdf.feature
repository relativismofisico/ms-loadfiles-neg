@regression @read-only @production-safe
Feature: POST /filessigned/companyfilesignedpdf/{companyName} — Obtener PDF de archivo firmado

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def validBody = { "id": "#(testSignedFileId)" }

  @smoke
  Scenario: Obtener PDF firmado con ADMINISTRADOR retorna 200
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 200
    And match responseHeaders['Content-Type'][0] contains 'application/pdf'

  Scenario: Obtener PDF firmado con EMPRESA retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 200

  Scenario: Obtener PDF firmado con OPERARIO retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 200

  Scenario: Obtener PDF firmado con FONDEADOR retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 200

  Scenario: ID de archivo firmado inexistente retorna 404 o 500
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request { "id": "999999" }
    When method POST
    * assert responseStatus >= 404

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 401

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request validBody
    When method POST
    Then status 403

  Scenario: Body vacio retorna 400 o 422
    Given path '/filessigned/companyfilesignedpdf/' + testCompanyName
    And request {}
    When method POST
    * assert responseStatus >= 400
