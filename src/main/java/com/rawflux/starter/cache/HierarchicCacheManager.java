package com.rawflux.starter.cache;

import java.util.Collection;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author weizibin
 * @since 2020/5/16 下午3:42 
 */
public class HierarchicCacheManager implements CacheManager {

	private List<CacheManager> cacheManagers;

	public HierarchicCacheManager(List<CacheManager> sortedCacheManagers) {
		this.cacheManagers = sortedCacheManagers;
	}

	@Override
	public Cache getCache(String s) {
		// TODO: 2020/5/16 获取组合cache
		return null;
	}

	@Override
	public Collection<String> getCacheNames() {
		// TODO: 2020/5/16 获取组合后的cachenames
		return null;
	}
}
