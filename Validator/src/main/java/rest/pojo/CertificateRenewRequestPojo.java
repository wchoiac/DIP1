package rest.pojo;

import java.util.Date;

public class CertificateRenewRequestPojo {

    private byte[] identifier;
    private Date noAfter;

    public CertificateRenewRequestPojo(byte[] identifier, Date noAfter) {
        this.identifier = identifier;
        this.noAfter = noAfter;
    }

    public byte[] getIdentifier() {
        return identifier;
    }

    public void setIdentifier(byte[] identifier) {
        this.identifier = identifier;
    }

    public CertificateRenewRequestPojo() {
    }

    public Date getNoAfter() {
        return noAfter;
    }

    public void setNoAfter(Date noAfter) {
        this.noAfter = noAfter;
    }
}
