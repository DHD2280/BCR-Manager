package it.dhd.bcrmanager.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer;

    private int lastPosition;
    private float lastSpeed;
    private boolean isSpeedSet = false;

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    private MediaPlayer.OnCompletionListener completionListener;

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        this.completionListener = listener;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Handle audio playback completion if needed
        if (completionListener != null) {
            completionListener.onCompletion(mp);
        }
    }

    /**
     * Method to start playback of audio file
     * @param c The application context
     * @param audioFilePath The Uri of the audio file
     */
    public void startPlayback(Context c, Uri audioFilePath) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes
                            .Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            mediaPlayer.setDataSource(c, audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to pause playback
     */
    public void pausePlayback() {
        if (mediaPlayer.isPlaying()) {
            lastPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    public int getLastPosition() {
        return lastPosition;
    }

    /**
     * Method to resume playback
     */
    public void resumePlayback() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    /**
     * Method to stop playback
     */
    public void stopPlayback() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    // Method to get current playback position
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    // Method to get total duration of the audio file
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    // Method to check if playback is currently in progress
    public boolean isPlaying() {
        try {
            return mediaPlayer.isPlaying();
        } catch(IllegalStateException e) {
            // media player is not initialized
            return false;
        }
    }

    public MediaPlayer getMediaPlayer() {
        if (mediaPlayer != null) return mediaPlayer; else return null;
    }

    public void seekTo(int newPosition) {
        mediaPlayer.seekTo(newPosition);
    }

    public boolean isSpeedSet() { return isSpeedSet; }

    public void reset() {
        mediaPlayer.reset();
    }

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
        mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
    }

    /**
     * Get the current playback speed
     * @return The current playback speed (1.0f if not set)
     */
    public float getSpeed() {
        float speed = mediaPlayer.getPlaybackParams().getSpeed();
        if (speed != 0.0) return speed; else return 1.0f;
    }

    public float getLastSpeed() {
        if (isSpeedSet) return lastSpeed;
        else return 1.0f;
    }
}