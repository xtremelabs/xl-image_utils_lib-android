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

interface ImageMemoryCacherInterface {
	Bitmap getBitmap(DecodeSignature decodeSignature);

	void cacheBitmap(Bitmap bitmap, DecodeSignature decodeSignature);

	void clearCache();

	void setMaximumCacheSize(long size);
	
	void removeAllImagesForUri(String uri);

	void trimCache(double percetangeToRemove);

	void trimCache(long numBytes);
	
	void trimCacheToPercentageOfMaximum(double percentage);
	
	void trimCacheToSize(long numBytes);
}
