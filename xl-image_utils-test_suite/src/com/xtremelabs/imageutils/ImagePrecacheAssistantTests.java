// TODO These tests need to be repaired for the new separate disk/memory calls.

///*
// * Copyright 2013 Xtreme Labs
// * 
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * 
// *     http://www.apache.org/licenses/LICENSE-2.0
// *     
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.xtremelabs.imageutils;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import android.test.ActivityInstrumentationTestCase2;
//
//import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheInformationProvider;
//import com.xtremelabs.imageutils.ImagePrecacheAssistant.PrecacheRequest;
//import com.xtremelabs.testactivity.MainActivity;
//
//public class ImagePrecacheAssistantTests extends ActivityInstrumentationTestCase2<MainActivity> {
//	private ImagePrecacheAssistant mAssistant;
//	private ImageLoader mImageLoader;
//	private int mCount;
//	private final List<PrecacheRequest> mList = new ArrayList<ImagePrecacheAssistant.PrecacheRequest>();
//
//	private final List<String> mDiskPrecacheRequests = new ArrayList<String>();
//	private final List<String> mMemoryPrecacheRequests = new ArrayList<String>();
//	private final List<Integer> mPositionsRequested = new ArrayList<Integer>();
//
//	public ImagePrecacheAssistantTests() {
//		super(MainActivity.class);
//	}
//
//	@Override
//	protected void setUp() throws Exception {
//		super.setUp();
//
//		ThreadChecker.disableUiThreadCheck();
//
//		mImageLoader = new ImageLoader(getActivity()) {
//			@Override
//			public void precacheImageToDisk(String uri) {
//				mDiskPrecacheRequests.add(uri);
//			}
//
//			@Override
//			public void precacheImageToDiskAndMemory(String uri, Dimensions bounds, Options options) {
//				mMemoryPrecacheRequests.add(uri);
//			}
//		};
//
//		mAssistant = new ImagePrecacheAssistant(mImageLoader, new PrecacheInformationProvider() {
//			@Override
//			public List<PrecacheRequest> onRowPrecacheRequestsRequired(int position) {
//				mPositionsRequested.add(position);
//				return mList;
//			}
//
//			@Override
//			public int getCount() {
//				return mCount;
//			}
//
//			@Override
//			public List<String> onRowPrecacheRequestsForDiskCacheRequired(int position) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public List<PrecacheRequest> onRowPrecacheRequestsForMemoryCacheRequired(int position) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		});
//
//		clearLists();
//	}
//
//	public void testListWithOneElement() {
//		mCount = 1;
//		mAssistant.onPositionVisited(0);
//		assertEquals(0, mDiskPrecacheRequests.size());
//		assertEquals(0, mMemoryPrecacheRequests.size());
//	}
//
//	public void testListWithTwoElements() {
//		mCount = 2;
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(0);
//		assertEquals(1, mMemoryPrecacheRequests.size());
//		assertEquals(0, mDiskPrecacheRequests.size());
//		assertEquals("1", mMemoryPrecacheRequests.get(0));
//		assertEquals(1, (int) mPositionsRequested.get(0));
//
//		clearLists();
//
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//
//		mAssistant.onPositionVisited(1);
//		assertEquals(0, mDiskPrecacheRequests.size());
//		assertEquals(0, mMemoryPrecacheRequests.size());
//
//		mAssistant.onPositionVisited(0);
//		assertEquals(0, mDiskPrecacheRequests.size());
//		assertEquals(0, mMemoryPrecacheRequests.size());
//	}
//
//	public void testDiskPrecache() {
//		mAssistant.setMemCacheRange(0);
//		mAssistant.setDiskCacheRange(1);
//
//		mCount = 2;
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(0);
//		assertEquals(0, mMemoryPrecacheRequests.size());
//		assertEquals(1, mDiskPrecacheRequests.size());
//		assertEquals("1", mDiskPrecacheRequests.get(0));
//		assertEquals(1, (int) mPositionsRequested.get(0));
//	}
//
//	public void testMemoryAndDiskPrecache() {
//		mAssistant.setMemCacheRange(1);
//		mAssistant.setDiskCacheRange(1);
//
//		mCount = 3;
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(0);
//		assertEquals(1, mMemoryPrecacheRequests.size());
//		assertEquals(1, mDiskPrecacheRequests.size());
//		assertEquals(2, (int) mPositionsRequested.get(0));
//		assertEquals(1, (int) mPositionsRequested.get(1));
//	}
//
//	public void testDirectionSwap() {
//		mAssistant.setMemCacheRange(2);
//		mAssistant.setDiskCacheRange(2);
//
//		mCount = 12;
//
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(3);
//		assertEquals(2, mMemoryPrecacheRequests.size());
//		assertEquals(2, mDiskPrecacheRequests.size());
//		List<Integer> expectedValues = new ArrayList<Integer>();
//		expectedValues.add(6);
//		expectedValues.add(7);
//		expectedValues.add(4);
//		expectedValues.add(5);
//		assertEquals(expectedValues, mPositionsRequested);
//
//		clearLists();
//
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(2);
//		assertEquals(1, mMemoryPrecacheRequests.size());
//		assertEquals(0, mDiskPrecacheRequests.size());
//		assertEquals(0, (int) mPositionsRequested.get(0));
//	}
//
//	public void testPositionsRequested() {
//		mAssistant.setMemCacheRange(1);
//		mAssistant.setDiskCacheRange(1);
//
//		mCount = 3;
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(0);
//		assertEquals(1, mMemoryPrecacheRequests.size());
//		assertEquals(1, mDiskPrecacheRequests.size());
//
//		assertEquals(2, (int) mPositionsRequested.get(0));
//		assertEquals(1, (int) mPositionsRequested.get(1));
//	}
//
//	public void testForNoExcessiveCalls() {
//		mAssistant.setMemCacheRange(2);
//		mAssistant.setDiskCacheRange(2);
//
//		mCount = 10;
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(0);
//		assertEquals(2, mMemoryPrecacheRequests.size());
//		assertEquals(2, mDiskPrecacheRequests.size());
//
//		clearLists();
//
//		mList.add(new PrecacheRequest("1", new Dimensions(null, null)));
//		mAssistant.onPositionVisited(1);
//		assertEquals(1, mMemoryPrecacheRequests.size());
//		assertEquals(1, mDiskPrecacheRequests.size());
//		assertEquals(5, (int) mPositionsRequested.get(0));
//		assertEquals(3, (int) mPositionsRequested.get(1));
//	}
//
//	private void clearLists() {
//		mDiskPrecacheRequests.clear();
//		mMemoryPrecacheRequests.clear();
//		mList.clear();
//		mPositionsRequested.clear();
//	}
// }
