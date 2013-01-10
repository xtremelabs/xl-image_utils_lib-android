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

import android.graphics.Bitmap;

public class DecodeSignature {
	String mUri;
	int mSampleSize;
	Bitmap.Config mBitmapConfig;

	DecodeSignature(String uri, int sampleSize, Bitmap.Config bitmapConfig) {
		mUri = uri;
		mSampleSize = sampleSize;
		mBitmapConfig = bitmapConfig;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mBitmapConfig == null) ? 0 : mBitmapConfig.hashCode());
		result = prime * result + mSampleSize;
		result = prime * result + ((mUri == null) ? 0 : mUri.hashCode());
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
		DecodeSignature other = (DecodeSignature) obj;
		if (mBitmapConfig != other.mBitmapConfig)
			return false;
		if (mSampleSize != other.mSampleSize)
			return false;
		if (mUri == null) {
			if (other.mUri != null)
				return false;
		} else if (!mUri.equals(other.mUri))
			return false;
		return true;
	}
}
