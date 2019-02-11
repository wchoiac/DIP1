package pojo;

public class RecordShortInfoPojo {

    private LocationPojo locationPojo;
    private long timestamp;
    private String medicalOrgName;

    public LocationPojo getLocationPojo() {
        return locationPojo;
    }

    public void setLocationPojo(LocationPojo locationPojo) {
        this.locationPojo = locationPojo;
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
