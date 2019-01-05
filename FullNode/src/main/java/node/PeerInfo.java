package node;

import java.util.Arrays;

public class PeerInfo {

    private byte[] peerIdentifier;
    private byte[] issuerIdentifier;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return Arrays.equals(getPeerIdentifier(), peerInfo.getPeerIdentifier()) &&
                Arrays.equals(getIssuerIdentifier(), peerInfo.getIssuerIdentifier());
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(getPeerIdentifier());
        result = 31 * result + Arrays.hashCode(getIssuerIdentifier());
        return result;
    }

    public PeerInfo(byte[] peerIdentifier, byte[] issuerIdentifier) {
        this.setPeerIdentifier(peerIdentifier);
        this.setIssuerIdentifier(issuerIdentifier);
    }

    public boolean isAuthority()
    {
        return Arrays.equals(peerIdentifier,issuerIdentifier);
    }

    public byte[] getPeerIdentifier() {
        return peerIdentifier;
    }

    public void setPeerIdentifier(byte[] peerIdentifier) {
        this.peerIdentifier = peerIdentifier;
    }

    public byte[] getIssuerIdentifier() {
        return issuerIdentifier;
    }

    public void setIssuerIdentifier(byte[] issuerIdentifier) {
        this.issuerIdentifier = issuerIdentifier;
    }
}
