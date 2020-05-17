package com.rawflux.starter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;

import com.rawflux.starter.cache.HierarchicCacheConfiguration;
import com.rawflux.starter.cache.HierarchicCacheManager;

import net.sf.ehcache.config.CacheConfiguration;

/**
 * @author weizibin
 * @since 2020/5/16 下午10:36 
 */
public class HierarchicCacheTests {

	private HierarchicCacheManager hierarchicCacheManager;

	private CacheManager l1CacheManager;
	private CacheManager l2CacheManager;

	public HierarchicCacheTests() throws Exception {
		ConcurrentMapCacheManager concurrentMapCacheManager = new ConcurrentMapCacheManager();

		net.sf.ehcache.config.Configuration cacheConfig = new net.sf.ehcache.config.Configuration();
		cacheConfig.addCache(new CacheConfiguration().maxEntriesLocalHeap(1000).name("c1").timeToLiveSeconds(30).timeToIdleSeconds(0));
		EhCacheCacheManager ehCacheCacheManager = new EhCacheCacheManager();
		net.sf.ehcache.CacheManager ehcacheManger = net.sf.ehcache.CacheManager.create(cacheConfig);
		ehCacheCacheManager.setCacheManager(ehcacheManger);

		HierarchicCacheConfiguration configuration = new HierarchicCacheConfiguration();
		configuration.setAllowNullValue(true);
		HierarchicCacheManager hierarchicCacheManager = new HierarchicCacheManager(Arrays
				.asList(concurrentMapCacheManager, ehCacheCacheManager), configuration);


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

	// TODO: 2020/5/17 test L1NonValueAndL2HasValue up store mechanism

	// TODO: 2020/5/17 test L1NonValueAndL2NotStoreCache

	// TODO: 2020/5/17 test L1L2NotStoreCache

	// TODO: 2020/5/17 test L2HasValueAndL1NotStoreCache

	// TODO: 2020/5/17 test clear

	// TODO: 2020/5/17 test evict

}
