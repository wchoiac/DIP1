package node;

import blockchain.Status;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Hello {

    private Status status;
    private String addressString=null;

    public void setStatus(Status status) {
        this.status = status;
    }
    public Status getStatus(){
        return status;
    }

    public void setAddressString(String addressString) {
        this.addressString = addressString;
    }
    public String getAddressString(){
        return addressString;
    }

    public Hello(Status status, String addressString)
    {
        this.setStatus(status);
        this.setAddressString(addressString);
    }

    public Hello()
    {
    }

    public byte[] getRaw()
    {
        byte[] lengthAndAddress=null;
        if(addressString!=null){ // in case name should be used
            lengthAndAddress= GeneralHelper.mergeByteArrays(GeneralHelper.intToBytes(addressString.length()),addressString.getBytes());
        }

        return GeneralHelper.mergeByteArrays(status.getRaw(), lengthAndAddress==null? GeneralHelper.intToBytes(0):lengthAndAddress);
    }

        public static Hello parse(ByteArrayReader byteArrayReader) throws UnknownHostException {

            Hello hello = new Hello();
            hello.setStatus(Status.parse(byteArrayReader));

            int length =byteArrayReader.readInt();
            if(length==0)
                hello.setAddressString(null);
            else{
                hello.setAddressString(InetAddress.getByName(new String(byteArrayReader.readBytes(length))).getHostName());
            }

        return hello;
    }

    public static Hello parse(byte[] raw) throws BlockChainObjectParsingException {

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);
        Hello hello = null;
        try {
            hello = parse(byteArrayReader);
        } catch (UnknownHostException e) {
            throw new BlockChainObjectParsingException();
        }

        if(!byteArrayReader.isFinished())
            throw new BlockChainObjectParsingException();

        return hello;
    }
}
