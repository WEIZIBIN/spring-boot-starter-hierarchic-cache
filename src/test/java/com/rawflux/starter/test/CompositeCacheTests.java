package com.rawflux.starter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.support.CompositeCacheManager;

import com.rawflux.starter.cache.HierarchicCacheConfiguration;
import com.rawflux.starter.cache.HierarchicCacheManager;

/**
 * @author weizibin
 * @since 2020/5/16 下午10:36 
 */
public class CompositeCacheTests {

	private CompositeCacheManager hierarchicCacheManager;

	private CacheManager l1CacheManager;
	private CacheManager l2CacheManager;

	public CompositeCacheTests() {
		ConcurrentMapCacheManager concurrentMapCacheManager = new ConcurrentMapCacheManager();

		ConcurrentMapCacheManager ehCacheCacheManager = new ConcurrentMapCacheManager();

		HierarchicCacheConfiguration configuration = new HierarchicCacheConfiguration();
		configuration.setAllowNullValue(true);
		CompositeCacheManager hierarchicCacheManager = new CompositeCacheManager(concurrentMapCacheManager,
				ehCacheCacheManager);


		this.l1CacheManager = concurrentMapCacheManager;
		this.l2CacheManager = ehCacheCacheManager;
		this.hierarchicCacheManager = hierarchicCacheManager;
	}

	@BeforeEach
	public void cleanBeforeEach() {
		Collection<String> cacheNames = hierarchicCacheManager.getCacheNames();
		cacheNames.forEach(s -> hierarchicCacheManager.getCache(s).clear());
	}

	@Test
	public void testL1HasValueAndL2Doesnt() {
		Cache hc1 = hierarchicCacheManager.getCache("c1");
		Cache l1cache = l1CacheManager.getCache("c1");
		Cache l2cache = l2CacheManager.getCache("c1");
		assertTrue(l1cache instanceof ConcurrentMapCache);
		l1cache.put("key1", "value1");
		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", l1cache.putIfAbsent("key1", "value1x").get());
		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value1", hc1.putIfAbsent("key1", "value1x").get());
		assertEquals("value1", hc1.get("key1").get());
		assertNull(l2cache.get("key1"));
	}

	@Test
	public void testL1NonValueAndL2Has() {
		Cache hc1 = hierarchicCacheManager.getCache("c1");
		Cache l1cache = l1CacheManager.getCache("c1");
		Cache l2cache = l2CacheManager.getCache("c1");
		assertTrue(l1cache instanceof ConcurrentMapCache);
		l2cache.put("key1", "value1");
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
		assertNotNull(l2cache.get("key1"));
	}

}
