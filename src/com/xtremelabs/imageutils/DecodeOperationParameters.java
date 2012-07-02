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

class DecodeOperationParameters {
	String mUrl;
	int mSampleSize;

	DecodeOperationParameters(String url, int sampleSize) {
		mUrl = url;
		mSampleSize = sampleSize;
	}

	@Override
	public int hashCode() {
		int hash;
		hash = 31 * mSampleSize + 17;
		hash += mUrl.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DecodeOperationParameters)) {
			return false;
		}

		DecodeOperationParameters otherObject = (DecodeOperationParameters) o;
		if (otherObject.mSampleSize == mSampleSize && otherObject.mUrl.equals(mUrl)) {
			return true;
		}
		return false;
	}
}
