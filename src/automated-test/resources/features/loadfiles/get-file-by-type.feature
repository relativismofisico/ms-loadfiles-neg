@regression @read-only @production-safe
Feature: POST /api/companyfile/upload/file/{companyId} — Obtener nombre de archivo activo por tipo

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def fileTypeBody = { "id": 10, "description": "Registro", "fileTypeName": "REGISTRO" }

  @smoke
  Scenario: Obtener archivo por tipo con ADMINISTRADOR retorna 200
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 200

  Scenario: Obtener archivo por tipo con EMPRESA retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 200

  Scenario: Obtener archivo por tipo con OPERARIO retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 200

  Scenario: Obtener archivo por tipo con FONDEADOR retorna 200
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 200

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 401

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/api/companyfile/upload/file/' + testCompanyId
    And request fileTypeBody
    When method POST
    Then status 403
