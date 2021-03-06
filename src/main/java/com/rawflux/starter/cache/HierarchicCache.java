package com.rawflux.starter.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.util.Assert;

/**
 * @author weizibin
 * @since 2020/5/16 下午4:08 
 */
public class HierarchicCache extends AbstractValueAdaptingCache {

	private List<Cache> caches;
	private String name;

	public HierarchicCache(String name, List<Cache> caches, boolean allowNullValues) {
		super(allowNullValues);

		Assert.notNull(name, "Name must not be null!");
		Assert.notNull(caches, "Caches must not be null!");

		this.caches = caches;
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public HierarchicCache getNativeCache() {
		return this;
	}

	// 已知问题： L1 L2 L3   L3比L1先过期，L1就只能等过期后才可更新缓存
	// 已知问题： L1 L2 L3
	// lookup(Object key) & get(Object key, Callable<T> valueLoader)
	// 过于依赖其他层级缓存的put方法，增加了报错概率

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T> T get(Object key, Callable<T> valueLoader) {
		ValueWrapper result = get(key);

		if (result != null) {
			return (T) result.get();
		}

		T value = valueFromLoader(key, valueLoader);
		put(key, value);
		return value;
	}

	@Override
	protected Object lookup(Object key) {
		List<Cache> permeateCaches = new ArrayList<>();
		Object obtain = null;
		for (Cache cache : caches) {
			ValueWrapper valueWrapper = cache.get(key);
			if (valueWrapper != null) {
				obtain = valueWrapper.get();
				break;
			} else {
				permeateCaches.add(cache);
			}
		}

		if (obtain == null) {
			return null;
		}

		for (Cache permeateCache : permeateCaches) {
			permeateCache.put(key, obtain);
		}
		return obtain;
	}

	@Override
	public void put(Object key, Object value) {
		for (Cache cache : caches) {
			cache.put(key, value);
		}
	}

	@Override
	public void evict(Object key) {
		for (Cache cache : caches) {
			cache.evict(key);
		}
	}

	@Override
	public void clear() {
		for (Cache cache : caches) {
			cache.clear();
		}
	}

	private static <T> T valueFromLoader(Object key, Callable<T> valueLoader) {
		try {
			return valueLoader.call();
		} catch (Exception e) {
			throw new ValueRetrievalException(key, valueLoader, e);
		}
	}
}
