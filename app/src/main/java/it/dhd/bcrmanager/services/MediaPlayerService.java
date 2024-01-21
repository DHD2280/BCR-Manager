package it.dhd.bcrmanager.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import it.dhd.bcrmanager.objects.CallLogItem;

public class MediaPlayerService extends Service implements Player.Listener {

    private ExoPlayer player;

    private long lastPosition;
    private float lastSpeed;
    private boolean isSpeedSet = false;

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle audio playback commands from your activity
        // You can use intent extras to pass commands or data
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
        }
        super.onDestroy();
    }

    private onCompleteListener completionListener;
    public interface onCompleteListener {
        void onCompletion();
    }

    public void setOnCompletionListener(onCompleteListener listener) {
        this.completionListener = listener;
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        // Handle playback state change here
        if (state == Player.STATE_ENDED)
            if (completionListener != null)
                completionListener.onCompletion();
    }

    /**
     * Method to start playback of audio file
     * @param item The CallLogItem to play
     */
    public void startPlayback(CallLogItem item) {
        try {
            player.setAudioAttributes(
                    new AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.DEFAULT.contentType)
                            .build(), true);
            player.setMediaItem(new MediaItem.Builder()
                    .setUri(item.getAudioFilePath())
                    .setMediaId(item.getFileName())
                            .setMediaMetadata(new MediaMetadata.Builder()
                                    .setTitle(item.getContactName())
                                    .setSubtitle(item.getNumber())
                                    .build())
                    .build());
            player.prepare();
            player.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to pause playback
     */
    public void pausePlayback() {
        if (player.isPlaying()) {
            lastPosition = player.getCurrentPosition();
            player.pause();
        }
    }

    public long getLastPosition() {
        return lastPosition;
    }

    /**
     * Method to resume playback
     */
    public void resumePlayback() {
        if (!player.isPlaying()) {
            player.play();
        }
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0);
            player.play();
        }
    }

    /**
     * Method to stop playback
     */
    public void stopPlayback() {
        if (player.isPlaying()) {
            player.stop();
        }
    }

    // Method to get current playback position
    public long getCurrentPosition() {
        return player.getCurrentPosition();
    }

    // Method to get total duration of the audio file
    public long getDuration() {
        return player.getDuration();
    }

    // Method to check if playback is currently in progress
    public boolean isPlaying() {
        return player.isPlaying();
    }

    public ExoPlayer getPlayer() {
        if (player != null) return player; else return null;
    }

    public void seekTo(long newPosition) {
        player.seekTo(newPosition);
    }

    public boolean isSpeedSet() { return isSpeedSet; }


    // Binder class for communication with the activity
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /**
     * Set the playback speed
     * @param speed The playback speed
     */
    public void setSpeed(float speed) {
        lastSpeed = speed;
        isSpeedSet = true;
        player.setPlaybackSpeed(speed);
    }

    /**
     * Get the current playback speed
     * @return The current playback speed (1.0f if not set)
     */
    public float getSpeed() {
        float speed = player.getPlaybackParameters().speed;
        if (speed != 0.0) return speed; else return 1.0f;
    }

    public float getLastSpeed() {
        if (isSpeedSet) return lastSpeed;
        else return 1.0f;
    }
}