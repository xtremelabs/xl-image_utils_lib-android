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

import java.io.IOException;
import java.io.InputStream;

public class DiskCacheStub implements ImageDiskCacherInterface {
	@Override
	public void downloadImageFromInputStream(String uri, InputStream inputStream) throws IOException {
	}

	@Override
	public boolean isCached(String uri) {
		return false;
	}

	@Override
	public int getSampleSize(ImageRequest imageRequest) {
		return 0;
	}

	@Override
	public void bumpOnDisk(String uri) {
	}

	@Override
	public void setDiskCacheSize(long sizeInBytes) {
	}

	@Override
	public Dimensions getImageDimensions(String uri) {
		return null;
	}

	@Override
	public boolean isDecodeRequestPending(DecodeSignature decodeSignature) {
		return false;
	}

	@Override
	public void retrieveImageDetails(String uri) {
	}

	@Override
	public void getBitmapAsynchronouslyFromDisk(DecodeSignature decodeSignature, ImageReturnedFrom returnedFrom, boolean noPreviousNetworkRequest) {
	}

	@Override
	public void bumpInQueue(DecodeSignature decodeSignature) {
	}

	@Override
	public void invalidateFileSystemUri(String uri) {
	}
}
