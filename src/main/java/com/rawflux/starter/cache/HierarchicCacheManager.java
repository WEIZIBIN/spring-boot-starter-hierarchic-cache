package com.rawflux.starter.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;

/**
 * @author weizibin
 * @since 2020/5/16 下午3:42 
 */
public class HierarchicCacheManager implements CacheManager {

	private List<CacheManager> cacheManagers;
	private HierarchicCacheConfiguration configuration;

	public HierarchicCacheManager(List<CacheManager> sortedCacheManagers, HierarchicCacheConfiguration configuration) {
		this.cacheManagers = sortedCacheManagers;
		this.configuration = configuration;
	}

	@Override
	@NonNull
	public Cache getCache(String s) {
		List<Cache> caches = new ArrayList<>();
		for (CacheManager cacheManager : cacheManagers) {
			Optional.ofNullable(cacheManager.getCache(s)).ifPresent(caches::add);
		}
		return new HierarchicCache(s, caches, configuration.getAllowNullValue());
	}

	@Override
	public Collection<String> getCacheNames() {
		Set<String> cacheNames = new HashSet<>();
		for (CacheManager cacheManager : cacheManagers) {
			cacheNames.addAll(cacheManager.getCacheNames());
		}
		return cacheNames;
	}
}
