package io.github.voidc.np4ilr.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import io.github.voidc.np4ilr.InternetUtils;
import io.github.voidc.np4ilr.R;
import io.github.voidc.np4ilr.RadioService;

/**
 * An activity representing a single Channel detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ChannelListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ChannelDetailFragment}.
 */
public class ChannelDetailActivity extends AppCompatActivity {
    private int channelId;
    private Toolbar toolbar;

    /*
    TODO:
    - make toolbar transparent
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_detail);

        toolbar = (Toolbar) findViewById(R.id.toolbar_channel_detail);
        setSupportActionBar(toolbar);
        //toolbar.getBackground().setAlpha(0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (savedInstanceState == null) {
            channelId = getIntent().getIntExtra(RadioService.ARG_CHANNEL_ID, 1);
            setTitle(InternetUtils.getChannelById(channelId).getFullName());
            Bundle arguments = new Bundle();
            arguments.putInt(RadioService.ARG_CHANNEL_ID, channelId);
            ChannelDetailFragment fragment = new ChannelDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.framelayout_channel_detail, fragment)
                    .commit();
        }
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
        getMenuInflater().inflate(R.menu.menu_channel_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_playlist) {
            Intent playlistIntent = new Intent(this, PlaylistActivity.class);
            playlistIntent.putExtra(RadioService.ARG_CHANNEL_ID, channelId);
            startActivity(playlistIntent);
            return true;
        }
        if (id == android.R.id.home) {
            NavUtils.navigateUpTo(this, new Intent(this, ChannelListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
