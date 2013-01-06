package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheInformationProvider;
import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheRequest;
import com.xtremelabs.testactivity.MainActivity;

public class ImagePrecacheAssistantTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private ImagePrecacheAssistant mAssistant;
	private ImageLoader mImageLoader;
	private int mCount;
	private final List<PrecacheRequest> mList = new ArrayList<ImagePrecacheAssistant.PrecacheRequest>();

	private final List<String> mDiskPrecacheRequests = new ArrayList<String>();
	private final List<String> mMemoryPrecacheRequests = new ArrayList<String>();

	public ImagePrecacheAssistantTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ThreadChecker.disableUiThreadCheck();

		mImageLoader = new ImageLoader(getActivity()) {
			@Override
			public void precacheImageToDisk(String uri) {
				mDiskPrecacheRequests.add(uri);
			}

			@Override
			public void precacheImageToDiskAndMemory(String uri, Integer width, Integer height) {
				mMemoryPrecacheRequests.add(uri);
			}
		};

		mAssistant = new ImagePrecacheAssistant(mImageLoader, new PrecacheInformationProvider() {
			@Override
			public List<PrecacheRequest> onRowPrecacheRequestsRequired(int position) {
				return mList;
			}

			@Override
			public int getCount() {
				return mCount;
			}
		});

		clearLists();
	}

	public void testListWithOneElement() {
		mCount = 1;
		mAssistant.onPositionVisited(0);
		assertEquals(0, mDiskPrecacheRequests.size());
		assertEquals(0, mMemoryPrecacheRequests.size());
	}

	public void testListWithTwoElements() {
		mCount = 2;
		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
		mAssistant.onPositionVisited(0);
		assertEquals(1, mMemoryPrecacheRequests.size());
		assertEquals(0, mDiskPrecacheRequests.size());
		assertEquals("1", mMemoryPrecacheRequests.get(0));

		clearLists();

		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));

		mAssistant.onPositionVisited(1);
		assertEquals(0, mDiskPrecacheRequests.size());
		assertEquals(0, mMemoryPrecacheRequests.size());

		mAssistant.onPositionVisited(0);
		assertEquals(0, mDiskPrecacheRequests.size());
		assertEquals(0, mMemoryPrecacheRequests.size());
	}

	public void testDiskPrecache() {
		mAssistant.setMemCacheRange(0);
		mAssistant.setDiskCacheRange(1);

		mCount = 2;
		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
		mAssistant.onPositionVisited(0);
		assertEquals(0, mMemoryPrecacheRequests.size());
		assertEquals(1, mDiskPrecacheRequests.size());
		assertEquals("1", mDiskPrecacheRequests.get(0));
	}

	public void testMemoryAndDiskPrecache() {
		mAssistant.setMemCacheRange(1);
		mAssistant.setDiskCacheRange(2);

		mCount = 3;
		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
		mAssistant.onPositionVisited(0);
		assertEquals(1, mMemoryPrecacheRequests.size());
		assertEquals(1, mDiskPrecacheRequests.size());
	}

	public void testDirectionSwap() {
		mAssistant.setMemCacheRange(2);
		mAssistant.setDiskCacheRange(4);

		mCount = 12;

		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
		mAssistant.onPositionVisited(3);
		assertEquals(2, mMemoryPrecacheRequests.size());
		assertEquals(2, mDiskPrecacheRequests.size());

		clearLists();

		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
		mAssistant.onPositionVisited(2);
		assertEquals(2, mMemoryPrecacheRequests.size());
		assertEquals(0, mDiskPrecacheRequests.size());
	}

	private void clearLists() {
		mDiskPrecacheRequests.clear();
		mMemoryPrecacheRequests.clear();
		mList.clear();
	}
}
