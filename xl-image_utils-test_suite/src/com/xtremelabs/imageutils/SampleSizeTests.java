/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xtremelabs.imageutils;

import android.test.ActivityInstrumentationTestCase2;

import com.xtremelabs.imageutils.ImageLoader.Options;
import com.xtremelabs.imageutils.ImageLoader.Options.ScalingPreference;
import com.xtremelabs.testactivity.MainActivity;

public class SampleSizeTests extends ActivityInstrumentationTestCase2<MainActivity> {
	private CacheRequest mCacheRequest;
	private ScalingInfo mScalingInfo;
	private Options mOptions;
	private Dimensions mDimensions;

	public SampleSizeTests() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mDimensions = new Dimensions(512, 512);
		mScalingInfo = new ScalingInfo();
		mOptions = new Options();
		mCacheRequest = new CacheRequest("some uri", mScalingInfo, mOptions);
		mOptions.scalingPreference = ScalingPreference.LARGER_THAN_VIEW_OR_FULL_SIZE;
	}

	public void testSampleSizesWithLargerThanView() {
		mOptions.scalingPreference = ScalingPreference.LARGER_THAN_VIEW_OR_FULL_SIZE;

		mScalingInfo.width = 600;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 500;
		mScalingInfo.height = 500;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 600;
		mScalingInfo.height = 100;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 170;
		mScalingInfo.height = 170;
		assertEquals(3, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = 100;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = null;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = null;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 128;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));
	}

	public void testSampleSizesMatchToLargerDimension() {
		mOptions.scalingPreference = ScalingPreference.MATCH_TO_LARGER_DIMENSION;

		mScalingInfo.width = 600;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 500;
		mScalingInfo.height = 500;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 600;
		mScalingInfo.height = 100;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 170;
		mScalingInfo.height = 170;
		assertEquals(3, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = 100;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = null;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = null;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 128;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));
	}

	public void testSampleSizesMatchToSmallerDimension() {
		mOptions.scalingPreference = ScalingPreference.MATCH_TO_SMALLER_DIMENSION;

		mScalingInfo.width = 600;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 500;
		mScalingInfo.height = 500;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = 600;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 600;
		mScalingInfo.height = 100;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 170;
		mScalingInfo.height = 170;
		assertEquals(3, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = 100;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = null;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = null;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 128;
		assertEquals(4, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));
	}

	public void testSampleSizesSmallerThanView() {
		mOptions.scalingPreference = ScalingPreference.SMALLER_THAN_VIEW;

		mScalingInfo.width = 600;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 500;
		mScalingInfo.height = 500;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = 600;
		assertEquals(6, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 600;
		mScalingInfo.height = 100;
		assertEquals(6, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 170;
		mScalingInfo.height = 170;
		assertEquals(4, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = 100;
		assertEquals(6, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = null;
		assertEquals(6, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = null;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 128;
		assertEquals(4, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));
	}

	public void testSampleSizesRoundToClosestMatch() {
		mOptions.scalingPreference = ScalingPreference.ROUND_TO_CLOSEST_MATCH;

		mScalingInfo.width = 600;
		mScalingInfo.height = 600;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 500;
		mScalingInfo.height = 500;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = 600;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 600;
		mScalingInfo.height = 100;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 170;
		mScalingInfo.height = 170;
		assertEquals(3, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = 100;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 100;
		mScalingInfo.height = null;
		assertEquals(5, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = null;
		mScalingInfo.height = null;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 128;
		assertEquals(4, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));
	}

	public void testBasicSampleSizeCalculations() {
		mScalingInfo.width = 512;
		mScalingInfo.height = 512;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 2000;
		mScalingInfo.height = 2000;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 257;
		mScalingInfo.height = 257;
		assertEquals(1, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 256;
		mScalingInfo.height = 256;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 250;
		mScalingInfo.height = 250;
		assertEquals(2, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 129;
		mScalingInfo.height = 129;
		assertEquals(3, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 128;
		mScalingInfo.height = 128;
		assertEquals(4, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 64;
		mScalingInfo.height = 64;
		assertEquals(8, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 32;
		mScalingInfo.height = 32;
		assertEquals(16, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));

		mScalingInfo.width = 16;
		mScalingInfo.height = 16;
		assertEquals(32, SampleSizeCalculationUtility.calculateSampleSize(mCacheRequest, mDimensions));
	}
}
