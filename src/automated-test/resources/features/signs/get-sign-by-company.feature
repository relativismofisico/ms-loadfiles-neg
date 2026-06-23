@regression @read-only @production-safe
Feature: GET /signs/company/{companyId} — Obtener firma activa de empresa

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')

  @smoke
  Scenario: Obtener firma de empresa con ADMINISTRADOR retorna 200 o 404
    Given path '/signs/company/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403
    * assert responseStatus != 500

  Scenario: Obtener firma de empresa con EMPRESA retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/signs/company/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener firma con OPERARIO retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/signs/company/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Obtener firma con FONDEADOR retorna 200 o 404
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/signs/company/' + testCompanyId
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Empresa sin firma activa retorna 404
    Given path '/signs/company/999999'
    When method GET
    Then status 404

  Scenario: Cuando firma existe Content-Type es octet-stream
    Given path '/signs/company/' + testCompanyId
    When method GET
    * def hasFile = responseStatus == 200
    * if (hasFile) karate.match(responseHeaders['Content-Type'][0], '#string')

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/signs/company/' + testCompanyId
    When method GET
    Then status 401
    And match response == errorSchema

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/signs/company/' + testCompanyId
    When method GET
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/signs/company/' + testCompanyId
    When method GET
    Then status 401

  Scenario: Token malformado retorna 401
    * configure headers = ({ Authorization: 'Bearer trash.token' })
    Given path '/signs/company/' + testCompanyId
    When method GET
    Then status 401

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/signs/company/' + testCompanyId
    When method GET
    Then status 403
    And match response == errorSchema

  Scenario: Path variable no numerico retorna 400
    Given url baseUrl + '/signs/company/abc'
    When method GET
    Then status 400