package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.xtremelabs.imageutils.ImagesTable.Columns;
import com.xtremelabs.imageutils.db.Database;
import com.xtremelabs.imageutils.db.Table;

class ImageSystemDatabase { // TODO should this be implementing an interface?
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

		mDatabase = new Database(context, tables);
		populateCache();
		performBadFileCheck();
	}

	public void beginWrite(String uri) {
		ImageEntry entry = new ImageEntry();
		entry.uri = uri;
		entry.lastAccessedTime = System.currentTimeMillis();
		mImagesTable.insert(mDatabase.getWritableDatabase(), entry);
		mCache.putEntry(entry);
	}

	public void endWrite(String uri) { // TODO what happens if try to endWrite with corresponding entry in cache?
		ImageEntry entry = mCache.getEntry(uri);
		entry.onDisk = true;
		mImagesTable.insert(mDatabase.getWritableDatabase(), entry);
	}

	public void bump(String uri) {
		ImageEntry entry = mCache.getEntry(uri);
		entry.lastAccessedTime = System.currentTimeMillis();
		mImagesTable.insert(mDatabase.getWritableDatabase(), entry);
	}

	public void writeFailed(String uri) {
		deleteEntry(uri);
	}

	public void deleteEntry(String uri) {
		mCache.removeEntry(uri);
		deleteRow(uri);
	}

	ImageEntry getEntry(String uri) {
		return mCache.getEntry(uri);
	}

	long getTotalFileSize() {
		String[] columns = new String[] { "SUM(" + Columns.FILE_SIZE.getName() + ")" };
		Cursor cursor = mImagesTable.selectFromTable(mDatabase.getReadableDatabase(), columns, null, null, null, null, null, null);
		cursor.moveToFirst();
		long total = cursor.getLong(0);
		cursor.close();
		return total;
	}

	void clear() {
		mCache.clear();
		mImagesTable.onClear(mDatabase.getWritableDatabase());
	}

	void submitDetails(String uri, Dimensions dimensions, long fileSize) {
		ImageEntry entry = mCache.getEntry(uri);
		entry.sizeX = dimensions.width;
		entry.sizeY = dimensions.height;
		entry.fileSize = fileSize;
		mImagesTable.insert(mDatabase.getWritableDatabase(), entry);
	}

	private void populateCache() {
		Cursor allEntries = mImagesTable.selectAllFromTable(mDatabase.getReadableDatabase());
		Log.d("JAMIE", "Entries in database: " + allEntries.getCount());
		while (allEntries.moveToNext()) {
			ImageEntry entry = ImagesTable.getEntryFromCursor(allEntries);
			mCache.putEntry(entry);
			Log.d("JAMIE", "Recovered URI: " + entry.uri + ", ONDISK: " + entry.onDisk);
		}
	}

	private void performBadFileCheck() {
		String onDiskColumn = ImagesTable.Columns.ON_DISK.getName();
		Cursor cursor = mImagesTable.selectFromTable(mDatabase.getReadableDatabase(), onDiskColumn + "=?", new String[] { "0" });

		while (cursor.moveToNext()) {
			ImageEntry entry = ImagesTable.getEntryFromCursor(cursor);
			Log.d("JAMIE", "Deleting an item. URI: " + entry.uri);
			deleteEntry(entry.uri);
			mObserver.onBadJournalEntry(entry);
		}
		cursor.close();
	}

	private void deleteRow(String uri) {
		String whereClause = ImagesTable.Columns.URI.getName() + "=?";
		String[] whereArgs = new String[] { uri };
		mImagesTable.delete(mDatabase.getWritableDatabase(), whereClause, whereArgs);
	}

	/**
	 * FOR TESTING PURPOSES ONLY! WILL SHUT DOWN DATABASE ACCESS.
	 */
	void close() {
		mDatabase.close();
	}

	interface ImageSystemDatabaseObserver {
		void onBadJournalEntry(ImageEntry entry);

		void onDetailsRequired(String filename);
	}

	public ImageEntry removeLRU() {
		String orderBy = Columns.LAST_ACCESSED_TIME + " ASC";
		String selection = Columns.ON_DISK.getName() + "=?";
		Cursor cursor = mImagesTable.selectFromTable(mDatabase.getReadableDatabase(), null, selection, new String[] { "1" }, null, null, orderBy, Integer.toString(1));

		ImageEntry entry = null;
		if (cursor.moveToFirst()) {
			entry = ImagesTable.getEntryFromCursor(cursor);
			deleteEntry(entry.uri);
		}

		return entry;
	}

}
