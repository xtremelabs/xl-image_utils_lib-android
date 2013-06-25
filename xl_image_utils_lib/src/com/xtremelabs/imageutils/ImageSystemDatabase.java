package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

import com.xtremelabs.imageutils.db.Database;
import com.xtremelabs.imageutils.db.Table;

class ImageSystemDatabase {
	private final ImagesTable mImagesTable = ImagesTable.getInstance();
	private final ImageSystemDatabaseObserver mObserver;
	private Database mDatabase;
	private final ImageSystemDatabaseCache mCache = new ImageSystemDatabaseCache();

	ImageSystemDatabase(ImageSystemDatabaseObserver imageSystemDatabaseObserver) {
		mObserver = imageSystemDatabaseObserver;
	}

	public void init(Context context) {
		List<Table<?>> tables = new ArrayList<Table<?>>();
		tables.add(mImagesTable);

		mDatabase = new Database(context, new SQLiteDatabase.CursorFactory() {
			@SuppressWarnings("deprecation")
			@Override
			public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
				return new SQLiteCursor(db, masterQuery, editTable, query);
			}
		}, tables);

		populateCache();
		performBadFileCheck();
	}

	public void beginWrite(String uri) {
		ImageEntry entry = new ImageEntry();
		entry.uri = uri;
		mImagesTable.insert(entry, mDatabase.getWritableDatabase());
		mCache.putEntry(entry);
	}

	public void endWrite(String uri) {
		ImageEntry entry = mCache.getEntry(uri);
		entry.onDisk = true;
		entry.lastAccessedTime = System.currentTimeMillis();
		mImagesTable.insert(entry, mDatabase.getWritableDatabase());
	}

	public void writeFailed(String uri) {
		ImageEntry entry = mCache.removeEntry(uri);
		String whereClause = ImagesTable.Columns.ID.getColumnName() + "=?";
		String[] whereArgs = new String[] { Long.toString(entry.id) };
		mImagesTable.delete(mDatabase.getWritableDatabase(), whereClause, whereArgs);
	}

	ImageEntry getEntry(String uri) {
		return mCache.getEntry(uri);
	}

	void clear() {
		mCache.clear();
		mImagesTable.onClear(mDatabase.getWritableDatabase());
	}

	void submitDetails(String uri, Dimensions dimensions) {
		ImageEntry entry = mCache.getEntry(uri);
		entry.sizeX = dimensions.width;
		entry.sizeY = dimensions.height;
		mImagesTable.insert(entry, mDatabase.getWritableDatabase());
	}

	private void populateCache() {
		Cursor allEntries = mImagesTable.selectAllFromTable(mDatabase.getReadableDatabase());
		Log.d("JAMIE", "Entries in database: " + allEntries.getCount());
		while (allEntries.moveToNext()) {
			ImageEntry entry = new ImageEntry();
			entry.id = allEntries.getLong(allEntries.getColumnIndex(ImagesTable.Columns.ID.getColumnName()));
			entry.uri = allEntries.getString(allEntries.getColumnIndex(ImagesTable.Columns.URI.getColumnName()));
			entry.creationTime = allEntries.getLong(allEntries.getColumnIndex(ImagesTable.Columns.CREATION_TIME.getColumnName()));
			entry.lastAccessedTime = allEntries.getLong(allEntries.getColumnIndex(ImagesTable.Columns.LAST_ACCESSED_TIME.getColumnName()));
			entry.onDisk = allEntries.getInt(allEntries.getColumnIndex(ImagesTable.Columns.ON_DISK.getColumnName())) == 1;
			entry.sizeX = allEntries.getInt(allEntries.getColumnIndex(ImagesTable.Columns.SIZE_X.getColumnName()));
			entry.sizeY = allEntries.getInt(allEntries.getColumnIndex(ImagesTable.Columns.SIZE_Y.getColumnName()));

			mCache.putEntry(entry);

			Log.d("JAMIE", "Recovered URI: " + entry.uri + ", ONDISK: " + entry.onDisk);
		}
	}

	private void performBadFileCheck() {
		String onDiskColumn = ImagesTable.Columns.ON_DISK.getColumnName();
		Cursor cursor = mImagesTable.selectFromTable(mDatabase.getReadableDatabase(), onDiskColumn + "=?", new String[] { "0" });

		while (cursor.moveToNext()) {
			Log.d("JAMIE", "Deleting an item. URI: " + cursor.getString(cursor.getColumnIndex(ImagesTable.Columns.URI.getColumnName())));
			int columnIndex = cursor.getColumnIndex(ImagesTable.Columns.ID.getColumnName());
			int badImageEntryId = cursor.getInt(columnIndex);
			String filename = Integer.toString(badImageEntryId);

			mObserver.onBadJournalEntry(filename);
			deleteRow(badImageEntryId);
		}
	}

	private void deleteRow(int badImageEntryId) {
		mImagesTable.delete(mDatabase.getWritableDatabase(), ImagesTable.Columns.ID.getColumnName() + "=?", new String[] { Integer.toString(badImageEntryId) });
	}

	/**
	 * FOR TESTING PURPOSES ONLY! WILL SHUT DOWN DATABASE ACCESS.
	 */
	void close() {
		mDatabase.close();
	}

	interface ImageSystemDatabaseObserver {
		void onBadJournalEntry(String filename);

		void onDetailsRequired(String filename);
	}
}
