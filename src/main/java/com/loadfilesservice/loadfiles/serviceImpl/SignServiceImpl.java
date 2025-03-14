package com.loadfilesservice.loadfiles.serviceImpl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loadfilesservice.loadfiles.dao.ISignDao;
import com.loadfilesservice.loadfiles.entity.Sign;
import com.loadfilesservice.loadfiles.service.ISignService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignServiceImpl implements ISignService{
	
    private final ISignDao signDao;

    @Override
    @Transactional(readOnly = true)
    public Optional<Sign> findActiveSignByCompany(Long company) {
        return signDao.findActiveSignByCompany(company);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sign> findActiveSignByUser(Long user) {
        return signDao.findActiveSignByUser(user);
    }

	@Override
	@Transactional
	public Sign save(Sign sign) {
		return signDao.save(sign);
	}

}
