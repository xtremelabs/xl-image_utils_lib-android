package com.xtremelabs.imageutils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.xtremelabs.imageutils.db.Table;

class ImagesTable extends Table<ImageEntry> {

	enum Columns {
		ID(0), URI(1), ON_DISK(2), CREATION_TIME(3), LAST_ACCESSED_TIME(4), SIZE_X(5), SIZE_Y(6);

		private final int mIndex;

		private Columns(int index) {
			mIndex = index;
		}

		String getColumnName() {
			return mColumns[mIndex];
		}
	}

	private static class InstanceHolder {
		private static ImagesTable sInstance = new ImagesTable();
	}

	private final static String[] mColumns = { "_id", "uri", "on_disk", "creation_time", "last_accessed_time", "size_x", "size_y" };
	private final String mTableName = "images";

	static ImagesTable getInstance() {
		return InstanceHolder.sInstance;
	}

	@Override
	public String[] getColumns() {
		return mColumns;
	}

	@Override
	public String getTableName() {
		return mTableName;
	}

	@Override
	public void insert(ImageEntry item, SQLiteDatabase db) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(mColumns[1], item.uri);
		contentValues.put(mColumns[2], item.onDisk);
		contentValues.put(mColumns[3], item.creationTime);
		contentValues.put(mColumns[4], item.lastAccessedTime);
		contentValues.put(mColumns[5], item.sizeX);
		contentValues.put(mColumns[6], item.sizeY);
		Log.d("JAMIE", "Insertion result: " + db.insertWithOnConflict(mTableName, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE));
	}

	@Override
	protected String getColumnsForCreation() {
		StringBuilder builder = new StringBuilder();
		builder.append(mColumns[0] + " INTEGER PRIMARY KEY AUTOINCREMENT, ");
		builder.append(mColumns[1] + " TEXT, ");
		builder.append(mColumns[2] + " INTEGER, ");
		builder.append(mColumns[3] + " BIGINT, ");
		builder.append(mColumns[4] + " BIGINT, ");
		builder.append(mColumns[5] + " INTEGER, ");
		builder.append(mColumns[6] + " INTEGER, ");
		return builder.toString();
	}

	@Override
	protected String getUniqueString() {
		return mColumns[0] + ", " + mColumns[1];
	}

	@Override
	protected ContentValues toContentValues(ImageEntry item) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(mColumns[0], item.id);
		contentValues.put(mColumns[1], item.uri);
		contentValues.put(mColumns[2], item.onDisk);
		contentValues.put(mColumns[3], item.creationTime);
		contentValues.put(mColumns[4], item.lastAccessedTime);
		contentValues.put(mColumns[5], item.sizeX);
		contentValues.put(mColumns[6], item.sizeY);
		return contentValues;
	}

	public ImageEntry getEntry(String uri, SQLiteDatabase db) {
		ImageEntry entry = null;

		Cursor cursor = selectFromTable(db, Columns.URI.getColumnName() + "=?", new String[] { uri });
		if (cursor.getCount() == 1) {
			entry = new ImageEntry();
			entry.id = cursor.getLong(cursor.getColumnIndex(Columns.ID.getColumnName()));
			entry.uri = cursor.getString(cursor.getColumnIndex(Columns.URI.getColumnName()));
			entry.creationTime = cursor.getLong(cursor.getColumnIndex(Columns.CREATION_TIME.getColumnName()));
			entry.lastAccessedTime = cursor.getLong(cursor.getColumnIndex(Columns.LAST_ACCESSED_TIME.getColumnName()));
			entry.onDisk = cursor.getInt(cursor.getColumnIndex(Columns.ON_DISK.getColumnName())) == 1;
			entry.sizeX = cursor.getInt(cursor.getColumnIndex(Columns.SIZE_X.getColumnName()));
			entry.sizeY = cursor.getInt(cursor.getColumnIndex(Columns.SIZE_Y.getColumnName()));
		}
		return entry;
	}
}
