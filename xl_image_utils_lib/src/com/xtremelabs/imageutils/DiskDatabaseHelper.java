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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DiskDatabaseHelper extends SQLiteOpenHelper {
	// TODO: Map columns to indices (Bug Josh).
	private final String[] columns = { "url", "sizeondisk", "width", "height", "lastaccess" };

	private final static int DATABASE_VERSION = 2;
	private final String DICTIONARY_TABLE_NAME = "img_cache";
	private final String DICTIONARY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + DICTIONARY_TABLE_NAME + " (" + columns[0] + " VARCHAR PRIMARY KEY, " + columns[1] + " INTEGER, " + columns[2] + " INTEGER, " + columns[3] + " INTEGER, "
			+ columns[4] + " INTEGER);";
	private final static String DATABASE_NAME = "imageCacheDatabase";
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private DiskDatabaseHelperObserver mObserver;

	private DatabaseCache mDatabaseCache = new DatabaseCache();

	public DiskDatabaseHelper(Context context, DiskDatabaseHelperObserver observer) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mObserver = observer;

		List<FileEntry> entries = getAllEntriesFromDatabase();
		Comparator<FileEntry> comparator = new Comparator<FileEntry>() {
			@Override
			public int compare(FileEntry lhs, FileEntry rhs) {
				long difference = lhs.getLastAccessTime() - rhs.getLastAccessTime();
				if (difference < 0) {
					return -1;
				} else if (difference > 0) {
					return 1;
				} else {
					return 0;
				}
			}
		};
		Collections.sort(entries, comparator);
		for (FileEntry entry : entries) {
			mDatabaseCache.put(entry.getUri(), entry);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DICTIONARY_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		resetTable(db);
		mObserver.onDatabaseWiped();
	}

	public FileEntry getFileEntryFromDatabase(String uri) {
		Cursor cursor = getReadableDatabase().query(DICTIONARY_TABLE_NAME, columns, columns[0] + " = ?", new String[] { uri }, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		} else {
			cursor.moveToFirst();
			FileEntry retval = createFileEntry(cursor);
			cursor.close();
			return retval;
		}
	}

	public FileEntry getFileEntryFromCache(String uri) {
		return mDatabaseCache.getFileEntry(uri);
	}

	private List<FileEntry> getAllEntriesFromDatabase() {
		Cursor cursor = getReadableDatabase().query(DICTIONARY_TABLE_NAME, null, null, null, null, null, null);
		List<FileEntry> list = new ArrayList<FileEntry>();
		while (cursor.moveToNext()) {
			list.add(createFileEntry(cursor));
		}
		cursor.close();
		return list;
	}

	public void addOrUpdateFile(String url, long size, int width, int height) {
		if (GeneralUtils.isStringBlank(url)) {
			throw new IllegalArgumentException("Cannot add a null URL to the database.");
		}

		long updateTime = System.currentTimeMillis();

		ContentValues values = new ContentValues();
		values.put(columns[0], url);
		values.put(columns[1], size);
		values.put(columns[2], width);
		values.put(columns[3], height);
		values.put(columns[4], updateTime);

		mDatabaseCache.put(url, new FileEntry(url, size, width, height, updateTime));
		getWritableDatabase().insertWithOnConflict(DICTIONARY_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	private boolean removeFileFromDatabase(String uri) {
		String[] args = { uri };
		return 1 == getWritableDatabase().delete(DICTIONARY_TABLE_NAME, columns[0] + " = ?", args);
	}

	/*
	 * TODO: Have the LRU information cached in memory. The database updates can take time.
	 */
	public void updateFile(final String uri) {
		if (GeneralUtils.isStringBlank(uri)) {
			throw new IllegalArgumentException("Cannot add a null URL to the database.");
		}
		executor.execute(new Runnable() {
			@Override
			public void run() {
				ContentValues values = new ContentValues(1);
				long updateTime = System.currentTimeMillis();
				values.put(columns[4], System.currentTimeMillis());
				mDatabaseCache.updateTime(uri, updateTime);
				getWritableDatabase().update(DICTIONARY_TABLE_NAME, values, columns[0] + " = ?", new String[] { uri });
			}
		});
	}

	void resetTable(SQLiteDatabase db) {
		db.execSQL("DROP TABLE " + DICTIONARY_TABLE_NAME);
		db.execSQL(DICTIONARY_TABLE_CREATE);
		mDatabaseCache = new DatabaseCache();
	}

	private static FileEntry createFileEntry(Cursor cursor) {
		FileEntry fileEntry = new FileEntry(cursor.getString(0), cursor.getLong(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4));
		return fileEntry;
	}

	public static interface DiskDatabaseHelperObserver {
		public void onDatabaseWiped();

		public void onImageEvicted(String uri);
	}

	public boolean isCached(String uri) {
		return mDatabaseCache.isCached(uri);
	}

	public void removeLeastUsedFileFromCache(long maximumCacheSize) {
		String uri;
		while ((uri = mDatabaseCache.removeLRU(maximumCacheSize)) != null) {
			removeFileFromDatabase(uri);
			mObserver.onImageEvicted(uri);
		}
	}

	public void deleteEntry(String uri) {
		mDatabaseCache.remove(uri);
		removeFileFromDatabase(uri);
	}

	public long getTotalSizeOnDisk() {
		return mDatabaseCache.getTotalSizeOnDisk();
	}

	public Collection<FileEntry> getAllEntries() {
		return mDatabaseCache.getAllEntries();
	}

	String getLRU() {
		return mDatabaseCache.getLRU();
	}
}
