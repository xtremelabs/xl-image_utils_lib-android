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

public class DecodeOperationParameters {
	RequestIdentifier mRequestIdentifier;
	int mSampleSize;

	DecodeOperationParameters(RequestIdentifier requestIdentifier, int sampleSize) {
		mRequestIdentifier = requestIdentifier;
		mSampleSize = sampleSize;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mRequestIdentifier == null) ? 0 : mRequestIdentifier.hashCode());
		result = prime * result + mSampleSize;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DecodeOperationParameters other = (DecodeOperationParameters) obj;
		if (mRequestIdentifier == null) {
			if (other.mRequestIdentifier != null)
				return false;
		} else if (!mRequestIdentifier.equals(other.mRequestIdentifier))
			return false;
		if (mSampleSize != other.mSampleSize)
			return false;
		return true;
	}
}
