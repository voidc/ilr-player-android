package io.github.voidc.np4ilr.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jorgecastilloprz.FABProgressCircle;

import java.io.IOException;

import io.github.voidc.np4ilr.FavoritesHelper;
import io.github.voidc.np4ilr.InternetUtils;
import io.github.voidc.np4ilr.R;
import io.github.voidc.np4ilr.RadioService;
import io.github.voidc.np4ilr.model.ILRChannel;
import io.github.voidc.np4ilr.model.ILRTrack;
import jp.wasabeef.blurry.Blurry;

/**
 * A fragment representing a single Channel detail screen.
 * This fragment is either contained in a {@link ChannelListActivity}
 * in two-pane mode (on tablets) or a {@link ChannelDetailActivity}
 * on handsets.
 */
public class ChannelDetailFragment extends Fragment implements ServiceConnection, RadioService.RadioServiceListener {
    /**
     * The dummy content this fragment is presenting.
     */
    private ILRChannel channel;
    private ILRTrack currentlyPlaying;
    private boolean playing = false;
    private boolean preparing = false;
    private RadioService playback;
    private boolean connectedToService;

    private FloatingActionButton fabPlay;
    private FABProgressCircle fabProgress;
    private TextView textArtist;
    private TextView textTitle;
    private ImageView imageCover;
    private ImageView imageCoverSmall;
    private ImageButton btnShare;
    private ImageButton btnFavorite;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChannelDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(RadioService.ARG_CHANNEL_ID)) {
            channel = InternetUtils.getChannelById(getArguments().getInt(RadioService.ARG_CHANNEL_ID));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (connectedToService) {
            playback.setRadioListener(null);
            getActivity().unbindService(this);
            connectedToService = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_channel_detail, container, false);

        fabPlay = (FloatingActionButton) rootView.findViewById(R.id.play_button);
        fabProgress = (FABProgressCircle) rootView.findViewById(R.id.prepare_progress);
        textArtist = (TextView) rootView.findViewById(R.id.track_artist);
        textTitle = (TextView) rootView.findViewById(R.id.track_title);
        imageCover = (ImageView) rootView.findViewById(R.id.track_cover);
        imageCoverSmall = (ImageView) rootView.findViewById(R.id.track_cover_small);
        btnShare = (ImageButton) rootView.findViewById(R.id.btn_share);
        btnFavorite = (ImageButton) rootView.findViewById(R.id.btn_favorite);

        fabPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFabClicked();
            }
        });
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShareButtonClicked();
            }
        });
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFavButtonClicked();
            }
        });
        imageCoverSmall.setLongClickable(true);
        imageCoverSmall.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveCoverArt();
                return true;
            }
        });


        updateCurrentlyPlaying();

        Intent bindIntent = new Intent(getContext(), RadioService.class);
        bindIntent.putExtra(RadioService.ARG_CHANNEL_ID, channel.getId());
        getActivity().bindService(bindIntent, this, Context.BIND_AUTO_CREATE);

        return rootView;
    }

    private void saveCoverArt() {
        int permission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(imageCoverSmall, R.string.msg_save_cover, Snackbar.LENGTH_LONG)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MediaStore.Images.Media.insertImage(getActivity().getContentResolver(),
                                    ((BitmapDrawable) imageCoverSmall.getDrawable()).getBitmap(),
                                    channel.getFullName(), currentlyPlaying.toString());
                        }
                    }).setActionTextColor(Color.WHITE).show();
        }
    }

    private void onShareButtonClicked() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, channel.getFullName());
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, currentlyPlaying.toString());
        startActivity(shareIntent);
    }

    private void onFavButtonClicked() {
        if (currentlyPlaying != null) {
            boolean favorited = FavoritesHelper.getInstance(this.getContext()).favorite(currentlyPlaying, channel.getName());
            setFavorited(favorited);
        }
    }

    private void setPlaying(boolean playing) {
        if (playing != this.playing) {
            this.playing = playing;
            fabPlay.setImageResource(playing ? R.drawable.ic_stop : R.drawable.ic_play);
        }
    }

    private void setPreparing(boolean preparing) {
        if (preparing != this.preparing) {
            this.preparing = preparing;
            if (preparing)
                fabProgress.show();
            else
                fabProgress.hide();
        }
    }

    private void onFabClicked() {
        if (!connectedToService)
            return;

        Intent startServiceIntent = new Intent(getActivity(), RadioService.class);
        startServiceIntent.setAction(playing ? RadioService.ACTION_STOP : RadioService.ACTION_PLAY);
        startServiceIntent.putExtra(RadioService.ARG_CHANNEL_ID, channel.getId());

        getActivity().startService(startServiceIntent);
    }

    private void updateCurrentlyPlaying() {
        new AsyncTask<Object, Integer, ILRTrack>() {
            @Override
            protected ILRTrack doInBackground(Object... params) {
                ILRTrack track = null;
                try {
                    track = InternetUtils.fetchTrack(channel);
                } catch (IOException ioe) {
                    cancel(true);
                }
                return track;
            }

            @Override
            protected void onCancelled() {
                Snackbar.make(getView(), R.string.msg_connection_error, Snackbar.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(ILRTrack track) {
                setCurrentlyPlaying(track);
            }
        }.execute(null, null);
    }

    private void setCurrentlyPlaying(ILRTrack track) {
        currentlyPlaying = track;

        textTitle.setText(track.getTitle());
        textArtist.setText(track.getArtist());

        setFavorited(FavoritesHelper.getInstance(getContext()).isFavorited(track));

        downloadCoverArt();
    }

    @Override
    public void setFavorited(boolean favorited) {
        int icon = favorited ? R.drawable.ic_favorite : R.drawable.ic_favorite_outline;
        btnFavorite.setImageDrawable(ContextCompat.getDrawable(getContext(), icon));
    }

    @Override
    public ILRTrack getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    private void downloadCoverArt() {
        new AsyncTask<Object, Integer, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... params) {
                Bitmap cover = null;
                try {
                    cover = InternetUtils.fetchCoverArt(currentlyPlaying);
                } catch (IOException ioe) {
                    cancel(true);
                }
                return cover;
            }

            @Override
            protected void onCancelled() {
                Snackbar.make(getView(), R.string.msg_connection_error, Snackbar.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(Bitmap cover) {
                imageCover.setImageBitmap(cover);
                Blurry.with(getContext()).radius(25).capture(imageCover).into(imageCover);
                imageCoverSmall.setImageBitmap(cover);
            }
        }.execute(null, null);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        connectedToService = true;
        playback = ((RadioService.RadioServiceBinder) service).getService();
        playback.setRadioListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        connectedToService = false;
    }

    @Override
    public void onTrackChanged(ILRTrack newTrack) {
        setCurrentlyPlaying(newTrack);
    }

    @Override
    public ILRChannel getChannel() {
        return channel;
    }

    @Override
    public void onStateChanged(RadioService.RadioState newState) {
        switch (newState) {
            case STOPPED:
                setPlaying(false);
                setPreparing(false);
                break;
            case PREPARING:
                setPlaying(true);
                setPreparing(true);
                break;
            case PLAYING:
            case PAUSED:
                setPlaying(true);
                setPreparing(false);
        }
    }
}
