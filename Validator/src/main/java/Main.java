import node.validator.Validator;
import config.Configuration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import rest.server.ValidatorAPIResolver;
import rest.server.ValidatorRestServer;

import java.io.Console;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



public class Main {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {

        if(!Configuration.SIGNING_KEYSTORE_FILE.exists()||!Configuration.CONNECTION_KEYSTORE_FILE.exists()
                ||!Configuration.API_KEYSTORE_FILE.exists()
                || !Configuration.SESSION_FOLDER.exists()||!Configuration.USERINFO_FOLDER.exists()) {
            System.out.println("Please use initializer before running node");
            return;
        }

        Console console = System.console();
        Scanner sc = new Scanner(System.in);



        KeyStore signingKeyStore= KeyStore.getInstance(Configuration.KEYSTORE_TYPE);
        char[] signingKeyStorePassword;
        while(true) {
            try {
                if (console != null) {
                    signingKeyStorePassword = console.readPassword("Please enter signing keystore password: ");
                } else {
                    System.out.print("Please enter signing keystore password: ");
                    signingKeyStorePassword = sc.next().toCharArray();

                }
                signingKeyStore.load(new FileInputStream(Configuration.SIGNING_KEYSTORE_FILE), signingKeyStorePassword);
                break;
            }
            catch (Exception e)
            {
                System.out.println("Wrong Password");
                continue;
            }
        }


        KeyStore connectionKeyStore= KeyStore.getInstance(Configuration.KEYSTORE_TYPE);
        char[] connectionKeyStorePassword;
        while(true) {
            try {
                if (console != null) {
                    connectionKeyStorePassword = console.readPassword("Please enter connection keystore password: ");
                } else {
                    System.out.print("Please enter connection keystore password: ");
                    connectionKeyStorePassword = sc.next().toCharArray();

                }
                connectionKeyStore.load(new FileInputStream(Configuration.CONNECTION_KEYSTORE_FILE), connectionKeyStorePassword);
                break;
            }
            catch (Exception e)
            {
                System.out.println("Wrong Password");
                continue;
            }
        }

        KeyStore apiKeyStore= KeyStore.getInstance(Configuration.KEYSTORE_TYPE);
        char[] apiKeyStorePassword;
        while(true) {
            try {
                if (console != null) {
                    apiKeyStorePassword = console.readPassword("Please enter api keystore password: ");
                } else {
                    System.out.print("Please enter api keystore password: ");
                    apiKeyStorePassword = sc.next().toCharArray();

                }
                apiKeyStore.load(new FileInputStream(Configuration.API_KEYSTORE_FILE), apiKeyStorePassword);
                break;
            }
            catch (Exception e)
            {
                System.out.println("Wrong Password");
                continue;
            }
        }


        System.out.print("Is your address public static ip address? [y/n]: ");
        String isStaticPublic = sc.next();
        String myAddress=null;

        if(isStaticPublic.equals("n"))
            while (true) {
                System.out.print("Enter addressable name: "); //## for debug
                String hostName = sc.next();

                try {
                    myAddress=InetAddress.getByName(hostName).getHostName();
                    break;
                } catch (UnknownHostException e) {
                    System.out.println("Not appropriate name");
                }
            }

        ArrayList<InetAddress> potentialPeers = new ArrayList<>();

        System.out.print("Enter number of known peer nodes: ");
        int numOfPeerNodeKnown = sc.nextInt();

        for (int i = 0; i < numOfPeerNodeKnown; ++i) {
            while (true) {
                System.out.print("Enter peer node host name or ip: "); //## for debug
                String hostName = sc.next();

                try {
                    potentialPeers.add(InetAddress.getByName(hostName));
                    break;
                } catch (UnknownHostException e) {
                    System.out.println("Not appropriate ip Address or port");
                }
            }
        }

        if(Configuration.BLOCKCHAIN_PREV_PEERS.exists())
        {
            List<String> prevNodes =Files.readAllLines(Configuration.BLOCKCHAIN_PREV_PEERS.toPath());

            for(String prevNode:prevNodes)
            {
                try {
                    InetAddress potentialPeer =InetAddress.getByName(prevNode);
                    if(!potentialPeers.contains(potentialPeer))
                        potentialPeers.add(potentialPeer);
                } catch (UnknownHostException e) {
                    System.out.println("Previous node file is corrupted");
                }
            }
        }

        if(Configuration.BLOCKCHAIN_SEED.exists())
        {
            List<String> seedNodes =Files.readAllLines(Configuration.BLOCKCHAIN_SEED.toPath());

            for(String seed:seedNodes)
            {
                try {
                    InetAddress potentialPeer =InetAddress.getByName(seed);
                    if(!potentialPeers.contains(potentialPeer))
                        potentialPeers.add(potentialPeer);
                } catch (UnknownHostException e) {
                    System.out.println("Seed file is corrupted");
                }
            }

        }

        sc.close();

        Validator validator = Validator.create(connectionKeyStore,connectionKeyStorePassword,signingKeyStore,
                signingKeyStorePassword,Configuration.NODE_SERVER_PORT, Configuration.BLOCKCHAIN_LOG_FILENAME, myAddress);
        ValidatorRestServer restServer= ValidatorRestServer.create(apiKeyStore,apiKeyStorePassword
                ,Configuration.API_SERVER_PORT, new ValidatorAPIResolver(validator),Configuration.API_LOG_FILENAME);

        Runtime.getRuntime().addShutdownHook(new Thread(()-> { {

            try {
                System.out.println("shutdown from main start");
                validator.shutdown();
                restServer.shutdown();
                System.out.println("shutdown from main end");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        }, "Shutdown-thread"));

        validator.start(potentialPeers);  //## for debug
        restServer.start();
    }
}
