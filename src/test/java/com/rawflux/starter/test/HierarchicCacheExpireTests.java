package com.rawflux.starter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
public class HierarchicCacheExpireTests {

	private static HierarchicCacheManager hierarchicCacheManager;

	private static CacheManager l1CacheManager;
	private static CacheManager l2CacheManager;

	private static RedisServer REDIS_SERVER = new RedisServer();

	@BeforeAll
	public static void init() {
		net.sf.ehcache.config.Configuration cacheConfig = new net.sf.ehcache.config.Configuration()
				.name("HierarchicCacheExpireTests-EhcacheManager")
				.cache(new CacheConfiguration().maxEntriesLocalHeap(1000).timeToLiveSeconds(2).name("bothCacheTtl2s"))
				.cache(new CacheConfiguration().maxEntriesLocalHeap(1000).timeToLiveSeconds(2)
						.name("L1CacheTtl2sL2CacheNoTtl"))
				.cache(new CacheConfiguration().maxEntriesLocalHeap(1000).name("L2CacheTtl2sL1CacheNoTtl"));
		EhCacheCacheManager ehCacheCacheManager = new EhCacheCacheManager();
		net.sf.ehcache.CacheManager ehcacheManger = net.sf.ehcache.CacheManager.newInstance(cacheConfig);
		ehCacheCacheManager.setCacheManager(ehcacheManger);
		ehCacheCacheManager.afterPropertiesSet();

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory();
		Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>();
		configurationMap
				.put("bothCacheTtl2s", RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(2L)));
		configurationMap
				.put("L1CacheTtl2sL2CacheNoTtl", RedisCacheConfiguration.defaultCacheConfig());
		configurationMap
				.put("L2CacheTtl2sL1CacheNoTtl", RedisCacheConfiguration.defaultCacheConfig()
						.entryTtl(Duration.ofSeconds(2L)));
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
		HierarchicCacheExpireTests.hierarchicCacheManager = hierarchicCacheManager;

		HierarchicCacheExpireTests.REDIS_SERVER.start();
		connectionFactory.afterPropertiesSet();
	}

	@AfterAll
	public static void destroy() {
		HierarchicCacheExpireTests.REDIS_SERVER.stop();
	}

	@BeforeEach
	public void cleanBeforeEach() {
		Collection<String> cacheNames = hierarchicCacheManager.getCacheNames();
		cacheNames.forEach(s -> hierarchicCacheManager.getCache(s).clear());
	}

	@Test
	public void testL1ExpireAndL2HasValue() throws InterruptedException {
		Cache hc1 = hierarchicCacheManager.getCache("L1CacheTtl2sL2CacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("L1CacheTtl2sL2CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("L1CacheTtl2sL2CacheNoTtl");

		l1cache.put("key1", "value1");
		l2cache.put("key1", "value2");
		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value2", l2cache.get("key1").get());

		TimeUnit.SECONDS.sleep(2);

		assertNull(l1cache.get("key1"));
		assertEquals("value2", hc1.get("key1").get());
		assertEquals("value2", l2cache.get("key1").get());
	}

	@Test
	public void testL2ExpireAndL1HasValue() throws InterruptedException {
		Cache hc1 = hierarchicCacheManager.getCache("L2CacheTtl2sL1CacheNoTtl");
		Cache l1cache = l1CacheManager.getCache("L2CacheTtl2sL1CacheNoTtl");
		Cache l2cache = l2CacheManager.getCache("L2CacheTtl2sL1CacheNoTtl");

		l1cache.put("key1", "value1");
		l2cache.put("key1", "value2");
		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value2", l2cache.get("key1").get());

		TimeUnit.SECONDS.sleep(2);

		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value1", l1cache.get("key1").get());
		assertNull(l2cache.get("key1"));
	}

	@Test
	public void testL1ExpireAndL2Expire() throws InterruptedException {
		Cache hc1 = hierarchicCacheManager.getCache("bothCacheTtl2s");
		Cache l1cache = l1CacheManager.getCache("bothCacheTtl2s");
		Cache l2cache = l2CacheManager.getCache("bothCacheTtl2s");

		l1cache.put("key1", "value1");
		l2cache.put("key1", "value2");
		assertEquals("value1", l1cache.get("key1").get());
		assertEquals("value1", hc1.get("key1").get());
		assertEquals("value2", l2cache.get("key1").get());

		TimeUnit.SECONDS.sleep(2);

		assertNull(l2cache.get("key1"));
		assertNull(l1cache.get("key1"));
		assertNull(hc1.get("key1"));
	}

}
