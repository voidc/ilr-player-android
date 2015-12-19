package io.github.voidc.np4ilr.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.github.voidc.np4ilr.InternetUtils;
import io.github.voidc.np4ilr.R;
import io.github.voidc.np4ilr.RadioService;
import io.github.voidc.np4ilr.model.ILRTrack;

public class PlaylistActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private int channelId;
    private ListView listView;
    private List<ILRTrack> playlist = new ArrayList<>();
    private PlaylistAdapter adapter;
    private final Calendar cal = Calendar.getInstance();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            channelId = getIntent().getIntExtra(RadioService.ARG_CHANNEL_ID, 1);

        setContentView(R.layout.activity_playlist);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new PlaylistAdapter(playlist);

        listView = (ListView) findViewById(R.id.list_playlist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        fetchPlaylist();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(RadioService.ARG_CHANNEL_ID, channelId);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        channelId = savedInstanceState.getInt(RadioService.ARG_CHANNEL_ID, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_playlist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_date) {
            DatePickerDialog dpd = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    cal.set(year, monthOfYear, dayOfMonth);
                    fetchPlaylist();
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.show();
            return true;
        }
        if (id == android.R.id.home) {
            Intent channelIntent = new Intent(this, ChannelDetailActivity.class);
            channelIntent.putExtra(RadioService.ARG_CHANNEL_ID, channelId);
            NavUtils.navigateUpTo(this, channelIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ILRTrack track = adapter.getItem(position);
        TrackDetailsDialog dialog = new TrackDetailsDialog();

        Bundle args = new Bundle();
        args.putString(TrackDetailsDialog.ARG_TRACK_TITLE, track.getTitle());
        args.putString(TrackDetailsDialog.ARG_TRACK_ARTIST, track.getArtist());
        args.putString(TrackDetailsDialog.ARG_TRACK_COVER, track.getImageURI());
        args.putString(TrackDetailsDialog.ARG_TRACK_CHANNEL_NAME, InternetUtils.getChannelById(channelId).getName());
        dialog.setArguments(args);

        dialog.show(getSupportFragmentManager(), TrackDetailsDialog.DIALOG_TAG);
    }

    public class PlaylistAdapter extends ArrayAdapter<ILRTrack> {

        public PlaylistAdapter(List<ILRTrack> playlist) {
            super(PlaylistActivity.this, R.layout.listitem_playlist, playlist);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ILRTrack track = getItem(position);

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.listitem_playlist, parent, false);
            }

            TextView textDate = (TextView) view.findViewById(R.id.text_playlist_item_time);
            textDate.setText(timeFormat.format(track.getTimestamp()));

            TextView textTitle = (TextView) view.findViewById(R.id.text_playlist_item_title);
            textTitle.setText(track.getTitle());

            TextView textArtist = (TextView) view.findViewById(R.id.text_playlist_item_artist);
            textArtist.setText(track.getArtist());

            return view;
        }
    }

    private void fetchPlaylist() {
        new AsyncTask<Object, Integer, List<ILRTrack>>() {

            @Override
            protected List<ILRTrack> doInBackground(Object... params) {
                List<ILRTrack> fetched = null;
                try {
                    fetched = InternetUtils.fetchPlaylist(InternetUtils.getChannelById(channelId), cal.getTime());
                } catch (IOException e) {
                    cancel(true);
                }
                return fetched;
            }

            @Override
            protected void onCancelled() {
                Snackbar.make(listView, R.string.msg_connection_error, Snackbar.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(List<ILRTrack> ilrTracks) {
                playlist.clear();
                playlist.addAll(ilrTracks);
                adapter.notifyDataSetChanged();
            }
        }.execute(null, null);
    }
}
