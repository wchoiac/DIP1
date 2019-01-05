package blockchain.utility;

import exception.BlockChainObjectParsingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class RawTranslator {

    public static byte[][] splitBytesToBytesArray(byte[] raw, int length) throws BlockChainObjectParsingException {

        ArrayList<byte[]> hashLocator = new ArrayList<>();

        if(raw.length%length!=0)
            throw new BlockChainObjectParsingException();

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);

        while(!byteArrayReader.isFinished())
        {
            hashLocator.add(byteArrayReader.readBytes(length));
        }

        return hashLocator.toArray(new byte[0][0]);
    }
    public static InetAddress[] parseAddresses(byte[] raw) throws BlockChainObjectParsingException {
        ArrayList<InetAddress> addresses = new ArrayList<>();

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);
        try{
            while(!byteArrayReader.isFinished())
            {
                byte length = byteArrayReader.readByte();
                addresses.add(InetAddress.getByAddress(byteArrayReader.readBytes(length)));
            }} catch (UnknownHostException e) {
            e.printStackTrace();
            throw new BlockChainObjectParsingException();
        }

        return addresses.toArray(new InetAddress[0]);
    }

    public static byte[] translateAddressesToBytes(InetAddress[] addresses){

        ByteArrayOutputStream byteArrayOutputStream =new ByteArrayOutputStream();

        try {
            for (InetAddress address : addresses) {
                byte[] addressBytes =address.getAddress();
                byteArrayOutputStream.write((byte)addressBytes.length);
                byteArrayOutputStream.write(addressBytes);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }
}
