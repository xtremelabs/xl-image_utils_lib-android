package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DiskCacheDatabaseHelper extends SQLiteOpenHelper {
	private String[] columns = { "url", "sizeondisk", "lastaccess" };

	private final static int DATABASE_VERSION = 1;
	private final String DICTIONARY_TABLE_NAME = "img_cache";
	private final String DICTIONARY_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + DICTIONARY_TABLE_NAME + " (" + columns[0] + " VARCHAR PRIMARY KEY, " + columns[1] + " INTEGER, " + columns[2]
			+ " INTEGER);";
	private final static String DATABASE_NAME = "imageCacheDatabase";

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
	}

	public FileEntry getFileEntry(String url) {
		String[] entries = { url };
		Cursor cursor = getReadableDatabase().query(DICTIONARY_TABLE_NAME, columns, columns[0] + " = ?", entries, null, null, null);
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

	public boolean addFile(String url, long size, long lastAccessTime) {
		if (GeneralUtils.isStringBlank(url)) {
			throw new IllegalArgumentException("Cannot add a null URL to the database.");
		}

		ContentValues values = new ContentValues(3);
		values.put(columns[0], url);
		values.put(columns[1], size);
		values.put(columns[2], lastAccessTime);
		return -1 != getWritableDatabase().insert(DICTIONARY_TABLE_NAME, null, values);
	}

	public boolean removeFile(String url) {
		String[] args = { url };
		return 1 == getWritableDatabase().delete(DICTIONARY_TABLE_NAME, columns[0] + " = ?", args);
	}

	public boolean updateFile(String url, long lastAccessTime) {
		if (GeneralUtils.isStringBlank(url)) {
			throw new IllegalArgumentException("Cannot add a null URL to the database.");
		}

		ContentValues values = new ContentValues(1);
		values.put(columns[2], lastAccessTime);
		String[] entries = { url };
		return 1 == getWritableDatabase().update(DICTIONARY_TABLE_NAME, values, columns[0] + " = ?", entries);
	}

	public void resetTable() {
		getWritableDatabase().execSQL("DROP TABLE " + DICTIONARY_TABLE_NAME);
		getWritableDatabase().execSQL(DICTIONARY_TABLE_CREATE);
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
				"SELECT * FROM " + DICTIONARY_TABLE_NAME + " WHERE " + columns[2] + " = (SELECT min(" + columns[2] + ") AS min FROM " + DICTIONARY_TABLE_NAME + ")", null);
		if (cursor.getCount() == 0) {
			return null;
		}
		cursor.moveToFirst();
		FileEntry entry = createFileEntry(cursor);
		cursor.close();
		return entry;
	}

	private FileEntry createFileEntry(Cursor cursor) {
		return new FileEntry(cursor.getString(0), cursor.getLong(1), cursor.getLong(2));
	}
}
