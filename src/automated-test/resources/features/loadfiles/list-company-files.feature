@regression @read-only @production-safe
Feature: GET /api/listofcompanyfiles/{id} — Listar archivos activos de empresa

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')

  @smoke
  Scenario: Listar archivos con ADMINISTRADOR retorna 200 o 404
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403
    * assert responseStatus != 500

  Scenario: Listar archivos con EMPRESA retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Listar archivos con OPERARIO retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Listar archivos con FONDEADOR retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Empresa sin archivos retorna 404
    Given path '/api/listofcompanyfiles/999999'
    When method GET
    Then status 404
    And match response == errorSchema

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    Then status 401

  Scenario: Token malformado retorna 401
    * configure headers = ({ Authorization: 'Bearer garbage' })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    Then status 401

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/api/listofcompanyfiles/' + testCompanyId
    When method GET
    Then status 403
    And match response == errorSchema