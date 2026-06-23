@regression @production-safe
Feature: Validacion de tokens JWT

  Background:
    * url baseUrl

  Scenario: Token completamente invalido retorna 401
    * configure headers = ({ Authorization: 'Bearer not.a.valid.jwt' })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401

  Scenario: Header Authorization vacio retorna 401
    * configure headers = ({ Authorization: '' })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401

  Scenario: Token expirado retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenExpirado })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401

  Scenario: Token con firma invalida retorna 401
    * configure headers = ({ Authorization: 'Bearer ' + tokenFirmaInvalida })
    Given path '/filesign/filestosignregistry'
    When method GET
    Then status 401

  Scenario: Token valido de ADMINISTRADOR es aceptado
    * configure headers = ({ Authorization: 'Bearer ' + tokenAdmin })
    Given path '/filesign/filestosignregistry'
    When method GET
    * assert responseStatus != 401
    * assert responseStatus != 500
