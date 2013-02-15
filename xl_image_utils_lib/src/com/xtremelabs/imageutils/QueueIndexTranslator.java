package com.xtremelabs.imageutils;

public class QueueIndexTranslator {
	/*
	 * Priorities for request types:
	 * 
	 * FIRST CONDITION: Images on-screen must have greater priority that images off screen. We need to show visible images first.
	 * 
	 * SECOND CONDITION: Images in adapters have lower priority that images outside of adapters. This is because adapter images are more likely to be scrolled away from sooner.
	 * 
	 * THIRD CONDITION: Items called for precaching into memory take precedence over items called for precaching just to disk.
	 */
	public static int translateToIndex(ImageRequestType imageRequestType) {
		switch (imageRequestType) {
		case DEFAULT:
			return 0;
		case ADAPTER_REQUEST:
			return 1;
		case PRECACHE_TO_MEMORY_FOR_ADAPTER:
			return 2;
		case PRECACHE_TO_DISK_FOR_ADAPTER:
			return 3;
		case DEPRIORITIZED_FOR_ADAPTER:
			return 4;
		case PRECACHE_TO_MEMORY:
			return 5;
		case PRECACHE_TO_DISK:
			return 6;
		default:
			throw new IllegalArgumentException("Unrecognized ImageRequestType!");
		}
	}
}
