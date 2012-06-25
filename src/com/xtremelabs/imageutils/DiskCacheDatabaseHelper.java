package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DiskCacheDatabaseHelper extends SQLiteOpenHelper {
	private static final String TAG = "DiskCacheDatabaseHelper";

	private String[] columns = { "url", "sizeondisk", "width", "height", "lastaccess" };

	private final static int DATABASE_VERSION = 2;
	private final String DICTIONARY_TABLE_NAME = "img_cache";
	private final String DICTIONARY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + DICTIONARY_TABLE_NAME + " (" + columns[0] + " VARCHAR PRIMARY KEY, "
			+ columns[1] + " INTEGER, " + columns[2] + " INTEGER, " + columns[3] + " INTEGER, " + columns[4] + " INTEGER);";
	private final static String DATABASE_NAME = "imageCacheDatabase";
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

	public DiskCacheDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DICTIONARY_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO: Fill this in. Kinda important...
		resetTable(db);
	}

	public FileEntry getFileEntry(String url) {
		Cursor cursor = getReadableDatabase().query(DICTIONARY_TABLE_NAME, columns, columns[0] + " = ?", new String[] { url }, null, null, null);
		if (cursor.getCount() == 0) {
			Log.d(TAG, "Sample size - Was unable to get file entry!");
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
		
		ContentValues values = new ContentValues();
		values.put(columns[0], url);
		values.put(columns[1], size);
		values.put(columns[2], width);
		values.put(columns[3], height);
		values.put(columns[4], System.currentTimeMillis());
		
		getWritableDatabase().insertWithOnConflict(DICTIONARY_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public boolean removeFile(String url) {
		String[] args = { url };
		return 1 == getWritableDatabase().delete(DICTIONARY_TABLE_NAME, columns[0] + " = ?", args);
	}

	public void updateFile(final String url) {
		if (GeneralUtils.isStringBlank(url)) {
			throw new IllegalArgumentException("Cannot add a null URL to the database.");
		}
		final ContentValues values = new ContentValues(1);
		values.put(columns[4], System.currentTimeMillis());
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getWritableDatabase().update(DICTIONARY_TABLE_NAME, values, columns[0] + " = ?", new String[] { url });
			}
		});
	}

	public void resetTable(SQLiteDatabase db) {
		db.execSQL("DROP TABLE " + DICTIONARY_TABLE_NAME);
		db.execSQL(DICTIONARY_TABLE_CREATE);
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
		Cursor cursor = getReadableDatabase().rawQuery(
				"SELECT * FROM " + DICTIONARY_TABLE_NAME + " WHERE " + columns[4] + " = (SELECT min(" + columns[4] + ") AS min FROM " + DICTIONARY_TABLE_NAME
						+ ")", null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		FileEntry entry = createFileEntry(cursor);
		cursor.close();
		return entry;
	}

	private FileEntry createFileEntry(Cursor cursor) {
		FileEntry fileEntry = new FileEntry(cursor.getString(0), cursor.getLong(1), cursor.getInt(2), cursor.getInt(3), cursor.getLong(4));
		Log.d(TAG, "Sample size - File entry stats: " + fileEntry.getLastAccessTime() + ", " + fileEntry.getDimensions().getWidth() + ", "
				+ fileEntry.getDimensions().getHeight());
		return fileEntry;
	}
}
