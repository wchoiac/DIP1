package blockchain.utility;

import general.utility.GeneralHelper;

import java.util.Arrays;

public class ByteArrayReader {

    private byte[] content;
    private int currOffset;

    public ByteArrayReader()
    {

    }

    public void set(byte[] content)
    {
        this.content=content;
        this.currOffset =0;
    }

    public byte[] readBytes(int length)
    {
        byte[] result = Arrays.copyOfRange(content,currOffset,currOffset+length);
        currOffset+=length;

       // System.out.println(GeneralHelper.bytesToStringHex(result)); // debug

        return result;
    }

    public byte readByte()
    {
//        int result = content[currOffset++];// debug
//        System.out.println(result); // debug
//        return (byte)result;// debug

        return content[currOffset++];
    }

    public int readInt()
    {
        return GeneralHelper.bytesToInt(readBytes(Integer.BYTES));
    }
    public long readLong()
    {
        return GeneralHelper.bytesToLong(readBytes(Long.BYTES));
    }

    public boolean isFinished()
    {
        return currOffset==content.length;
    }
}
