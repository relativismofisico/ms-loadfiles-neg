package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.ResourceNotFoundException
import com.loadfilesservice.loadfiles.domain.FileToSign
import com.loadfilesservice.loadfiles.infraestrutura.persistence.IFileToSignDao
import com.loadfilesservice.loadfiles.infraestrutura.usecase.FileToSignServiceImpl
import spock.lang.Specification

class FileToSignServiceImplSpec extends Specification {

    IFileToSignDao fileToSignDao = Mock()
    FileToSignServiceImpl service = new FileToSignServiceImpl(fileToSignDao)

    def "findByCompanyFileTypeAndState - delegates to dao"() {
        given:
        def files = [new FileToSign(), new FileToSign()]
        fileToSignDao.findByCompanyFileTypeAndState(10L, 1L) >> files

        when:
        def result = service.findByCompanyFileTypeAndState(10L, 1L)

        then:
        result.size() == 2
    }

    def "findByCompanyFileTypeAndState - empty result"() {
        given:
        fileToSignDao.findByCompanyFileTypeAndState(99L, 0L) >> []

        when:
        def result = service.findByCompanyFileTypeAndState(99L, 0L)

        then:
        result.isEmpty()
    }

    def "findById - returns result when file exists"() {
        given:
        def fileToSign = new FileToSign()
        fileToSign.id = 1L
        fileToSign.fileName = "tosign.pdf"
        fileToSignDao.findById(1L) >> Optional.of(fileToSign)

        when:
        def result = service.findById(1L)

        then:
        result.isPresent()
        result.get().id == 1L
        result.get().fileName == "tosign.pdf"
    }

    def "findById - throws ResourceNotFoundException when not found"() {
        given:
        fileToSignDao.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        thrown(ResourceNotFoundException)
    }
}