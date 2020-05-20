package com.rawflux.starter.controller;

import java.util.Arrays;

import com.rawflux.starter.cache.HierarchicCacheConfiguration;
import com.rawflux.starter.cache.HierarchicCacheManager;
import net.sf.ehcache.config.CacheConfiguration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;

/**
 * @author weizibin
 * @since 2020/5/17 下午10:51 
 */
@Repository
public class TestConfig {

	@Bean
	public CacheManager cacheManager() {
		ConcurrentMapCacheManager concurrentMapCacheManager = new ConcurrentMapCacheManager();

		net.sf.ehcache.config.Configuration cacheConfig = new net.sf.ehcache.config.Configuration();
		cacheConfig.addCache(new CacheConfiguration().maxEntriesLocalHeap(1000).name("bothCacheNoTtl").timeToLiveSeconds(30).timeToIdleSeconds(0));
		EhCacheCacheManager ehCacheCacheManager = new EhCacheCacheManager();
		net.sf.ehcache.CacheManager ehcacheManger = net.sf.ehcache.CacheManager.create(cacheConfig);
		ehCacheCacheManager.setCacheManager(ehcacheManger);

		HierarchicCacheConfiguration configuration = new HierarchicCacheConfiguration();
		configuration.setAllowNullValue(true);

		return new HierarchicCacheManager(Arrays
				.asList(concurrentMapCacheManager, ehCacheCacheManager), configuration);
	}



}
