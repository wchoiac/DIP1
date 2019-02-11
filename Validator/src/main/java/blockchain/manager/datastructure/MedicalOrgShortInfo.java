package blockchain.manager.datastructure;

import java.util.Arrays;

public class MedicalOrgShortInfo {

    private String name;
    private byte[] identifier;

    public MedicalOrgShortInfo(String name,  byte[] identifier)
    {
        this.setName(name);
        this.setIdentifier(identifier);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getIdentifier() {
        return identifier;
    }

    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    // only check identifier
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalOrgShortInfo that = (MedicalOrgShortInfo) o;
        return Arrays.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(identifier);
    }
}
