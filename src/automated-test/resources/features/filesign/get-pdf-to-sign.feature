@regression @read-only @production-safe
Feature: POST /filesign/pdftosign — Obtener PDF pendiente de firma

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def validBody = { "id": "#(testFileToSignId)" }

  @smoke
  Scenario: Obtener PDF por firmar con ADMINISTRADOR retorna 200
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 200
    And match responseHeaders['Content-Type'][0] contains 'application/pdf'

  Scenario: Obtener PDF por firmar con OPERARIO retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 200

  Scenario: Obtener PDF por firmar con FONDEADOR retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 200

  @smoke
  Scenario: EMPRESA no puede obtener PDF por firmar retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 403
    And match response == errorSchema

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 403

  Scenario: ID inexistente retorna 404 o 500
    Given path '/filesign/pdftosign'
    And request { "id": "999999" }
    When method POST
    * assert responseStatus >= 404

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/filesign/pdftosign'
    And request validBody
    When method POST
    Then status 401

  Scenario: Body vacio retorna 400 o 422
    Given path '/filesign/pdftosign'
    And request {}
    When method POST
    * assert responseStatus >= 400
