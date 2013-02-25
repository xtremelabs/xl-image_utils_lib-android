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

class QueueIndexTranslator {
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
		case DEPRIORITIZED:
		case DEPRIORITIZED_PRECACHE_TO_MEMORY_FOR_ADAPTER:
		case DEPRIORITIZED_PRECACHE_TO_DISK_FOR_ADAPTER:
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
