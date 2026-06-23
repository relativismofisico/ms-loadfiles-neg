@regression @read-only @production-safe
Feature: GET /filesign/filestosignregistry — Listar archivos de registro pendientes de firma

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def fileToSignSchema = read('classpath:schemas/file-to-sign-response-schema.json')

  @smoke
  Scenario: Listar archivos por firmar con ADMINISTRADOR retorna 200 o 404
    Given path '/filesign/filestosignregistry'
    When method GET
    * assert responseStatus == 200 || responseStatus == 404
    * if (responseStatus == 200) karate.match(response, '#[] fileToSignSchema')

  Scenario: Listar archivos por firmar con OPERARIO retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/filesign/filestosignregistry'
    When method GET
    * assert responseStatus == 200 || responseStatus == 404

  Scenario: Listar archivos por firmar con FONDEADOR retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/filesign/filestosignregistry'
    When method GET
    * assert responseStatus == 200 || responseStatus == 404

  @smoke
  Scenario: EMPRESA no tiene acceso a revision retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 403
    And match response == errorSchema
    And match response.status == 403

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 403

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401

  Scenario: Token malformado retorna 401
    * configure headers = ({ Authorization: 'Bearer garbage' })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401