package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class AdapterPrecacheAccessorTests extends AndroidTestCase {
	private AdapterPrecacheAccessor mAccessor;
	private DefaultPrioritizable[] mPrioritizables;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mAccessor = new AdapterPrecacheAccessor();
		mPrioritizables = new DefaultPrioritizable[5];
	}

	public void testAttach() {
		assertEquals(0, mAccessor.size());

		for (int i = 0; i < mPrioritizables.length; i++) {
			mPrioritizables[i] = generatePrioritizable(i);
			mAccessor.attach(mPrioritizables[i]);
		}

		assertEquals(2, mAccessor.size());
	}

	public void testDetachHighestPriority() {
		assertEquals(0, mAccessor.size());

		for (int i = 0; i < mPrioritizables.length; i++) {
			mPrioritizables[i] = generatePrioritizable(i);
			mAccessor.attach(mPrioritizables[i]);
		}

		DefaultPrioritizable prioritizable = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertNotNull(prioritizable);
		assertEquals(3, prioritizable.getCacheRequest().getCacheKey().position);
		assertEquals(1, mAccessor.size());

		prioritizable = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertNotNull(prioritizable);
		assertEquals(4, prioritizable.getCacheRequest().getCacheKey().position);
		assertEquals(0, mAccessor.size());

		prioritizable = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertNull(prioritizable);
		assertEquals(0, mAccessor.size());
	}

	public void testingAddingAndRemoving() {
		mPrioritizables[0] = generatePrioritizable(0);
		mPrioritizables[1] = generatePrioritizable(0);
		mPrioritizables[2] = generatePrioritizable(1);
		mPrioritizables[3] = generatePrioritizable(1);
		mPrioritizables[4] = generatePrioritizable(2);

		assertNull(mAccessor.detachHighestPriorityItem());
		mAccessor.attach(mPrioritizables[0]);
		mAccessor.attach(mPrioritizables[1]);
		mAccessor.attach(mPrioritizables[2]);
		mAccessor.detachHighestPriorityItem();
		mAccessor.attach(mPrioritizables[3]);
		mAccessor.detachHighestPriorityItem();
		mAccessor.attach(mPrioritizables[4]);
	}

	private DefaultPrioritizable generatePrioritizable(int position) {
		return new DefaultPrioritizable(generateCacheRequest(position), new Request<String>("baller")) {
			@Override
			public void execute() {
			}
		};
	}

	private CacheRequest generateCacheRequest(int position) {
		CacheRequest cacheRequest = new CacheRequest("blah");
		cacheRequest.setCacheKey(new CacheKey(1, position, 2));
		return cacheRequest;
	}
}
