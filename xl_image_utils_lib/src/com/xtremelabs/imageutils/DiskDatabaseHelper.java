package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DiskDatabaseHelper extends SQLiteOpenHelper {
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

		List<FileEntry> entries = getAllEntries();
		Comparator<FileEntry> comparator = new Comparator<FileEntry>() {
			@Override
			public int compare(FileEntry lhs, FileEntry rhs) {
				return (int) (lhs.getLastAccessTime() - rhs.getLastAccessTime());
			}
		};
		Collections.sort(entries, comparator);
		for (FileEntry entry : entries) {
			mDatabaseCache.put(entry.getUrl(), entry.getLastAccessTime());
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

	public FileEntry getFileEntry(String url) {
		Cursor cursor = getReadableDatabase().query(DICTIONARY_TABLE_NAME, columns, columns[0] + " = ?", new String[] { url }, null, null, null);
		if (cursor.getCount() == 0) {
			return null;
		} else {
			cursor.moveToFirst();
			FileEntry retval = createFileEntry(cursor);
			cursor.close();
			return retval;
		}
	}

	public List<FileEntry> getAllEntries() {
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

		getWritableDatabase().insertWithOnConflict(DICTIONARY_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);

		mDatabaseCache.put(url, updateTime);
	}

	public boolean removeFile(String url) {
		String[] args = { url };
		mDatabaseCache.remove(url);
		return 1 == getWritableDatabase().delete(DICTIONARY_TABLE_NAME, columns[0] + " = ?", args);
	}

	/*
	 * TODO: Have the LRU information cached in memory. The database updates can take time.
	 */
	public void updateFile(final String url) {
		if (GeneralUtils.isStringBlank(url)) {
			throw new IllegalArgumentException("Cannot add a null URL to the database.");
		}
		final ContentValues values = new ContentValues(1);
		long updateTime = System.currentTimeMillis();
		values.put(columns[4], System.currentTimeMillis());
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getWritableDatabase().update(DICTIONARY_TABLE_NAME, values, columns[0] + " = ?", new String[] { url });
			}
		});

		mDatabaseCache.put(url, updateTime);
	}

	public void resetTable(SQLiteDatabase db) {
		db.execSQL("DROP TABLE " + DICTIONARY_TABLE_NAME);
		db.execSQL(DICTIONARY_TABLE_CREATE);
		mDatabaseCache = new DatabaseCache();
	}

	public long getTotalSizeOnDisk() {
		Cursor cursor = getReadableDatabase().rawQuery("SELECT sum(" + columns[1] + ") AS Sum FROM " + DICTIONARY_TABLE_NAME, null);
		long retval;
		if (cursor.getCount() == 0) {
			retval = 0;
		} else {
			cursor.moveToFirst();
			retval = cursor.getLong(0);
			cursor.close();
		}
		return retval;
	}

	public FileEntry getLRU() {
		// Cursor cursor = getReadableDatabase().rawQuery(
		// "SELECT * FROM " + DICTIONARY_TABLE_NAME + " WHERE " + columns[4] + " = (SELECT min(" + columns[4] + ") AS min FROM " + DICTIONARY_TABLE_NAME
		// + ")", null);
		// if (cursor.getCount() == 0) {
		// return null;
		// }
		// cursor.moveToFirst();
		// FileEntry entry = createFileEntry(cursor);
		// cursor.close();
		return getFileEntry(mDatabaseCache.getLRU());
	}

	private FileEntry createFileEntry(Cursor cursor) {
		FileEntry fileEntry = new FileEntry(cursor.getString(0), cursor.getLong(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4));
		return fileEntry;
	}

	public static interface DiskDatabaseHelperObserver {
		public void onDatabaseWiped();
	}
}
