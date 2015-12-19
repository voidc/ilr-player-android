package io.github.voidc.np4ilr.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

import io.github.voidc.np4ilr.FavoritesHelper;
import io.github.voidc.np4ilr.InternetUtils;
import io.github.voidc.np4ilr.R;
import io.github.voidc.np4ilr.model.ILRChannel;
import io.github.voidc.np4ilr.model.ILRTrack;

public class TrackDetailsDialog extends AppCompatDialogFragment {
    private ILRTrack track;
    private String channelName;

    private OnFavoriteChangedListener favListener;
    private boolean favChanged = false;

    private ImageView imageCover;
    private TextView textTitle;
    private TextView textArtist;
    private ImageButton btnShare;
    private ImageButton btnFavorite;

    public static final String ARG_TRACK_TITLE = "title";
    public static final String ARG_TRACK_ARTIST = "artist";
    public static final String ARG_TRACK_COVER = "cover";
    public static final String ARG_TRACK_CHANNEL_NAME = "channelname";

    public static final String DIALOG_TAG = "ILRTrackDetailsDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /*if(savedInstanceState != null) {
            String title = savedInstanceState.getString(ARG_TRACK_TITLE);
            String artist = savedInstanceState.getString(ARG_TRACK_ARTIST);
            String cover = savedInstanceState.getString(ARG_TRACK_COVER);
            track = new ILRTrack(artist, title, cover);
        }*/

        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString(ARG_TRACK_TITLE);
            String artist = args.getString(ARG_TRACK_ARTIST);
            String cover = args.getString(ARG_TRACK_COVER);
            channelName = args.getString(ARG_TRACK_CHANNEL_NAME);
            track = new ILRTrack(artist, title, cover);
        }

        View rootView = getActivity().getLayoutInflater().inflate(R.layout.dialog_trackdetails, null);
        imageCover = (ImageView) rootView.findViewById(R.id.dialog_track_image_cover);
        textTitle = (TextView) rootView.findViewById(R.id.dialog_track_text_title);
        textArtist = (TextView) rootView.findViewById(R.id.dialog_track_text_artist);
        btnShare = (ImageButton) rootView.findViewById(R.id.dialog_track_btn_share);
        btnFavorite = (ImageButton) rootView.findViewById(R.id.dialog_track_btn_fav);

        textTitle.setText(track.getTitle());
        textArtist.setText(track.getArtist());
        setFavorited(FavoritesHelper.getInstance(getContext()).isFavorited(track));
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
        fetchCoverImage();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(rootView);
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (favListener != null && favChanged) {
            favListener.onFavoriteChanged();
        }
    }

    public void setOnFavoriteChangedListener(OnFavoriteChangedListener listener) {
        this.favListener = listener;
    }

    private void onFavButtonClicked() {
        favChanged = !favChanged;
        boolean favorited = FavoritesHelper.getInstance(this.getContext()).favorite(track, channelName);
        setFavorited(favorited);
    }

    private void onShareButtonClicked() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ILRChannel.convertToFullName(channelName));
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, track.toString());
        startActivity(shareIntent);
    }

    private void setFavorited(boolean favorited) {
        int icon = favorited ? R.drawable.ic_favorite : R.drawable.ic_favorite_outline;
        btnFavorite.setImageDrawable(ContextCompat.getDrawable(getContext(), icon));
    }

    private void fetchCoverImage() {
        new AsyncTask<Object, Integer, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Object... params) {
                Bitmap cover = null;
                try {
                    cover = InternetUtils.fetchCoverArt(track);
                } catch (IOException ioe) {
                    cancel(true);
                }
                return cover;
            }

            @Override
            protected void onCancelled() {
                Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.msg_connection_error, Snackbar.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(Bitmap cover) {
                imageCover.setImageBitmap(cover);
            }
        }.execute(null, null);
    }

    public interface OnFavoriteChangedListener {
        void onFavoriteChanged();
    }

}
