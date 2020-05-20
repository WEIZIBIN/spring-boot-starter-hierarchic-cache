package com.rawflux.starter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
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
public class HierarchicCacheCommonTests {

	private static HierarchicCacheManager hierarchicCacheManager;

	private static CacheManager l1CacheManager;
	private static CacheManager l2CacheManager;

	private static RedisServer REDIS_SERVER = new RedisServer();

	@BeforeAll
	public static void init() {
		net.sf.ehcache.config.Configuration cacheConfig = new net.sf.ehcache.config.Configuration()
				.name("HierarchicCacheCommonTests-EhcacheManager")
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
		HierarchicCacheCommonTests.hierarchicCacheManager = hierarchicCacheManager;

		HierarchicCacheCommonTests.REDIS_SERVER.start();
		connectionFactory.afterPropertiesSet();
	}

	@AfterAll
	public static void destroy() {
		HierarchicCacheCommonTests.REDIS_SERVER.stop();

	}

	@BeforeEach
	public void cleanBeforeEach() {
		Collection<String> cacheNames = hierarchicCacheManager.getCacheNames();
		cacheNames.forEach(s -> hierarchicCacheManager.getCache(s).clear());
	}

	@Test
	public void testL1HasValueAndL2Doesnt() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");
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
	public void testL1NonValueAndL2HasValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l2cache.put("key1", "value1");
		assertEquals("value1", l2cache.get("key1").get());
		assertNull(l1cache.get("key1"));
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value1", l1cache.get("key1").get());
	}

	@Test
	public void testL1L2BothHasDifferenceValueAndReturnL1() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "from l1cache");
		l2cache.put("key1", "from l2cache");

		assertEquals("from l1cache", l1cache.get("key1").get());
		assertEquals("from l2cache", l2cache.get("key1").get());
		assertEquals("from l1cache", hc1.get("key1").get());
	}

	@Test
	public void testL1NonValueAndL2NotStoreCache() {
		Cache hc1 = hierarchicCacheManager.getCache("onlyL1CacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("onlyL1CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("onlyL1CacheNoTtl");

		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
		assertNull(l2cache);
	}

	@Test
	public void testL2NonValueAndL1NotStoreCache() {
		Cache hc1 = hierarchicCacheManager.getCache("onlyL2CacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("onlyL2CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("onlyL2CacheNoTtl");

		assertNull(l2cache.get("key1"));
		assertNull(hc1.get("key1"));
		assertNull(l1cache);
	}

	@Test
	public void testL1L2NotStoreCache() {
		Cache hc1 = hierarchicCacheManager.getCache("noStoreCache");
		Cache l1cache = l1CacheManager.getCache("noStoreCache");
		Cache l2cache = l2CacheManager.getCache("noStoreCache");

		assertNull(hc1);
		assertNull(l2cache);
		assertNull(l1cache);
	}

	@Test
	public void testL2HasValueAndL1NotStoreCache() {
		Cache hc1 = hierarchicCacheManager.getCache("onlyL2CacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("onlyL2CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("onlyL2CacheNoTtl");

		l2cache.put("key1", "value1");

		assertNull(l1cache);
		assertEquals("value1", l2cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertNull(l1CacheManager.getCache("onlyL2CacheNoTtl"));
	}

	@Test
	public void testL1HasValueAndClear() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "value1");
		l1cache.put("key2", "value2");

		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value2", l1cache.get("key2").get());
		assertEquals("value2", hc1.get("key2").get());
		assertNull(l2cache.get("key1"));
		assertNull(l2cache.get("key2"));

		l1cache.clear();
		assertNull(l1cache.get("key1"));
		assertNull(l1cache.get("key2"));
		assertNull(hc1.get("key1"));
		assertNull(hc1.get("key2"));
	}

	@Test
	public void testL2HasValueAndClear() {
		Cache hc1 = hierarchicCacheManager.getCache("onlyL2CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("onlyL2CacheNoTtl");

		l2cache.put("key1", "value1");
		l2cache.put("key2", "value2");

		assertEquals("value1", l2cache.get("key1").get());
		assertEquals("value2", l2cache.get("key2").get());
		assertEquals("value2", hc1.get("key2").get());
		assertEquals("value1", hc1.get("key1").get());
		assertNull(l1CacheManager.getCache("onlyL2CacheNoTtl"));

		l2cache.clear();
		assertNull(l2cache.get("key1"));
		assertNull(l2cache.get("key2"));
		assertNull(hc1.get("key1"));
		assertNull(hc1.get("key2"));
	}

	@Test
	public void testL1L2HasValueAndAllClear() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "value1");
		l1cache.put("key2", "value2");
		l2cache.put("key1", "value1");
		l2cache.put("key2", "value2");

		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", l2cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value2", l1cache.get("key2").get());
		assertEquals("value2", l2cache.get("key2").get());
		assertEquals("value2", hc1.get("key2").get());

		hc1.clear();
		assertNull(l1cache.get("key1"));
		assertNull(l1cache.get("key2"));
		assertNull(l2cache.get("key1"));
		assertNull(l2cache.get("key2"));
	}

	@Test
	public void testL1EvictAndL2NonValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "value1");

		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertNull(l2cache.get("key1"));

		l1cache.evict("key1");
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
		assertNull(l2cache.get("key1"));
	}

	@Test
	public void testL1EvictAndL2HasValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "from l1cache");
		l2cache.put("key1", "from l2cache");

		assertEquals("from l1cache", hc1.get("key1").get());
		assertEquals("from l1cache", l1cache.get("key1").get());
		assertEquals("from l2cache", l2cache.get("key1").get());

		l1cache.evict("key1");
		assertNull(l1cache.get("key1"));
		assertEquals("from l2cache", l2cache.get("key1").get());
		assertEquals("from l2cache", hc1.get("key1").get());
	}

	@Test
	public void testL2EvictAndL1NonValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "value1");

		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertNull(l2cache.get("key1"));

		l1cache.evict("key1");
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
		assertNull(l2cache.get("key1"));
	}

	@Test
	public void testL2EvictAndL1HasValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "from l1cache");
		l2cache.put("key1", "from l2cache");

		assertEquals("from l1cache", hc1.get("key1").get());
		assertEquals("from l1cache", l1cache.get("key1").get());
		assertEquals("from l2cache", l2cache.get("key1").get());

		l2cache.evict("key1");
		assertNull(l2cache.get("key1"));
		assertEquals("from l1cache", l1cache.get("key1").get());
		assertEquals("from l1cache", hc1.get("key1").get());
	}

	@Test
	public void testAllEvictWhenL1HasValueAndL2HasValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "from l1cache");
		l1cache.put("key2", "key2 from l1cache");
		l2cache.put("key1", "from l2cache");
		l2cache.put("key2", "key2 from l2cache");


		assertEquals("from l1cache", hc1.get("key1").get());
		assertEquals("from l1cache", l1cache.get("key1").get());
		assertEquals("from l2cache", l2cache.get("key1").get());
		assertEquals("key2 from l1cache", l1cache.get("key2").get());
		assertEquals("key2 from l2cache", l2cache.get("key2").get());

		hc1.evict("key1");

		assertEquals("key2 from l1cache", l1cache.get("key2").get());
		assertEquals("key2 from l2cache", l2cache.get("key2").get());

		assertNull(l2cache.get("key1"));
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
	}

	@Test
	public void testAllEvictWhenL1NonValueAndL2NonValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key2", "key2 from l1cache");
		l2cache.put("key2", "key2 from l2cache");


		assertNull(l2cache.get("key1"));
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));

		assertEquals("key2 from l1cache", l1cache.get("key2").get());
		assertEquals("key2 from l2cache", l2cache.get("key2").get());

		hc1.evict("key1");

		assertEquals("key2 from l1cache", l1cache.get("key2").get());
		assertEquals("key2 from l2cache", l2cache.get("key2").get());
		assertNull(l2cache.get("key1"));
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
	}

	@Test
	public void testAllEvictWhenL1HasValueAndL2NonValue() {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("bothCacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("bothCacheNoTtl");

		l1cache.put("key1", "from l1cache");
		l1cache.put("key2", "key2 from l1cache");
		l2cache.put("key2", "key2 from l2cache");


		assertEquals("from l1cache", hc1.get("key1").get());
		assertEquals("from l1cache", l1cache.get("key1").get());
		assertNull(l2cache.get("key1"));
		assertEquals("key2 from l1cache", l1cache.get("key2").get());
		assertEquals("key2 from l2cache", l2cache.get("key2").get());

		hc1.evict("key1");

		assertEquals("key2 from l1cache", l1cache.get("key2").get());
		assertEquals("key2 from l2cache", l2cache.get("key2").get());

		assertNull(l2cache.get("key1"));
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
	}

	@Test
	public void testAllEvictWhenL2HasValueAndL1NotStoreValue() {
		Cache hc1 = hierarchicCacheManager.getCache("onlyL2CacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("onlyL2CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("onlyL2CacheNoTtl");

		l2cache.put("key1", "value1");
		l2cache.put("key2", "value2");

		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value1", l2cache.get("key1").get());
		assertEquals("value2", hc1.get("key2").get());
		assertEquals("value2", l2cache.get("key2").get());
		assertNull(l1cache);

		hc1.evict("key1");

		assertNull(hc1.get("key1"));
		assertNull(l2cache.get("key1"));
		assertEquals("value2", hc1.get("key2").get());
		assertEquals("value2", l2cache.get("key2").get());
		assertNull(l1CacheManager.getCache("onlyL2CacheNoTtl"));
	}

}
