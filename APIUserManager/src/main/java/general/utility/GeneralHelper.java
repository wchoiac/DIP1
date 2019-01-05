package general.utility;

import java.util.ArrayList;

public class GeneralHelper {
    public static String bytesToStringHex(byte[] data) {
        if (data == null)
            return null;
        StringBuilder hashValue = new StringBuilder();
        for (byte b : data) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1)
                hashValue.append("0" + hex);
            else
                hashValue.append(hex);
        }
        return hashValue.toString();
    }

    public static byte[] mergeByteArrays(byte[]... bs) {
        ArrayList<Byte> mergedList = new ArrayList<>();
        for (byte[] bytes : bs) {
            if (bytes != null)
                for (byte aByte : bytes)
                    mergedList.add(aByte);
        }

        byte[] mergedArray = new byte[mergedList.size()];

        for (int i = 0; i < mergedArray.length; ++i)
            mergedArray[i] = mergedList.get(i);

        return mergedArray;
    }

    public static byte[] charToByte(char[] array) {

        byte[] result = new byte[array.length];

        for (int i = 0; i < array.length; i++) {
            result[i] = (byte) array[i];
        }

        return result;
    }
}
