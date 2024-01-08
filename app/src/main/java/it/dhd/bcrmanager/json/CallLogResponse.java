package it.dhd.bcrmanager.json;

import java.util.List;

public class CallLogResponse {
    private String timestamp;
    private String direction;
    private int sim_slot;
    private String call_log_name;
    private List<Call> calls;

    private Output output;

    // Constructors, getters, and setters

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    public void setSimSlot(int simSlot) {
        this.sim_slot = simSlot;
    }

    public String getCallLogName() {
        return call_log_name;
    }

    public void setCallLogName(String callLogName) {
        this.call_log_name = callLogName;
    }

    public List<Call> getCalls() {
        return calls;
    }

    public Output getOutput() {
        return output;
    }

    public void setCalls(List<Call> calls) {
        this.calls = calls;
    }
}
