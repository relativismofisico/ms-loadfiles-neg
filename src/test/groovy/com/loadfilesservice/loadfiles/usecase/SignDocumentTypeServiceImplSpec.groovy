package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.domain.SignDocumentType
import com.loadfilesservice.loadfiles.infraestrutura.persistence.ISignDocumentTypeDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.SignDocumentTypeServiceImpl
import spock.lang.Specification

class SignDocumentTypeServiceImplSpec extends Specification {

    ISignDocumentTypeDao signDocumentTypeDao = Mock()
    SignDocumentTypeServiceImpl service = new SignDocumentTypeServiceImpl(signDocumentTypeDao)

    def "findAllActive - delegates to dao"() {
        given:
        def types = [new SignDocumentType(), new SignDocumentType()]
        signDocumentTypeDao.findAllByActivoTrue() >> types

        when:
        def result = service.findAllActive()

        then:
        result.size() == 2
    }

    def "findAll - returns every type including inactive"() {
        given:
        def types = [new SignDocumentType(), new SignDocumentType(), new SignDocumentType()]
        signDocumentTypeDao.findAll() >> types

        when:
        def result = service.findAll()

        then:
        result.size() == 3
    }

    def "findById - returns result when it exists"() {
        given:
        def type = new SignDocumentType()
        type.id = 1L
        type.name = "Términos y uso de la plataforma"
        signDocumentTypeDao.findById(1L) >> Optional.of(type)

        when:
        def result = service.findById(1L)

        then:
        result.isPresent()
        result.get().name == "Términos y uso de la plataforma"
    }

    def "findById - returns empty when it does not exist"() {
        given:
        signDocumentTypeDao.findById(99L) >> Optional.empty()

        when:
        def result = service.findById(99L)

        then:
        result.isEmpty()
    }

    def "save - delegates to dao"() {
        given:
        def type = new SignDocumentType()
        signDocumentTypeDao.save(type) >> type

        when:
        def result = service.save(type)

        then:
        result == type
    }

    def "deactivate - sets activo=false and saves"() {
        given:
        def type = new SignDocumentType()
        type.id = 1L
        type.activo = true
        signDocumentTypeDao.findById(1L) >> Optional.of(type)

        when:
        service.deactivate(1L)

        then:
        1 * signDocumentTypeDao.save({ it.activo == false })
    }

    def "deactivate - throws ResourceNotFoundException when it does not exist"() {
        given:
        signDocumentTypeDao.findById(99L) >> Optional.empty()

        when:
        service.deactivate(99L)

        then:
        thrown(ResourceNotFoundException)
        0 * signDocumentTypeDao.save(_)
    }
}
