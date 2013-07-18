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

	public Cursor selectFromTable(SQLiteDatabase db, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return db.query(getTableName(), columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	public Cursor selectFromTable(SQLiteDatabase db, String selection, String[] selectArgs) {
		return db.query(getTableName(), getColumns(), selection, selectArgs, null, null, null);
	}

	public void insert(SQLiteDatabase db, List<T> items) {
		for (T item : items) {
			insert(db, item);
		}
	}

	/**
	 * {@link SQLiteDatabase#insert(String, String, ContentValues)}
	 */
	public long insert(SQLiteDatabase db, T item) {
		ContentValues values = toContentValues(item);
		return db.insert(getTableName(), null, values);
	}

	/**
	 * {@link SQLiteDatabase#update(String, ContentValues, String, String[])}
	 */
	public int update(SQLiteDatabase db, T item, String where, String[] whereArgs) {
		ContentValues values = toContentValues(item);
		return db.update(getTableName(), values, where, whereArgs);
	}

	/**
	 * {@link SQLiteDatabase#delete(String, String, String[])}
	 */
	public int delete(SQLiteDatabase db, String whereClause, String[] whereArgs) {
		return db.delete(getTableName(), whereClause, whereArgs);
	}
}
