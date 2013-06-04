package com.xtremelabs.imageutils.db;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

public class Database extends SQLiteOpenHelper {

	private final List<Table<?>> mTables;

	public Database(Context context, CursorFactory factory, List<Table<?>> tables) {
		super(context, DatabaseConfig.NAME, factory, DatabaseConfig.VERSION);
		if (tables == null)
			throw new IllegalArgumentException("The list of tables cannot be null!");
		mTables = tables;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Database(Context context, CursorFactory factory, List<Table<?>> tables, DatabaseErrorHandler errorHandler) {
		super(context, DatabaseConfig.NAME, factory, DatabaseConfig.VERSION, errorHandler);
		if (tables == null)
			throw new IllegalArgumentException("The list of tables cannot be null!");
		mTables = tables;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for (Table<?> table : mTables) {
			table.onCreate(db);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (Table<?> table : mTables) {
			table.onUpgrade(db, oldVersion, newVersion);
		}
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (Table<?> table : mTables) {
			table.onDowngrade(db, oldVersion, newVersion);
		}
	}

	public void clear() {
		SQLiteDatabase db = getWritableDatabase();
		for (Table<?> table : mTables) {
			table.onClear(db);
		}
	}

	public void reset() {
		for (Table<?> table : mTables) {
			SQLiteDatabase db = getWritableDatabase();
			table.onDropTable(db);
			table.onCreate(db);
		}
	}
}
