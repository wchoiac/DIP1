package general.utility;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtility {
    public static void showNetworkInterfaces() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces=NetworkInterface.getNetworkInterfaces();
        while(networkInterfaces.hasMoreElements())
        {
            NetworkInterface networkInterface= networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            if(!inetAddresses.hasMoreElements())
                continue;
            String ipString=null;
            while(inetAddresses.hasMoreElements())
            {
                try {
                    InetAddress address = InetAddress.getByName(inetAddresses.nextElement().getHostAddress());
                    if (address instanceof Inet4Address)
                        ipString="IPv4: " + address.getHostAddress();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if(ipString==null)
                continue;

            System.out.println("Display name: "+networkInterface.getDisplayName());
            System.out.println("Name: "+networkInterface.getName());
            System.out.println(ipString);
            System.out.println();

        }
    }

    // not display name
    public static InetAddress getMyAddress(String name) throws SocketException {
        NetworkInterface networkInterface= NetworkInterface.getByName(name);
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while(inetAddresses.hasMoreElements())
        {
            try {
                InetAddress address = InetAddress.getByName(inetAddresses.nextElement().getHostAddress());
                if (address instanceof Inet4Address)
                    return address;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return null;

    }
}
