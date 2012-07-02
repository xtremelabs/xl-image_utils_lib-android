/*
 * Copyright 2012 Xtreme Labs
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

class EvictionQueueContainer {
	private String mUrl;
	private int mSampleSize;

	public EvictionQueueContainer(String url, int sampleSize) {
		mUrl = url;
		mSampleSize = sampleSize;
	}

	public String getUrl() {
		return mUrl;
	}

	public int getSampleSize() {
		return mSampleSize;
	}

	public String toString() {
		return "EvictionQueueContainer url, samplesize: (" + this.mUrl + ", " + this.mSampleSize + ")"; 
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof EvictionQueueContainer))
			return false;

		final EvictionQueueContainer evictionQueueContainer = (EvictionQueueContainer) o;
		final String url = evictionQueueContainer.getUrl();
		final int sampleSize = evictionQueueContainer.getSampleSize();

		return (mUrl != null && mUrl.equals(url)) && sampleSize == mSampleSize;
	}
}
