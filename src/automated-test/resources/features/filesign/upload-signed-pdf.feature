@full
Feature: POST /filesign/uploadsignedpdf — Subir PDF firmado (multipart)

  Background:
    * url baseUrl
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    * def errorSchema = read('classpath:schemas/error-response-schema.json')
    * def validFileSignedInfo =
      """
      {
        "originalFileName": "registro-empresa-firmado.pdf",
        "ipLoad": "#(testIpLoad)",
        "companyName": "#(testCompanyName)",
        "company": #(testCompanyId),
        "user": #(testUserId),
        "companyFileType": { "id": 10, "description": "Registro", "fileTypeName": "REGISTRO" }
      }
      """

  @smoke
  Scenario: Subir PDF firmado con ADMINISTRADOR retorna 201
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'signed-test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 201
    And match response == { saveFile: true }

  Scenario: Subir PDF firmado con OPERARIO retorna 201
    * configure headers = ({ Authorization: 'Bearer ' + tokenOperario })
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'signed-operario.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 201

  @smoke
  Scenario: EMPRESA no puede subir PDF firmado retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenEmpresa })
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 403
    And match response == errorSchema

  Scenario: FONDEADOR no puede subir PDF firmado retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenFondeador })
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 403

  Scenario: Rol sin permiso retorna 403
    * configure headers = ({ Authorization: 'Bearer ' + tokenSinAcceso })
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 403

  @smoke
  Scenario: Sin JWT retorna 401
    * configure headers = null
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 401

  Scenario: JWT expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 401

  Scenario: JWT firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/filesign/uploadsignedpdf'
    And multipart file file = { read: 'classpath:testfiles/dummy.pdf', filename: 'test.pdf', contentType: 'application/pdf' }
    And multipart field fileinfo = validFileSignedInfo
    When method POST
    Then status 401
