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

package com.xtremelabs.imageutils.testutils;

import java.io.IOException;
import java.io.InputStream;

public class OneKilobyteStream extends InputStream {
	private final long ONE_KILOBYTE = 1024;
	private long mBytesRead = 0;

	@Override
	public int read() throws IOException {
		if (mBytesRead < ONE_KILOBYTE) {
			mBytesRead++;
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		int numBytesRead = (int) Math.min(buffer.length, ONE_KILOBYTE - mBytesRead);
		mBytesRead += numBytesRead;
		return numBytesRead;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		int numBytesRead = (int) Math.min(length, ONE_KILOBYTE - mBytesRead);
		mBytesRead += numBytesRead;
		return numBytesRead;
	}

	@Override
	public synchronized void reset() throws IOException {
		mBytesRead = 0;
	}
}
