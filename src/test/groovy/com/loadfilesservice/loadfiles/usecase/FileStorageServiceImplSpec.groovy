package com.loadfilesservice.loadfiles.usecase

import com.loadfilesservice.loadfiles.application.exception.InternalServerErrorException
import com.loadfilesservice.loadfiles.infraestrutura.usecase.FileStorageServiceImpl
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.Files

class FileStorageServiceImplSpec extends Specification {

    FileStorageServiceImpl service = new FileStorageServiceImpl()

    def "loadFile - existing readable file returns resource"() {
        given:
        def tempFile = File.createTempFile("test_load", ".pdf")
        tempFile.text = "PDF content"
        def fileName = tempFile.name
        def path = tempFile.parent

        when:
        def result = service.loadFile(fileName, path)

        then:
        result != null
        result.exists()

        cleanup:
        tempFile.delete()
    }

    def "loadFile - non-existent file throws InternalServerErrorException"() {
        given:
        def tmpDir = System.getProperty("java.io.tmpdir")

        when:
        service.loadFile("nonexistent_file_xyz_12345.pdf", tmpDir)

        then:
        thrown(InternalServerErrorException)
    }

    def "copyFile - with original filename strips spaces and prefixes UUID"() {
        given:
        def tempDir = Files.createTempDirectory("test_copy").toFile()
        def file = new MockMultipartFile("file", "my file.pdf", "application/pdf", "content".bytes)

        when:
        def result = service.copyFile(file, tempDir.absolutePath)

        then:
        result.endsWith("_myfile.pdf")
        new File(tempDir, result).exists()

        cleanup:
        new File(tempDir, result).delete()
        tempDir.delete()
    }

    def "copyFile - null original filename uses 'file' as default"() {
        given:
        def tempDir = Files.createTempDirectory("test_copy_null").toFile()
        def file = Mock(MultipartFile)
        file.getOriginalFilename() >> null
        file.getInputStream() >> new java.io.ByteArrayInputStream("content".bytes)

        when:
        def result = service.copyFile(file, tempDir.absolutePath)

        then:
        result.endsWith("_file")
        new File(tempDir, result).exists()

        cleanup:
        new File(tempDir, result).delete()
        tempDir.delete()
    }

    def "deleteFile - null filename returns false"() {
        expect:
        !service.deleteFile(null, "/tmp")
    }

    def "deleteFile - empty filename returns false"() {
        expect:
        !service.deleteFile("", "/tmp")
    }

    def "deleteFile - existing file deletes and returns true"() {
        given:
        def tempFile = File.createTempFile("test_delete", ".pdf")
        def fileName = tempFile.name
        def path = tempFile.parent

        when:
        def result = service.deleteFile(fileName, path)

        then:
        result == true
        !tempFile.exists()
    }

    def "deleteFile - non-existent file returns false"() {
        given:
        def tmpDir = System.getProperty("java.io.tmpdir")

        when:
        def result = service.deleteFile("does_not_exist_xyz_987654.pdf", tmpDir)

        then:
        result == false
    }

    def "createFolder - creates new directory successfully"() {
        given:
        def parentDir = Files.createTempDirectory("test_parent").toFile()
        def newFolderPath = new File(parentDir, "newSubFolder").absolutePath

        when:
        service.createFolder(newFolderPath)

        then:
        new File(newFolderPath).exists()
        new File(newFolderPath).isDirectory()

        cleanup:
        new File(newFolderPath).delete()
        parentDir.delete()
    }

    def "createFolder - existing directory does not throw"() {
        given:
        def existingDir = Files.createTempDirectory("test_existing").toFile()

        when:
        service.createFolder(existingDir.absolutePath)

        then:
        notThrown(Exception)

        cleanup:
        existingDir.delete()
    }

    def "createFolder - invalid path throws InternalServerErrorException"() {
        given:
        def tempFile = File.createTempFile("blocking", ".tmp")
        def invalidPath = tempFile.absolutePath + File.separator + "child"

        when:
        service.createFolder(invalidPath)

        then:
        thrown(InternalServerErrorException)

        cleanup:
        tempFile.delete()
    }

    def "getPath - returns absolute resolved path"() {
        when:
        def result = service.getPath("file.pdf", System.getProperty("java.io.tmpdir"))

        then:
        result.isAbsolute()
        result.toString().endsWith("file.pdf")
    }
}