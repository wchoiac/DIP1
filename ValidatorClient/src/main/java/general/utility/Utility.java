package general.utility;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;


public class Utility {

	public static byte[] bytesFromBase64String(String base64String)
	{
		return Base64.getDecoder().decode(base64String);
	}

	public static String bytesToBase64String(byte[] bytes)
	{
		return Base64.getEncoder().encodeToString(bytes);
	}

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

	//##reference: https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
	public static byte[] stringHexToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static byte[] mergeByteArrays(byte[]... bs) {
		ArrayList<Byte> mergedList = new ArrayList<>();
		for (byte[] bytes : bs) {
			if(bytes!=null)
				for (byte aByte : bytes)
					mergedList.add(aByte);
		}

		byte[] mergedArray = new byte[mergedList.size()];

		for (int i = 0; i < mergedArray.length; ++i)
			mergedArray[i] = mergedList.get(i);

		return mergedArray;
	}


	// from https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
	public static byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	public static long bytesToLong(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(bytes);
		buffer.flip();//need flip
		return buffer.getLong();
	}

	// from https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java
	public static byte[] intToBytes(int x) {
		ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
		buffer.putInt(x);
		return buffer.array();
	}


	// reference:   ............................
	public static class hashCompare implements Comparator<byte[]> {
		@Override
		public int compare(byte[] byteArray1, byte[] byteArray2) {

			int result = 0;
			int maxLength = Math.max(byteArray1.length, byteArray2.length);
			for (int index = 0; index < maxLength; index++) {
				int o1Value = index < byteArray1.length ? byteArray1[index] : 0;
				int o2Value = index < byteArray2.length ? byteArray2[index] : 0;
				int cmp = Integer.compare(o1Value, o2Value);
				if (cmp != 0) {
					result = cmp;
					break;
				}
			}
			return result;
		}
	}

}
