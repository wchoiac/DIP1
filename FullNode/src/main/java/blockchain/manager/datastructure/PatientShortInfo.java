package blockchain.manager.datastructure;

public class PatientShortInfo {
    private Location location;
    private long timestamp;

    public PatientShortInfo(Location location, long timestamp) {
        this.location = location;
        this.timestamp = timestamp;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
