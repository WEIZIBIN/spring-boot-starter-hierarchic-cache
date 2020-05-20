package com.rawflux.starter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

/**
 * @author weizibin
 * @since 2020/5/17 下午10:51 
 */
@Repository
public class TestRepository {

	private static final Logger log = LoggerFactory.getLogger(TestRepository.class);

	@Cacheable(value = "bothCacheNoTtl", sync = true)
	public String get() {
		log.info("get from repository");
		return "my value";
	}

}
