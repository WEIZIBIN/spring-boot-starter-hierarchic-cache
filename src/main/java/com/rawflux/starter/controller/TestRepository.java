package com.rawflux.starter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author weizibin
 * @since 2020/5/17 下午10:51 
 */
@Repository
public class TestRepository {

	private static final Logger log = LoggerFactory.getLogger(TestRepository.class);

	@Cacheable("c1")
	public String get() {
		log.info("get from repository");
		return "my value";
	}

}
