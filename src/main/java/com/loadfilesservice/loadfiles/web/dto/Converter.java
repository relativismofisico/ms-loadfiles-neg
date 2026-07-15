package com.loadfilesservice.loadfiles.web.dto;

import java.time.ZoneId;
import java.util.Date;

import com.loadfilesservice.loadfiles.domain.CompanyFile;
import com.loadfilesservice.loadfiles.domain.FileSigned;
import com.loadfilesservice.loadfiles.domain.FileToSign;
import org.springframework.stereotype.Component;

/** Componente que convierte entre entidades y DTOs. */
@Component
public class Converter {

    /** Convierte un CompanyFile a su DTO de respuesta. */
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
        String reviewStatus = companyFile.getReviewStatus();
        if (reviewStatus == null) {
            reviewStatus = "PENDIENTE";
        }
        response.setReviewStatus(reviewStatus);
        response.setRejectionReason(companyFile.getRejectionReason());
        return response;
    }

    /** Convierte un DTO de solicitud a la entidad CompanyFile. */
    public CompanyFile companyFileDTOtoCompanyFile(CompanyFileDTORequest companyFileDTO) {
        CompanyFile companyFile = new CompanyFile();
        companyFile.setIpLoad(companyFileDTO.getIpLoad());
        companyFile.setCompany(companyFileDTO.getCompany());
        companyFile.setCompanyFileType(companyFileDTO.getCompanyFileType());
        return companyFile;
    }

    /** Convierte un FileToSign a su DTO de respuesta. */
    public FileToSignDTOResponse fileToSignToDTO(FileToSign fileToSign) {
        FileToSignDTOResponse response = new FileToSignDTOResponse();
        response.setId(fileToSign.getId());
        response.setFileName(fileToSign.getFileName());
        response.setFilePath(fileToSign.getFilePath());
        response.setSignDocumentType(fileToSign.getSignDocumentType());
        return response;
    }

    /** Convierte un DTO de solicitud de archivo firmado a la entidad FileSigned. */
    public FileSigned fileSignedDtoToFileSigned(FileSignedDTORequest fileSignedDTO) {
        FileSigned fileSigned = new FileSigned();
        fileSigned.setOriginalFileName(fileSignedDTO.getOriginalFileName());
        fileSigned.setIpLoad(fileSignedDTO.getIpLoad());
        fileSigned.setCompany(fileSignedDTO.getCompany());
        fileSigned.setUser(fileSignedDTO.getUser());
        fileSigned.setSignDocumentType(fileSignedDTO.getSignDocumentType());
        return fileSigned;
    }

    /** Convierte un FileSigned a su DTO de respuesta. */
    public FileSignedDTOResponse fileSignedToDTO(FileSigned fileSigned) {
        FileSignedDTOResponse response = new FileSignedDTOResponse();
        response.setId(fileSigned.getId());
        response.setFileName(fileSigned.getFileName());
        response.setOriginalFileName(fileSigned.getOriginalFileName());
        if (fileSigned.getLoadTime() != null) {
            response.setLoadTime(Date.from(fileSigned.getLoadTime()
                    .atZone(ZoneId.systemDefault()).toInstant()));
        }
        response.setIpLoad(fileSigned.getIpLoad());
        response.setCompany(fileSigned.getCompany());
        response.setSignDocumentType(fileSigned.getSignDocumentType());
        String reviewStatus = fileSigned.getReviewStatus();
        if (reviewStatus == null) {
            reviewStatus = "PENDIENTE";
        }
        response.setReviewStatus(reviewStatus);
        response.setRejectionReason(fileSigned.getRejectionReason());
        return response;
    }

}
