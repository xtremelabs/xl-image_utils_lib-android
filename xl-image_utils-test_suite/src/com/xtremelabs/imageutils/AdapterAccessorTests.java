package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

import com.xtremelabs.imageutils.AdapterAccessor.AdapterAccessorType;

public class AdapterAccessorTests extends AndroidTestCase {
	private AdapterAccessor mAccessor;
	private DefaultPrioritizable[] mRequests;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mAccessor = new AdapterAccessor(AdapterAccessorType.DEPRIORITIZED);
		mRequests = new DefaultPrioritizable[5];

		mRequests[0] = generatePrioritizable(0);
		mRequests[1] = generatePrioritizable(1);
		mRequests[2] = generatePrioritizable(2);
		mRequests[3] = generatePrioritizable(3);
		mRequests[4] = generatePrioritizable(4);
	}

	public void testInitialCondition() {
		assertNull(mAccessor.detachHighestPriorityItem());
		assertNull(mAccessor.peek());
		assertEquals(0, mAccessor.size());
		assertFalse(mAccessor.contains(generatePrioritizable(0)));
	}

	public void testAttach() {
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
		assertEquals(ImageRequestType.PRECACHE_TO_MEMORY_FOR_ADAPTER, p.getCacheRequest().getRequestType());

		p = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertEquals(mRequests[2], p);
		assertEquals(ImageRequestType.PRECACHE_TO_DISK_FOR_ADAPTER, p.getCacheRequest().getRequestType());

		p = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertEquals(mRequests[1], p);
		assertEquals(ImageRequestType.PRECACHE_TO_DISK_FOR_ADAPTER, p.getCacheRequest().getRequestType());

		p = (DefaultPrioritizable) mAccessor.detachHighestPriorityItem();
		assertNull(p);
	}

	public void testRepeatedAttach() {
		mAccessor.attach(mRequests[0]);
		assertEquals(1, mAccessor.size());

		assertEquals(mRequests[0], mAccessor.detachHighestPriorityItem());

		mAccessor.attach(mRequests[1]);
		assertEquals(1, mAccessor.size());

		assertEquals(mRequests[1], mAccessor.detachHighestPriorityItem());

		mAccessor.attach(mRequests[2]);
		assertEquals(1, mAccessor.size());

		assertEquals(mRequests[2], mAccessor.detachHighestPriorityItem());

		mAccessor.attach(mRequests[3]);
		assertEquals(1, mAccessor.size());

		mAccessor.attach(mRequests[4]);
		assertEquals(2, mAccessor.size());

		assertEquals(mRequests[4], mAccessor.detachHighestPriorityItem());
		assertEquals(mRequests[3], mAccessor.detachHighestPriorityItem());
	}

	public void testAttachWithMultipleEvictions() {
		mRequests[0] = generatePrioritizable(0);
		mRequests[1] = generatePrioritizable(0);

		mAccessor.attach(mRequests[0]);
		mAccessor.attach(mRequests[1]);
		mAccessor.attach(mRequests[2]);
		mAccessor.attach(mRequests[3]);
		mAccessor.attach(mRequests[4]);
		mAccessor.attach(generatePrioritizable(7));
		mAccessor.attach(generatePrioritizable(8));
		mAccessor.attach(generatePrioritizable(9));

		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();
		mAccessor.detachHighestPriorityItem();

		assertTrue(mAccessor.size() == 0);
	}

	public void testSwap() {
		AdapterAccessor memAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_MEMORY);
		AdapterAccessor diskAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_DISK);
		AdapterAccessor deprioritizedAccessor = new AdapterAccessor(AdapterAccessorType.DEPRIORITIZED);

		memAccessor.attach(mRequests[0]);
		memAccessor.attach(mRequests[1]);
		diskAccessor.attach(mRequests[2]);
		deprioritizedAccessor.attach(mRequests[3]);
		deprioritizedAccessor.attach(mRequests[4]);

		assertEquals(2, memAccessor.size());
		assertEquals(1, diskAccessor.size());
		assertEquals(2, deprioritizedAccessor.size());

		deprioritizedAccessor.swap(new CacheKey(1, 0, 2, 2), memAccessor, diskAccessor);

		assertEquals(2, memAccessor.size());
		assertEquals(0, diskAccessor.size());
		assertEquals(3, deprioritizedAccessor.size());

		assertTrue(deprioritizedAccessor.contains(mRequests[0]));
		assertTrue(deprioritizedAccessor.contains(mRequests[1]));
		assertTrue(deprioritizedAccessor.contains(mRequests[2]));
		assertTrue(memAccessor.contains(mRequests[3]));
		assertTrue(memAccessor.contains(mRequests[4]));
	}

	public void testSwapWhileAllEmpty() {
		AdapterAccessor memAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_MEMORY);
		AdapterAccessor diskAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_DISK);
		AdapterAccessor deprioritizedAccessor = new AdapterAccessor(AdapterAccessorType.DEPRIORITIZED);

		assertEquals(0, memAccessor.size());
		assertEquals(0, diskAccessor.size());
		assertEquals(0, deprioritizedAccessor.size());

		deprioritizedAccessor.swap(new CacheKey(1, 0, 2, 2), memAccessor, diskAccessor);

		assertEquals(0, memAccessor.size());
		assertEquals(0, diskAccessor.size());
		assertEquals(0, deprioritizedAccessor.size());
	}

	public void testSwapWhileMemEmpty() {
		AdapterAccessor memAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_MEMORY);
		AdapterAccessor diskAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_DISK);
		AdapterAccessor deprioritizedAccessor = new AdapterAccessor(AdapterAccessorType.DEPRIORITIZED);

		diskAccessor.attach(mRequests[0]);
		diskAccessor.attach(mRequests[1]);
		diskAccessor.attach(mRequests[2]);
		deprioritizedAccessor.attach(mRequests[3]);
		deprioritizedAccessor.attach(mRequests[4]);

		assertEquals(0, memAccessor.size());
		assertEquals(2, diskAccessor.size());
		assertEquals(2, deprioritizedAccessor.size());

		deprioritizedAccessor.swap(new CacheKey(1, 0, 2, 2), memAccessor, diskAccessor);

		assertEquals(2, memAccessor.size());
		assertEquals(0, diskAccessor.size());
		assertEquals(2, deprioritizedAccessor.size());

		assertTrue(deprioritizedAccessor.contains(mRequests[1]));
		assertTrue(deprioritizedAccessor.contains(mRequests[2]));
		assertTrue(memAccessor.contains(mRequests[3]));
		assertTrue(memAccessor.contains(mRequests[4]));
	}

	public void testSwapWithOnlyDiskSwap() {
		AdapterAccessor memAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_MEMORY);
		AdapterAccessor diskAccessor = new AdapterAccessor(AdapterAccessorType.PRECACHE_DISK);
		AdapterAccessor deprioritizedAccessor = new AdapterAccessor(AdapterAccessorType.DEPRIORITIZED);

		deprioritizedAccessor.attach(mRequests[0]);
		deprioritizedAccessor.attach(mRequests[1]);
		deprioritizedAccessor.attach(mRequests[2]);
		deprioritizedAccessor.attach(mRequests[3]);
		deprioritizedAccessor.attach(mRequests[4]);

		assertEquals(mRequests[4], deprioritizedAccessor.detachHighestPriorityItem());
		assertEquals(mRequests[3], deprioritizedAccessor.detachHighestPriorityItem());

		deprioritizedAccessor.swap(new CacheKey(1, 0, 2, 2), memAccessor, diskAccessor);

		assertEquals(0, memAccessor.size());
		assertEquals(2, diskAccessor.size());
		assertEquals(0, deprioritizedAccessor.size());

		assertEquals(mRequests[2], diskAccessor.detachHighestPriorityItem());
		assertEquals(mRequests[1], diskAccessor.detachHighestPriorityItem());
		assertNull(diskAccessor.detachHighestPriorityItem());
		assertNull(deprioritizedAccessor.detachHighestPriorityItem());
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
		cacheRequest.setCacheKey(new CacheKey(1, position, 2, 2));
		return cacheRequest;
	}
}
