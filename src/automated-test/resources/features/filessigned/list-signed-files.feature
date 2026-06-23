@regression @read-only @production-safe
Feature: GET /filessigned/listofcompanysignedfiles/{id} — Listar archivos firmados de empresa

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def fileSignedSchema = read('classpath:schemas/file-signed-response-schema.json')

  @smoke
  Scenario: Listar archivos firmados con ADMINISTRADOR retorna 200 o 404
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    * assert responseStatus == 200 || responseStatus == 404
    * if (responseStatus == 200) karate.match(response, '#[] fileSignedSchema')

  Scenario: Listar archivos firmados con EMPRESA retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    * assert responseStatus == 200 || responseStatus == 404

  Scenario: Listar archivos firmados con OPERARIO retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    * assert responseStatus == 200 || responseStatus == 404

  Scenario: Listar archivos firmados con FONDEADOR retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    * assert responseStatus == 200 || responseStatus == 404

  Scenario: Empresa sin archivos firmados retorna 404
    Given path '/filessigned/listofcompanysignedfiles/999999'
    When method GET
    Then status 404
    And match response == errorSchema

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    Then status 401

  Scenario: Token malformado retorna 401
    * configure headers = ({ Authorization: 'Bearer invalid.token.here' })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    Then status 401

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/filessigned/listofcompanysignedfiles/' + testCompanyId
    When method GET
    Then status 403
    And match response == errorSchema