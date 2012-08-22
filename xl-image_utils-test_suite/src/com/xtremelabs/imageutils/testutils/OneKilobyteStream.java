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
