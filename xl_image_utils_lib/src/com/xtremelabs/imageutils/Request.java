package com.xtremelabs.imageutils;

public class Request<T> {
	private final T mData;

	public Request(T data) {
		mData = data;
	}

	public T getData() {
		return mData;
	}

	@Override
	public int hashCode() {
		return mData.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof Request))
			return false;
		return mData.equals(((Request<?>) o).mData);
	}
}
