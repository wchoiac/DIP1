package blockchain.utility;

import exception.BlockChainObjectParsingException;
import general.utility.GeneralHelper;

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
    public static String[] parseAddresses(byte[] raw) throws BlockChainObjectParsingException {
        ArrayList<String> addresses = new ArrayList<>();

        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(raw);
        try{
            while(!byteArrayReader.isFinished())
            {
                int length = byteArrayReader.readInt();
                addresses.add(InetAddress.getByName(new String(byteArrayReader.readBytes(length))).getHostName());
            }} catch (UnknownHostException e) {
            e.printStackTrace();
            throw new BlockChainObjectParsingException();
        }

        return addresses.toArray(new String[0]);
    }

    public static byte[] translateAddressesToBytes(String[] addresses){

        ByteArrayOutputStream byteArrayOutputStream =new ByteArrayOutputStream();

        try {
            for (String address : addresses) {
                byte[] addressBytes =address.getBytes();
                byteArrayOutputStream.write(GeneralHelper.intToBytes(addressBytes.length));
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
