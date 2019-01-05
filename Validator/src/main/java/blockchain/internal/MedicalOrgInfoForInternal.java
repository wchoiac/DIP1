package blockchain.internal;

import blockchain.block.MedicalOrgInfo;

import java.util.Objects;

public class MedicalOrgInfoForInternal{

    private MedicalOrgInfo medicalOrgInfo;
    private byte[] authorityIdentifier;


    public MedicalOrgInfoForInternal(MedicalOrgInfo info, byte[] authorityIdentifier)
    {
        this.setMedicalOrgInfo(info);
        this.setAuthorityIdentifier(authorityIdentifier);

    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (!(o instanceof MedicalOrgInfoForInternal)) {
            return false;
        }

        MedicalOrgInfoForInternal medicalOrgInfoForInternal = (MedicalOrgInfoForInternal) o;
        return medicalOrgInfoForInternal.getMedicalOrgInfo().equals(getMedicalOrgInfo());

    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getMedicalOrgInfo());

    }

    public MedicalOrgInfo getMedicalOrgInfo() {
        return medicalOrgInfo;
    }

    public void setMedicalOrgInfo(MedicalOrgInfo medicalOrgInfo) {
        this.medicalOrgInfo = medicalOrgInfo;
    }

    public byte[] getAuthorityIdentifier() {
        return authorityIdentifier;
    }

    public void setAuthorityIdentifier(byte[] authorityIdentifier) {
        this.authorityIdentifier = authorityIdentifier;
    }
}
