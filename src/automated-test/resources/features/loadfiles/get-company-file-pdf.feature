@regression @read-only @production-safe
Feature: POST /api/companyfilepdf — Obtener PDF de archivo de empresa

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def validBody = { "id": #(testFileId), "ipLoad": "192.168.1.1" }

  @smoke
  Scenario: Obtener PDF con ADMINISTRADOR no retorna 401 ni 403
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener PDF con EMPRESA no retorna 401 ni 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener PDF con OPERARIO no retorna 401 ni 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener PDF con FONDEADOR no retorna 401 ni 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    Then status 401

  @smoke
  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/api/companyfilepdf'
    And request validBody
    When method POST
    Then status 403
    And match response == errorSchema