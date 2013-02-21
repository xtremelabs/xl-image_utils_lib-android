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

interface PriorityAccessor {

	public void attach(Prioritizable prioritizable);

	public Prioritizable detachHighestPriorityItem();

	public int size();

	public Prioritizable peek();

	public void clear();

	public void swap(CacheKey cacheKey, PriorityAccessor priorityAccessor, PriorityAccessor priorityAccessor2);
}
