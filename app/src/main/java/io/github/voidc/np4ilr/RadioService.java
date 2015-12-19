package io.github.voidc.np4ilr;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.github.voidc.np4ilr.model.ILRChannel;
import io.github.voidc.np4ilr.model.ILRTrack;
import io.github.voidc.np4ilr.ui.ChannelDetailActivity;

public class RadioService extends Service implements MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
    public static final String ACTION_PLAY = "io.github.voidc.np4ilr.PLAY";
    public static final String ACTION_STOP = "io.github.voidc.np4ilr.STOP";
    public static final String ACTION_PAUSE = "io.github.voidc.np4ilr.PAUSE";
    public static final String ACTION_FAVORITE = "io.github.voidc.np4ilr.FAVORITE";

    public static final String ARG_CHANNEL_ID = "channel";

    public static final String TAG_WIFILOCK = "ILR_WifiLock";
    public static final int NOTIFICATION_ID = 8525;

    private MediaPlayer mp = null;

    private ILRChannel channel;
    private ILRTrack currentlyPlaying;
    private RadioState state;

    private boolean polling = false;
    private boolean shouldSwitch = false;

    private RadioServiceListener listener;

    private WifiManager.WifiLock wifiLock;
    private Notification notification;
    private AudioManager audioManager;
    private Timer pollTrackTimer;

    /*
    TODO:
    - implement RemoteControlClient
    - FAB unresponsive in case of switching tracks while preparing
    - notification color doesn't update after channel switched
     */

    public RadioService() {
    }

    public void setRadioListener(RadioServiceListener listener) {
        Log.d("ListenerConnected", listener != null ? listener.getChannel().toString() : "null");
        this.listener = listener;

        if (listener != null && channel.equals(listener.getChannel())) //if you bind with the chd. which is currently playing the chd. should be adjusted accordingly (fab stop)
            listener.onStateChanged(state);

        if (polling == false) //if the service is new polling has to be started
            pollTrackTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateCurrentlyPlaying();
                }
            }, 0, 5000);
    }

    @Override
    public void onCreate() {
        state = RadioState.STOPPED; //inititial value, no need to call setter
        initMediaPlayer();
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, TAG_WIFILOCK);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        pollTrackTimer = new Timer();
    }

    private void initMediaPlayer() {
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(this);
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public void onDestroy() {
        if (mp != null) {
            mp.release();
        }
        audioManager.abandonAudioFocus(this);

        if (wifiLock.isHeld())
            wifiLock.release();

        polling = false;
        pollTrackTimer.cancel();
        stopForeground(true);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (getState() == RadioState.STOPPED) { //if nothing is playing and no chd is bound the service is no longer needed
            stopSelf();
        }
        return false;
    }

    public RadioState getState() {
        return state;
    }

    private void setState(RadioState state) {
        if (this.state == state)
            return;

        switch (this.state) {
            case STOPPED:
                if (state != RadioState.PREPARING)
                    throw new IllegalStateException();
                break;
            case PAUSED:
                if (state == RadioState.PLAYING)
                    throw new IllegalStateException();
                break;
        }

        this.state = state;
        if (listener != null)
            listener.onStateChanged(state);
    }

    private void createNotification() {
        if (getState() == RadioState.STOPPED) return;

        Intent channelIntent = new Intent(getApplicationContext(), ChannelDetailActivity.class);
        channelIntent.putExtra(RadioService.ARG_CHANNEL_ID, channel.getId());
        PendingIntent channelPi = PendingIntent.getActivity(getApplicationContext(), 0, channelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String action = getState() == RadioState.PAUSED ? ACTION_PLAY : ACTION_PAUSE;
        Intent playbackIntent = new Intent(action);
        playbackIntent.setComponent(new ComponentName(this, RadioService.class));
        PendingIntent playbackPendingIntent = PendingIntent.getService(this, 0, playbackIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent favIntent = new Intent(ACTION_FAVORITE);
        favIntent.setComponent(new ComponentName(this, RadioService.class));
        PendingIntent favPandingIntent = PendingIntent.getService(this, 0, favIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent stopIntent = new Intent(ACTION_STOP);
        stopIntent.setComponent(new ComponentName(this, RadioService.class));
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        int playbackIcon = getState() == RadioState.PAUSED ? R.drawable.ic_play : R.drawable.ic_pause;

        boolean favorited = FavoritesHelper.getInstance(this).isFavorited(currentlyPlaying);
        int favIcon = favorited ? R.drawable.ic_favorite : R.drawable.ic_favorite_outline;

        Log.d("RadioNotification", channel.toString() + ", " + getState().toString());

        notification = new NotificationCompat.Builder(this)
                .setStyle(new NotificationCompat.MediaStyle())
                .setContentTitle(currentlyPlaying.getTitle())
                .setContentText(currentlyPlaying.getArtist())
                .setSmallIcon(R.drawable.ilr_logo)
                .setLargeIcon(InternetUtils.getCachedCoverArt(currentlyPlaying))
                .setColor(InternetUtils.getChannelColor(channel.getId()))
                .setOngoing(getState() != RadioState.PAUSED)
                .setDeleteIntent(stopPendingIntent)
                .setContentIntent(channelPi)
                .addAction(playbackIcon, "Toggle Playback", playbackPendingIntent)
                .addAction(favIcon, "Favorite", favPandingIntent)
                .build();

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ServiceCommand", intent.getAction());
        switch (intent.getAction()) {
            case ACTION_PLAY:
                if (intent.hasExtra(ARG_CHANNEL_ID) && listener != null
                        && !listener.getChannel().equals(channel)) { //sent from other chd (listener)
                    ILRChannel newChannel = InternetUtils.getChannelById(intent.getIntExtra(ARG_CHANNEL_ID, 1));
                    switchChannel(newChannel);
                    //or: switchChannel(listener.getChannel());
                }
                if (getState() != RadioState.PREPARING) {
                    setState(RadioState.PREPARING);
                    start();
                }
                break;
            case ACTION_PAUSE:
                setState(RadioState.PAUSED);
                pause();
                break;
            case ACTION_STOP:
                setState(RadioState.STOPPED);
                stop();
                break;
            case ACTION_FAVORITE:
                favorite();
                break;
        }

        return Service.START_NOT_STICKY;
    }

    private void start() {
        Log.d("RadioService", "START");
        shouldSwitch = false;
        createNotification();
        wifiLock.acquire();
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //audioManager.registerMediaButtonEventReceiver(new ComponentName(this, RemoteControlReciever.class));

        try {
            mp.setDataSource(channel.getStreamURI());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mp.prepareAsync();
    }

    private void stop() {
        Log.d("RadioPlayback", "STOP");
        pause();
        wifiLock.release();
        //audioManager.unregisterMediaButtonEventReceiver(new ComponentName(this, RemoteControlReciever.class));
        stopForeground(true);
    }

    private void pause() {
        Log.d("RadioPlayback", "PAUSE");
        createNotification();
        if (mp.isPlaying())
            mp.stop();

        mp.reset();
        audioManager.abandonAudioFocus(this);
    }

    private void switchChannel(ILRChannel channel) {
        Log.d("RadioPlayback", "SWITCH: " + channel.toString());
        shouldSwitch = true;
        this.channel = channel;
        currentlyPlaying = InternetUtils.getCachedTrack(channel);

        if (mp.isPlaying()) {
            mp.stop();
            mp.reset();
        }
    }

    private void favorite() {
        if (currentlyPlaying != null) {
            boolean favorited = FavoritesHelper.getInstance(this).favorite(currentlyPlaying, channel.getName());
            createNotification();
            if (listener != null && channel.equals(listener.getChannel())) {
                listener.setFavorited(favorited);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (getState() == RadioState.STOPPED)
            switchChannel(InternetUtils.getChannelById(intent.getIntExtra(ARG_CHANNEL_ID, 1)));

        return new RadioServiceBinder();
        /* if playing, no new channels can be bound -> no direct switching of channels possible
         *
         */
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (shouldSwitch) {
            mp.stop();
            mp.reset();
            start();
            return;
        }

        if (getState() == RadioState.PREPARING) {
            setState(RadioState.PLAYING);
            mp.start();
        } else
            stop();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mp.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                setState(RadioState.PAUSED);
                pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mp.setVolume(0.0f, 0.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mp.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public ILRChannel getChannel() {
        return channel;
    }

    public ILRTrack getCurrentlyPlaying() {
        return currentlyPlaying;
    }

    private void updateCurrentlyPlaying() {
        new AsyncTask<Object, Integer, ILRTrack[]>() {
            @Override
            protected ILRTrack[] doInBackground(Object... params) {
                ILRTrack[] tracks = new ILRTrack[2];
                try {
                    tracks[0] = InternetUtils.fetchTrack(channel);
                    InternetUtils.fetchCoverArt(tracks[0]);
                    if (listener != null && !channel.equals(listener.getChannel())) {
                        tracks[1] = InternetUtils.fetchTrack(listener.getChannel());
                        InternetUtils.fetchCoverArt(tracks[1]);
                    }
                } catch (IOException ioe) {
                    cancel(true);
                }
                return tracks;
            }

            @Override
            protected void onPostExecute(ILRTrack[] tracks) {
                if (!tracks[0].equals(currentlyPlaying)) {
                    RadioService.this.currentlyPlaying = tracks[0];
                    createNotification();

                    if (tracks[1] == null && listener != null)
                        listener.onTrackChanged(tracks[0]);
                }

                if (tracks[1] != null && listener != null
                        && !tracks[1].equals(listener.getCurrentlyPlaying())) {
                    listener.onTrackChanged(tracks[1]);
                }
            }
        }.execute(null, null);
    }

    public enum RadioState {
        STOPPED, PREPARING, PLAYING, PAUSED
    }

    public interface RadioServiceListener {
        void onTrackChanged(ILRTrack newTrack);

        ILRChannel getChannel();

        void onStateChanged(RadioState newState);

        ILRTrack getCurrentlyPlaying();

        void setFavorited(boolean favorited);
    }

    public class RadioServiceBinder extends Binder {
        public RadioService getService() {
            return RadioService.this;
        }
    }
}
