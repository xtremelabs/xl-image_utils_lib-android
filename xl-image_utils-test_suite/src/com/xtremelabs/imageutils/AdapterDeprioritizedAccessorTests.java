package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class AdapterDeprioritizedAccessorTests extends AndroidTestCase {
	private AdapterDeprioritizedAccessor mAccessor;
	private DefaultPrioritizable[] mRequests;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mAccessor = new AdapterDeprioritizedAccessor();
		mRequests = new DefaultPrioritizable[5];
	}

	public void testInitialCondition() {
		assertNull(mAccessor.detachHighestPriorityItem());
		assertNull(mAccessor.peek());
		assertEquals(0, mAccessor.size());
		assertFalse(mAccessor.contains(generatePrioritizable(0)));
	}

	public void testAttach() {
		mRequests[0] = generatePrioritizable(0);
		mRequests[1] = generatePrioritizable(1);
		mRequests[2] = generatePrioritizable(2);
		mRequests[3] = generatePrioritizable(3);
		mRequests[4] = generatePrioritizable(4);

		assertFalse(mAccessor.contains(mRequests[0]));
		assertFalse(mAccessor.contains(mRequests[1]));
		assertFalse(mAccessor.contains(mRequests[2]));

		mAccessor.attach(mRequests[0]);
		mAccessor.attach(mRequests[1]);
		mAccessor.attach(mRequests[2]);
		mAccessor.attach(mRequests[3]);
		mAccessor.attach(mRequests[4]);

		DefaultPrioritizable p;

		p = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertEquals(mRequests[4], p);
		assertEquals(ImageRequestType.PRECACHE_TO_MEMORY_FOR_ADAPTER, p.getCacheRequest().getRequestType());

		p = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertEquals(mRequests[3], p);
		assertEquals(ImageRequestType.PRECACHE_TO_DISK_FOR_ADAPTER, p.getCacheRequest().getRequestType());
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
		cacheRequest.setCacheKey(new CacheKey(1, position, 2, 1, 1));
		return cacheRequest;
	}
}
