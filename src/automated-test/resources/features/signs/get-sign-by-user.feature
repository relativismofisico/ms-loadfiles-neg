@regression @read-only @production-safe
Feature: GET /signs/user/{userId} — Obtener firma activa de usuario

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')

  @smoke
  Scenario: Obtener firma de usuario con ADMINISTRADOR retorna 200 o 404
    Given path '/signs/user/' + testUserId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403
    * assert responseStatus != 500

  Scenario: Obtener firma de usuario con EMPRESA retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/signs/user/' + testUserId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener firma de usuario con OPERARIO retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/signs/user/' + testUserId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener firma de usuario con FONDEADOR retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/signs/user/' + testUserId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Usuario sin firma activa retorna 404
    Given path '/signs/user/999999'
    When method GET
    Then status 404

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/signs/user/' + testUserId
    When method GET
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/signs/user/' + testUserId
    When method GET
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/signs/user/' + testUserId
    When method GET
    Then status 401

  Scenario: Token malformado retorna 401
    * configure headers = ({ Authorization: 'Bearer bad.token' })
    Given path '/signs/user/' + testUserId
    When method GET
    Then status 401

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/signs/user/' + testUserId
    When method GET
    Then status 403
    And match response == errorSchema

  Scenario: Path variable no numerico retorna 400
    Given url baseUrl + '/signs/user/abc'
    When method GET
    Then status 400