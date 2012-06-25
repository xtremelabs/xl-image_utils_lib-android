package com.xtremelabs.imageutils;

class ScalingInfo {
	public Integer sampleSize = null;
	public Integer width;
	public Integer height;
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		
		if (!(o instanceof ScalingInfo)) {
			return false;
		}
		
		ScalingInfo info = (ScalingInfo) o;
		
		if (info.sampleSize != sampleSize || info.width != width || info.height != height) {
			return false;
		}
		
		return true;
	}
}
