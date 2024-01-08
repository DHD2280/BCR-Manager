package it.dhd.bcrmanager.json;

public class Output {

    private Recording recording;
    private Format format;

    public Recording getRecording() {
        return recording;
    }

    public Format getFormat() {
        return format;
    }


    public static class Format {
        private String type;

        public String getType() {
            return type;
        }
    }

    public static class Recording {
        private long frames_total;
        private long frames_encoded;
        private int sample_rate;
        private int channel_count;
        private double duration_secs_total;
        private double duration_secs_encoded;
        private int buffer_frames;
        private int buffer_overruns;
        private boolean was_ever_paused;
        private boolean was_ever_holding;

        // getters and setters

        public double getDurationSecsTotal() {
            return duration_secs_total;
        }

    }

}
