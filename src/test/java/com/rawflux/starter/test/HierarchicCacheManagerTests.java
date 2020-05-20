package com.rawflux.starter.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.rawflux.starter.cache.HierarchicCacheConfiguration;
import com.rawflux.starter.cache.HierarchicCacheManager;

import net.sf.ehcache.config.CacheConfiguration;
import redis.embedded.RedisServer;

/**
 * @author weizibin
 * @since 2020/5/16 下午10:36 
 */
public class HierarchicCacheManagerTests {

	private static HierarchicCacheManager hierarchicCacheManager;

	private static CacheManager l1CacheManager;
	private static CacheManager l2CacheManager;

	private static RedisServer REDIS_SERVER = new RedisServer();

	@BeforeAll
	public static void init() {
		net.sf.ehcache.config.Configuration cacheConfig = new net.sf.ehcache.config.Configuration()
				.name("HierarchicCacheManagerTests-EhcacheManager")
				.cache(new CacheConfiguration().maxEntriesLocalHeap(1000).name("bothCacheNoTtl"))
				.cache(new CacheConfiguration().maxEntriesLocalHeap(1000).name("onlyL1CacheNoTtl"));
		EhCacheCacheManager ehCacheCacheManager = new EhCacheCacheManager();
		net.sf.ehcache.CacheManager ehcacheManger = net.sf.ehcache.CacheManager.newInstance(cacheConfig);
		ehCacheCacheManager.setCacheManager(ehcacheManger);
		ehCacheCacheManager.afterPropertiesSet();

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>();
		configurationMap.put("bothCacheNoTtl", RedisCacheConfiguration.defaultCacheConfig());
		configurationMap.put("onlyL2CacheNoTtl", RedisCacheConfiguration.defaultCacheConfig());
		RedisCacheManager redisCacheManager = RedisCacheManager.RedisCacheManagerBuilder
				.fromConnectionFactory(connectionFactory).disableCreateOnMissingCache()
				.withInitialCacheConfigurations(configurationMap).build();
		redisCacheManager.afterPropertiesSet();

		HierarchicCacheConfiguration configuration = new HierarchicCacheConfiguration();
		configuration.setAllowNullValue(true);
		HierarchicCacheManager hierarchicCacheManager = new HierarchicCacheManager(Arrays
				.asList(ehCacheCacheManager, redisCacheManager), configuration);

		l1CacheManager = ehCacheCacheManager;
		l2CacheManager = redisCacheManager;
		HierarchicCacheManagerTests.hierarchicCacheManager = hierarchicCacheManager;

		HierarchicCacheManagerTests.REDIS_SERVER.start();
		connectionFactory.afterPropertiesSet();
	}

	@AfterAll
	public static void destroy() {
		HierarchicCacheManagerTests.REDIS_SERVER.stop();
	}

	@BeforeEach
	public void cleanBeforeEach() {
		Collection<String> cacheNames = hierarchicCacheManager.getCacheNames();
		cacheNames.forEach(s -> hierarchicCacheManager.getCache(s).clear());
	}

	@Test
	public void testGetCache() {
		assertNotNull(hierarchicCacheManager.getCache("bothCacheNoTtl"));
		assertNotNull(hierarchicCacheManager.getCache("onlyL1CacheNoTtl"));
		assertNotNull(hierarchicCacheManager.getCache("onlyL2CacheNoTtl"));
		assertNull(hierarchicCacheManager.getCache("bothNoCache"));
	}

	@Test
	public void testGetCacheNames() {
		assertTrue(hierarchicCacheManager.getCacheNames().contains("bothCacheNoTtl"));
		assertTrue(hierarchicCacheManager.getCacheNames().contains("onlyL1CacheNoTtl"));
		assertTrue(hierarchicCacheManager.getCacheNames().contains("onlyL2CacheNoTtl"));
		assertFalse(hierarchicCacheManager.getCacheNames().contains("bothNoCache"));
	}

}
