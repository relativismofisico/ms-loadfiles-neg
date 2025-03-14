package com.loadfilesservice.loadfiles.service;

import java.util.Optional;

import com.loadfilesservice.loadfiles.entity.Sign;

public interface ISignService {

	public Optional<Sign> findActiveSignByCompany(Long company);
	
	public Optional<Sign> findActiveSignByUser(Long user);
	
	public Sign save(Sign sign);
}
