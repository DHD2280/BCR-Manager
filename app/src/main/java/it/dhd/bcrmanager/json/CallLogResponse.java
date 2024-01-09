package it.dhd.bcrmanager.json;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CallLogResponse {
    @SerializedName("timestamp_unix_ms") private String unixTimestamp;
    private String direction;
    private int sim_slot;
    private String call_log_name;
    private List<Call> calls;

    private Output output;

    // Constructors, getters, and setters

    public String getUnixTimestamp() {
        return unixTimestamp;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getSimSlot() {
        return sim_slot;
    }

    public String getCallLogName() {
        return call_log_name;
    }

    public List<Call> getCalls() {
        return calls;
    }

    public Output getOutput() {
        return output;
    }
}
