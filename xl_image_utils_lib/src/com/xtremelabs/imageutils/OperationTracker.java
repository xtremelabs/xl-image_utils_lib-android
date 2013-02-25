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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OperationTracker<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> {
	private final Map<OPERATION_KEY, List<OPERATION_LIST_VALUE>> mOperationKeyToValueList = new HashMap<OPERATION_KEY, List<OPERATION_LIST_VALUE>>();
	private final Map<KEY_REFERENCE, OPERATION_KEY> mReferenceToOperation = new HashMap<KEY_REFERENCE, OPERATION_KEY>();

	public synchronized void register(OPERATION_KEY operationKey, OPERATION_LIST_VALUE operationListValue, KEY_REFERENCE keyReference) {
		List<OPERATION_LIST_VALUE> list = mOperationKeyToValueList.get(operationKey);
		if (list == null) {
			list = new ArrayList<OPERATION_LIST_VALUE>();
			mOperationKeyToValueList.put(operationKey, list);
		}
		list.add(operationListValue);

		mReferenceToOperation.put(keyReference, operationKey);
	}

	public synchronized void removeRequest(OPERATION_KEY key, ValueMatcher<OPERATION_LIST_VALUE> valueMatcher, KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider) {
		List<OPERATION_LIST_VALUE> valueList = mOperationKeyToValueList.get(key);
		if (valueList != null) {
			int valueListSize = valueList.size();
			for (int i = 0; i < valueListSize; i++) {
				OPERATION_LIST_VALUE value = valueList.get(i);
				if (valueMatcher.shouldRemoveValue(value)) {
					valueList.remove(i);
					mReferenceToOperation.remove(keyReferenceProvider.getKeyReference(key, value));
					if (valueList.size() == 0) {
						mOperationKeyToValueList.remove(key);
					}
					break;
				}
			}
		}
	}

	public synchronized boolean hasPendingOperation(OPERATION_KEY operationKey) {
		return mOperationKeyToValueList.containsKey(operationKey);
	}

	public synchronized int getNumPendingOperations() {
		return mOperationKeyToValueList.size();
	}

	public synchronized int getNumListValues() {
		return mReferenceToOperation.size();
	}

	public synchronized boolean isOperationPendingForReference(KEY_REFERENCE keyReference) {
		return mReferenceToOperation.containsKey(keyReference);
	}

	public List<OPERATION_LIST_VALUE> transferOperationToTracker(OPERATION_KEY operationKey, OperationTracker<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> targetTracker,
			KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider) {

		List<OPERATION_LIST_VALUE> operationValueList;
		List<KEY_REFERENCE> keyReferences;
		synchronized (this) {
			operationValueList = mOperationKeyToValueList.remove(operationKey);
			keyReferences = getAndRemoveKeyReferences(operationKey, operationValueList, keyReferenceProvider);
		}

		if (operationValueList != null) {
			targetTracker.initializeList(operationKey);

			int listSize = operationValueList.size();
			for (int i = 0; i < listSize; i++) {
				OPERATION_LIST_VALUE value = operationValueList.get(i);
				KEY_REFERENCE keyReference = keyReferences.get(i);
				targetTracker.register(operationKey, value, keyReference);
			}
		}

		return operationValueList == null ? new ArrayList<OPERATION_LIST_VALUE>() : operationValueList;
	}

	private synchronized void initializeList(OPERATION_KEY operationKey) {
		mOperationKeyToValueList.put(operationKey, new ArrayList<OPERATION_LIST_VALUE>());
	}

	public void transferOperation(OPERATION_KEY operationKey, OperationTransferer<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> operationTransferer,
			KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider) {

		List<OPERATION_LIST_VALUE> list;
		List<KEY_REFERENCE> keyReferences;
		synchronized (this) {
			list = mOperationKeyToValueList.remove(operationKey);
			keyReferences = getAndRemoveKeyReferences(operationKey, list, keyReferenceProvider);
		}

		if (list != null) {
			int listSize = list.size();
			for (int i = 0; i < listSize; i++) {
				operationTransferer.transferOperation(operationKey, list.get(i), keyReferences.get(i));
			}
		}
	}

	private synchronized List<KEY_REFERENCE> getAndRemoveKeyReferences(OPERATION_KEY operationKey, List<OPERATION_LIST_VALUE> list, KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider) {
		List<KEY_REFERENCE> keyReferences = new ArrayList<KEY_REFERENCE>();
		if (list != null) {
			for (OPERATION_LIST_VALUE value : list) {
				KEY_REFERENCE keyReference = keyReferenceProvider.getKeyReference(operationKey, value);
				mReferenceToOperation.remove(keyReference);
				keyReferences.add(keyReference);
			}
		}
		return keyReferences;
	}

	public synchronized List<OPERATION_LIST_VALUE> removeList(OPERATION_KEY operationKey, KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider) {
		List<OPERATION_LIST_VALUE> list = mOperationKeyToValueList.remove(operationKey);

		if (list != null) {
			for (OPERATION_LIST_VALUE value : list) {
				mReferenceToOperation.remove(keyReferenceProvider.getKeyReference(operationKey, value));
			}
		}

		return list;
	}

	public synchronized OPERATION_LIST_VALUE removeRequest(KEY_REFERENCE keyReference, KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider, boolean deleteMapIfEmpty) {
		OPERATION_LIST_VALUE valueToRemove = null;

		OPERATION_KEY operationKey = mReferenceToOperation.remove(keyReference);
		if (operationKey != null) {
			List<OPERATION_LIST_VALUE> list = mOperationKeyToValueList.get(operationKey);
			if (list != null) {
				valueToRemove = findOperationListValueToRemove(keyReference, keyReferenceProvider, operationKey, list);
				if (valueToRemove != null) {
					list.remove(valueToRemove);

					if (deleteMapIfEmpty && list.isEmpty()) {
						mOperationKeyToValueList.remove(operationKey);
					}
				}
			}
		}

		return valueToRemove;
	}

	private OPERATION_LIST_VALUE findOperationListValueToRemove(KEY_REFERENCE keyReference, KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> keyReferenceProvider, OPERATION_KEY operationKey,
			List<OPERATION_LIST_VALUE> list) {
		OPERATION_LIST_VALUE valueToRemove = null;
		for (OPERATION_LIST_VALUE value : list) {
			KEY_REFERENCE keyReferenceContainedInValue = keyReferenceProvider.getKeyReference(operationKey, value);
			if (keyReference.equals(keyReferenceContainedInValue)) {
				valueToRemove = value;
				break;
			}
		}
		return valueToRemove;
	}

	public interface KeyReferenceProvider<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> {
		public KEY_REFERENCE getKeyReference(OPERATION_KEY operationKey, OPERATION_LIST_VALUE operationListValue);
	}

	public interface OperationTransferer<OPERATION_KEY, OPERATION_LIST_VALUE, KEY_REFERENCE> {
		public void transferOperation(OPERATION_KEY operationKey, OPERATION_LIST_VALUE operationListValue, KEY_REFERENCE keyReference);
	}

	public interface ValueMatcher<OPERATION_LIST_VALUE> {
		public boolean shouldRemoveValue(OPERATION_LIST_VALUE value);
	}
}
