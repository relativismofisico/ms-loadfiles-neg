@full
Feature: POST /signs/save — Guardar o reemplazar firma digital de empresa

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def signSchema = read('classpath:schemas/sign-response-schema.json')

  @smoke
  Scenario: Guardar firma con ADMINISTRADOR retorna 201 y Sign valido
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 201
    And match response == signSchema

  Scenario: Guardar firma con EMPRESA retorna 201
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = '0'
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 201

  Scenario: user=0 es aceptado y mapeado a null
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = '0'
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 201
    And match response.user == '#null'

  @smoke
  Scenario: OPERARIO no puede guardar firma retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 403
    And match response == errorSchema

  Scenario: FONDEADOR no puede guardar firma retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 403

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 403

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 401

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/signs/save'
    And multipart file file = { read: 'classpath:testfiles/dummy-sign.png', filename: 'firma.png', contentType: 'image/png' }
    And multipart field company = testCompanyId
    And multipart field user = testUserId
    And multipart field ipLoad = testIpLoad
    And multipart field companyName = testCompanyName
    When method POST
    Then status 401
