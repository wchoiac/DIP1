package blockchain.manager.datastructure;

public class RecordShortInfo {

    private Location location;
    private long timestamp;
    private String medicalOrgName;

    public RecordShortInfo(Location location, long timestamp, String medicalOrgName) {
        this.location = location;
        this.timestamp = timestamp;
        this.medicalOrgName = medicalOrgName;
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

    public String getMedicalOrgName() {
        return medicalOrgName;
    }

    public void setMedicalOrgName(String medicalOrgName) {
        this.medicalOrgName = medicalOrgName;
    }
}
