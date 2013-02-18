package com.xtremelabs.imageutils;

public interface PriorityAccessor {

	public void attach(Prioritizable prioritizable);

	public Prioritizable detachHighestPriorityItem();

	public int size();

	public Prioritizable peek();

	public void clear();

	public void swap(CacheKey cacheKey, PriorityAccessor priorityAccessor, PriorityAccessor priorityAccessor2);
}
