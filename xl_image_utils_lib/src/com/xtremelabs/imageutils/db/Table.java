package com.xtremelabs.imageutils.db;

import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class Table<T> {
	/**
	 * Create
	 * <table>
	 * <columns> Unique <columns>
	 */
	static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s ( %s  UNIQUE ( %s ) ON CONFLICT REPLACE);";
	static final String CLEAR_TABLE = "DELETE FROM %s";
	static final String DROP_TABLE = "DROP TABLE IF EXISTS %s";

	public abstract String[] getColumns();

	public abstract String getTableName();

	protected abstract String getColumnsForCreation();

	protected abstract String getUniqueString();

	protected abstract ContentValues toContentValues(T item);

	public void onCreate(SQLiteDatabase db) {
		String query = String.format(CREATE_TABLE, getTableName(), getColumnsForCreation(), getUniqueString());
		db.execSQL(query);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onDropTable(db);
		onCreate(db);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onDropTable(db);
		onCreate(db);
	}

	public void onClear(SQLiteDatabase db) {
		String query = String.format(CLEAR_TABLE, getTableName());
		db.execSQL(query);
	}

	public void onDropTable(SQLiteDatabase db) {
		String query = String.format(DROP_TABLE, getTableName());
		db.execSQL(query);
	}

	public Cursor selectAllFromTable(SQLiteDatabase db) {
		return db.query(getTableName(), getColumns(), null, null, null, null, null);
	}

	public Cursor selectFromTable(SQLiteDatabase db, String selection, String[] selectArgs) {
		return db.query(getTableName(), getColumns(), selection, selectArgs, null, null, null);
	}

	public void insert(List<T> items, SQLiteDatabase db) {
		for (T item : items) {
			insert(item, db);
		}
	}

	public void insert(T item, SQLiteDatabase db) {
		ContentValues values = toContentValues(item);
		db.insertWithOnConflict(getTableName(), null, values, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void update(T item, String where, String[] whereArgs, SQLiteDatabase db) {
		ContentValues values = toContentValues(item);
		db.updateWithOnConflict(getTableName(), values, where, whereArgs, SQLiteDatabase.CONFLICT_REPLACE);
	}

	public void delete(SQLiteDatabase db, String whereClause, String[] whereArgs) {
		db.delete(getTableName(), whereClause, whereArgs);
	}
}
