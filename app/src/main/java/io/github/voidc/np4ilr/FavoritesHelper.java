package io.github.voidc.np4ilr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import io.github.voidc.np4ilr.model.ILRTrack;

public class FavoritesHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "ILRPlayer.db";
    public static final int DATABASE_VERSION = 6;
    public static final String TABLE_NAME = "favorites";
    public static final String COL_TRACK_TITLE = "title";
    public static final String COL_TRACK_ARTIST = "artist";
    public static final String COL_TRACK_COVER = "cover";
    public static final String COL_TRACK_CHANNEL = "channel";

    private static final String SQL_MATCH_TRACK = COL_TRACK_TITLE + " = ? AND " + COL_TRACK_ARTIST + " = ?";

    private static FavoritesHelper instance;

    private FavoritesHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public static FavoritesHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new FavoritesHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_NAME + "(" +
                        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COL_TRACK_TITLE + " VARCHAR(200)," +
                        COL_TRACK_ARTIST + " VARCHAR(200)," +
                        COL_TRACK_COVER + " VARCHAR(300)," +
                        COL_TRACK_CHANNEL + " VARCHAR(100)" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private void insert(SQLiteDatabase db, ILRTrack track, String channelName) {
        ContentValues entry = new ContentValues();
        entry.put(COL_TRACK_TITLE, track.getTitle());
        entry.put(COL_TRACK_ARTIST, track.getArtist());
        entry.put(COL_TRACK_COVER, track.getImageURI());
        entry.put(COL_TRACK_CHANNEL, channelName);
        db.insert(TABLE_NAME, null, entry);
    }

    public boolean favorite(ILRTrack track, String channelName) {
        SQLiteDatabase db = getWritableDatabase();
        if (isFavorited(track)) {
            db.delete(TABLE_NAME, SQL_MATCH_TRACK, new String[]{track.getTitle(), track.getArtist()});
            db.close();
            return false;
        } else {
            insert(db, track, channelName);
            //db.close();
            return true;
        }
    }

    public Cursor getCursor() {
        SQLiteDatabase db = getWritableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public boolean isFavorited(ILRTrack track) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + SQL_MATCH_TRACK,
                new String[]{track.getTitle(), track.getArtist()});
        boolean fav = c.getCount() > 0;
        c.close();
        //db.close();
        return fav;
    }

    public void clearFavorites() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }
}
