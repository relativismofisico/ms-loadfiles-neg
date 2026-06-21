package com.loadfilesservice.loadfiles.service;

import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.loadfilesservice.loadfiles.entity.Sign;

public interface ISignService {

	public Optional<Sign> findActiveSignByCompany(Long company);

	public Optional<Sign> findActiveSignByUser(Long user);

	public Sign save(Sign sign);

	public Sign replaceSign(MultipartFile file, Long company, Long user, String ipLoad, String companyName);

}
