package com.loadfilesservice.loadfiles.web.dto;

import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.FileSigned;
import com.loadfilesservice.loadfiles.domain.FileToSign;

@Component
public class Converter {

    public CompanyFileDTOResponse companyFileToDTO(CompanyFile companyFile) {
        CompanyFileDTOResponse response = new CompanyFileDTOResponse();
        response.setId(companyFile.getId());
        response.setFileName(companyFile.getFileName());
        if (companyFile.getLoadTime() != null) {
            response.setLoadTime(Date.from(companyFile.getLoadTime()
                    .atZone(ZoneId.systemDefault()).toInstant()));
        }
        response.setIpLoad(companyFile.getIpLoad());
        response.setCompany(companyFile.getCompany());
        response.setCompanyFileType(companyFile.getCompanyFileType());
        return response;
    }

    public CompanyFile companyFileDTOtoCompanyFile(CompanyFileDTORequest companyFileDTO) {
        CompanyFile companyFile = new CompanyFile();
        companyFile.setIpLoad(companyFileDTO.getIpLoad());
        companyFile.setCompany(companyFileDTO.getCompany());
        companyFile.setCompanyFileType(companyFileDTO.getCompanyFileType());
        return companyFile;
    }

    public FileToSignDTOResponse fileToSignToDTO(FileToSign fileToSign) {
        FileToSignDTOResponse response = new FileToSignDTOResponse();
        response.setId(fileToSign.getId());
        response.setFileName(fileToSign.getFileName());
        response.setFilePath(fileToSign.getFilePath());
        response.setCompanyFileType(fileToSign.getCompanyFileType());
        return response;
    }

    public FileSigned fileSignedDtoToFileSigned(FileSignedDTORequest fileSignedDTO) {
        FileSigned fileSigned = new FileSigned();
        fileSigned.setOriginalFileName(fileSignedDTO.getOriginalFileName());
        fileSigned.setIpLoad(fileSignedDTO.getIpLoad());
        fileSigned.setCompany(fileSignedDTO.getCompany());
        fileSigned.setUser(fileSignedDTO.getUser());
        fileSigned.setCompanyFileType(fileSignedDTO.getCompanyFileType());
        return fileSigned;
    }

}
