package io.github.voidc.np4ilr.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import io.github.voidc.np4ilr.FavoritesHelper;
import io.github.voidc.np4ilr.R;

public class FavoritesActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private ListView listView;
    private FavoritesAdapter adapter;

    /*
    TODO:
    - sort by channel
    - for fav and playlist: click on item to open dialog showing track details
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new FavoritesAdapter(FavoritesHelper.getInstance(this).getCursor());
        listView = (ListView) findViewById(R.id.list_favorites);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_favorites, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, ChannelListActivity.class));
            return true;
        }
        if (id == R.id.action_clear_favorites) {
            FavoritesHelper.getInstance(this).clearFavorites();
            refresh();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = adapter.getCursor();
        c.moveToPosition(position);
        String title = c.getString(c.getColumnIndex(FavoritesHelper.COL_TRACK_TITLE));
        String artist = c.getString(c.getColumnIndex(FavoritesHelper.COL_TRACK_ARTIST));
        String cover = c.getString(c.getColumnIndex(FavoritesHelper.COL_TRACK_COVER));
        String channel = c.getString(c.getColumnIndex(FavoritesHelper.COL_TRACK_CHANNEL));

        TrackDetailsDialog dialog = new TrackDetailsDialog();

        Bundle args = new Bundle();
        args.putString(TrackDetailsDialog.ARG_TRACK_TITLE, title);
        args.putString(TrackDetailsDialog.ARG_TRACK_ARTIST, artist);
        args.putString(TrackDetailsDialog.ARG_TRACK_COVER, cover);
        args.putString(TrackDetailsDialog.ARG_TRACK_CHANNEL_NAME, channel);
        dialog.setArguments(args);

        dialog.setOnFavoriteChangedListener(new TrackDetailsDialog.OnFavoriteChangedListener() {
            @Override
            public void onFavoriteChanged() { //broken :(
                refresh();
            }
        });


        dialog.show(getSupportFragmentManager(), TrackDetailsDialog.DIALOG_TAG);
    }

    private void refresh() {
        adapter.swapCursor(FavoritesHelper.getInstance(this).getCursor());
    }

    public class FavoritesAdapter extends CursorAdapter {

        public FavoritesAdapter(Cursor c) {
            super(FavoritesActivity.this, c, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.listitem_favorites, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView textTitle = (TextView) view.findViewById(R.id.text_fav_item_title);
            textTitle.setText(cursor.getString(cursor.getColumnIndex(FavoritesHelper.COL_TRACK_TITLE)));

            TextView textArtist = (TextView) view.findViewById(R.id.text_fav_item_artist);
            textArtist.setText(cursor.getString(cursor.getColumnIndex(FavoritesHelper.COL_TRACK_ARTIST)));

            TextView textChannel = (TextView) view.findViewById(R.id.text_fav_item_channel);
            textChannel.setText(cursor.getString(cursor.getColumnIndex(FavoritesHelper.COL_TRACK_CHANNEL)));
        }
    }
}
