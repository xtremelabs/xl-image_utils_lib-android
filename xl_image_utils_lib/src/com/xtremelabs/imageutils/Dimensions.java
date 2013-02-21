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

// TODO This class' visibility should idealy be default. Change it once the ImageLoader is no longer using it.
public class Dimensions {
	public final Integer width;
	public final Integer height;

	public Dimensions(Integer width, Integer height) {
		this.width = width;
		this.height = height;
	}

	public Dimensions(Dimensions dimensionsToCopy) {
		width = dimensionsToCopy.width;
		height = dimensionsToCopy.height;
	}
}
