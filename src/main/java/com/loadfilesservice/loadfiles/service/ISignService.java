package com.loadfilesservice.loadfiles.service;

import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.entity.Sign;

public interface ISignService {

	Optional<Sign> findActiveSignByCompany(Long company);

	Optional<Sign> findActiveSignByUser(Long user);

	Sign save(Sign sign);

	Sign replaceSign(MultipartFile file, Long company, Long user, String ipLoad, String companyName);

}