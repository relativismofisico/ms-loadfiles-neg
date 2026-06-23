@full
Feature: POST /api/companyfile/upload — Subir archivo de empresa (multipart)

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def validFileInfo =
      """
      {
        "originalFileName": "registro-empresa.pdf",
        "ipLoad": "#(testIpLoad)",
        "companyName": "#(testCompanyName)",
        "company": #(testCompanyId),
        "user": #(testUserId),
        "companyFileType": { "id": 10, "description": "Registro", "fileTypeName": "REGISTRO" }
      }
      """

  @smoke
  Scenario: Subir archivo con ADMINISTRADOR no retorna 401 ni 403
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Subir archivo con EMPRESA no retorna 401 ni 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'empresa.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  Scenario: Subir archivo con OPERARIO no retorna 401 ni 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'operario.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    * assert responseStatus != 401
    * assert responseStatus != 403

  @smoke
  Scenario: FONDEADOR no puede subir archivos retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    Then status 403
    And match response == errorSchema

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    Then status 403

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    Then status 401

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/api/companyfile/upload'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileInfo
    When method POST
    Then status 401