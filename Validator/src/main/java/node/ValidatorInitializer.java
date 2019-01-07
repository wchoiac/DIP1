package node;

import blockchain.block.BlockContent;
import blockchain.block.BlockHeader;
import blockchain.internal.StateInfo;
import blockchain.Status;
import blockchain.block.AuthorityInfo;
import blockchain.block.Block;
import blockchain.utility.BlockChainSecurityHelper;
import blockchain.utility.ByteArrayReader;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import general.utility.GeneralHelper;
import org.bouncycastle.operator.OperatorCreationException;
import general.security.SecurityHelper;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;


/**
 * get key file and produce a keystore with certificate and private key
 */
public class ValidatorInitializer {
    public static void main(String[] args) throws IOException, ClassNotFoundException, KeyStoreException, CertificateException, NoSuchAlgorithmException, OperatorCreationException, InvalidKeySpecException, NoSuchProviderException, FileCorruptionException, BlockChainObjectParsingException {


        Scanner sc = new Scanner(System.in);


        int selection = 0;
        while (selection < 5 && selection >= 0) {
            System.out.println("0. Load genesis block");
            System.out.println("1. Initialize for API");
            System.out.println("2. Generate certificate and keystore for signing");
            System.out.println("3. Generate certificate and keystore for Blockchain platform connection");
            System.out.println("4. Generate certificate and keystore for Rest API server");
            System.out.println("5. Quit");

            System.out.println("Choose your action:");
            selection = sc.nextInt();

            switch (selection) {
                case 0:
                    genesisLoad();
                    break;
                case 1:
                    initializeForAPI();
                    break;
                case 2:
                    createCertAndKeyStoreForSigning(sc);
                    break;
                case 3:
                    createCertAndKeyStoreForConnection(sc);
                    break;
                case 4:
                    createCertAndKeyStoreForAPI(sc);
                    break;
                default:
                    break;

            }
            // create index file with the genesisBlock

        }
        sc.close();
    }

    public static void initializeForAPI() {
        if (!Configuration.SESSION_FOLDER.exists()) {
            Configuration.SESSION_FOLDER.mkdirs();
        }

        if (!Configuration.USERINFO_FOLDER.exists()) {
            Configuration.USERINFO_FOLDER.mkdirs();
        }

        System.out.println("Successfully initialized");
    }

    public static void createCertAndKeyStoreForSigning(Scanner sc) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, KeyStoreException, CertificateException, OperatorCreationException, NoSuchProviderException {
        if (!Configuration.AUTHENTICATION_FOLDER.exists())
            Configuration.AUTHENTICATION_FOLDER.mkdirs();

        if (Configuration.SIGNING_KEYSTORE_FILE.exists()) {
            System.out.println("Error: signingKeyStore file already exists");
        } else {


            Console console = System.console();

            KeyStore signingKeyStore = KeyStore.getInstance("JKS");
            signingKeyStore.load(null);
            char[] signingKeyStorePassword;

            ECPublicKey signingPublicKey;
            ECPrivateKey signingPrivateKey;
            X509Certificate signingCert;

            System.out.print("Please enter your signing public key file name: ");
            String signingPubString = sc.next();
            while (!new File(signingPubString).exists()) {
                System.out.print("File doesn't exist, please enter again: ");
                signingPubString = sc.next();
            }
            signingPublicKey = (ECPublicKey) SecurityHelper.getPublicKeyFromPEM(signingPubString, "EC");

            System.out.print("Please enter your signing private key file name: ");
            String signingPrivString = sc.next();
            while (!new File(signingPrivString).exists()) {
                System.out.print("File doesn't exist, please enter again: ");
                signingPrivString = sc.next();
            }
            signingPrivateKey = (ECPrivateKey)SecurityHelper.getPrivateFromPEM(signingPrivString, "EC");


            Date noAfter;
            String name;
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            while (true) {
                try {
                    System.out.print("Please enter expiry date in yyyy-MM-dd format: ");
                    noAfter = formatter.parse(sc.next());
                } catch (ParseException e) {
                    System.out.println("Wrong Format");
                    continue;
                }
                break;
            }

            System.out.print("Please enter name: ");
            name = sc.next();

            if (console != null) {
                do {
                    signingKeyStorePassword = console.readPassword("Please enter keystore password: ");
                }
                while (!Arrays.equals(signingKeyStorePassword, console.readPassword("Please re-enter keystore password: ")));
            } else {
                do {
                    System.out.print("Please enter keystore password: ");
                    signingKeyStorePassword = sc.next().toCharArray();
                    System.out.print("Please re-enter keystore password: ");
                }
                while (!Arrays.equals(signingKeyStorePassword, sc.next().toCharArray()));
            }

            signingCert = SecurityHelper.issueCertificate(signingPublicKey, signingPublicKey
                    , signingPrivateKey, noAfter, name, name, BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(signingPublicKey)
                    ,BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(signingPublicKey), null, Configuration.SIGNING_CERTIFICATE_SIGNATURE_ALGORITHM
                    , true);
            signingKeyStore.setKeyEntry(Configuration.SIGNING_KEYSTORE_ALIAS, signingPrivateKey, signingKeyStorePassword, new Certificate[]{signingCert});
            signingKeyStore.store(new FileOutputStream(Configuration.SIGNING_KEYSTORE_FILE), signingKeyStorePassword);
            System.out.println("Successful: Signing keyStore created.");
        }
    }


    public static void createCertAndKeyStoreForAPI(Scanner sc) throws OperatorCreationException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException, NoSuchProviderException {
        if (!Configuration.SIGNING_KEYSTORE_FILE.exists()) {
            System.out.println("Error: signingKeyStore file doesn't exist");
            return;
        }

        if (Configuration.API_KEYSTORE_FILE.exists()) {
            System.out.println("Error: apiKeystore file already exists");
        } else {


            Console console = System.console();


            KeyStore signingKeyStore = KeyStore.getInstance("JKS");
            char[] signingKeyStorePassword;

            ECPublicKey signingPublicKey;
            ECPrivateKey signingPrivateKey;
            String issuerName;
            X509Certificate signingCert;
            while (true) {
                try {
                    if (console != null) {
                        signingKeyStorePassword = console.readPassword("Please enter signing keystore password: ");
                    } else {
                        System.out.print("Please enter signing keystore password: ");
                        signingKeyStorePassword = sc.next().toCharArray();

                    }
                    signingKeyStore.load(new FileInputStream(Configuration.SIGNING_KEYSTORE_FILE), signingKeyStorePassword);
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) signingKeyStore.getEntry(Configuration.SIGNING_KEYSTORE_ALIAS, new KeyStore.PasswordProtection(signingKeyStorePassword));
                    signingPublicKey = (ECPublicKey) entry.getCertificateChain()[0].getPublicKey();
                    signingPrivateKey = (ECPrivateKey) entry.getPrivateKey();
                    signingCert = (X509Certificate) entry.getCertificateChain()[0];
                    issuerName = SecurityHelper.getSubjectCNFromX509Certificate(signingCert);
                    break;
                } catch (Exception e) {
                    System.out.println("Wrong Password");
                    continue;
                }
            }


            KeyStore apiKeyStore = KeyStore.getInstance("JKS");
            apiKeyStore.load(null);
            char[] apiKeyStorePassword;

            System.out.print("Please enter your api public key file name: ");
            String apiPubString = sc.next();
            while (!new File(apiPubString).exists()) {
                System.out.print("File doesn't exist, please enter again: ");
                apiPubString = sc.next();
            }
            PublicKey apiPublicKey = SecurityHelper.getPublicKeyFromPEM(apiPubString, "RSA");

            System.out.print("Please enter your api private key file name: ");
            String apiPrivString = sc.next();
            while (!new File(apiPrivString).exists()) {
                System.out.print("File doesn't exist, please enter again: ");
                apiPrivString = sc.next();
            }
            PrivateKey apiPrivateKey = SecurityHelper.getPrivateFromPEM(apiPrivString, "RSA");

            Date noAfter;
            String subjectName;
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            while (true) {
                try {
                    System.out.print("Please enter expiry date in yyyy-MM-dd format: ");
                    noAfter = formatter.parse(sc.next());
                } catch (ParseException e) {
                    System.out.println("Wrong Format");
                    continue;
                }
                break;
            }

            System.out.print("Please enter subject name: ");
            subjectName = sc.next();

            InetAddress apiServerAddress;
            while (true) {
                System.out.print("Enter server's ip address:"); //## for debug
                try {
                    apiServerAddress = InetAddress.getByName(sc.next());
                    break;
                } catch (UnknownHostException e) {
                    System.out.println("Not appropriate input");
                }
            }

            if (console != null) {
                do {
                    apiKeyStorePassword = console.readPassword("Please enter keystore password: ");
                }
                while (!Arrays.equals(apiKeyStorePassword, console.readPassword("Please re-enter keystore password: ")));
            } else {
                do {
                    System.out.print("Please enter keystore password: ");
                    apiKeyStorePassword = sc.next().toCharArray();
                    System.out.print("Please re-enter keystore password: ");
                }
                while (!Arrays.equals(apiKeyStorePassword, sc.next().toCharArray()));
            }

            X509Certificate apiCert = SecurityHelper.issueCertificate(apiPublicKey, signingPublicKey, signingPrivateKey, noAfter
                    , subjectName, issuerName,null, BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(signingPublicKey),
                    apiServerAddress, Configuration.API_CERTIFICATE_SIGNATURE_ALGORITHM, false);
            apiKeyStore.setKeyEntry(Configuration.API_KEYSTORE_ALIAS, apiPrivateKey, apiKeyStorePassword, new Certificate[]{apiCert, signingCert});
            apiKeyStore.store(new FileOutputStream(Configuration.API_KEYSTORE_FILE), apiKeyStorePassword);
            System.out.println("Successful: API keyStore created.");
        }
    }


    public static void createCertAndKeyStoreForConnection(Scanner sc) throws OperatorCreationException, CertificateException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, KeyStoreException, NoSuchProviderException {
        if (!Configuration.SIGNING_KEYSTORE_FILE.exists()) {
            System.out.println("Error: signingKeyStore file doesn't exist");
            return;
        }

        if (Configuration.CONNECTION_KEYSTORE_FILE.exists()) {
            System.out.println("Step2 Error: blockChainKeystore file already exists");
        } else {

            Console console = System.console();


            KeyStore signingKeyStore = KeyStore.getInstance("JKS");
            char[] signingKeyStorePassword;

            ECPublicKey signingPublicKey;
            ECPrivateKey signingPrivateKey;
            String issuerName;
            X509Certificate signingCert;
            while (true) {
                try {
                    if (console != null) {
                        signingKeyStorePassword = console.readPassword("Please enter signing keystore password: ");
                    } else {
                        System.out.print("Please enter signing keystore password: ");
                        signingKeyStorePassword = sc.next().toCharArray();

                    }
                    signingKeyStore.load(new FileInputStream(Configuration.SIGNING_KEYSTORE_FILE), signingKeyStorePassword);
                    KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) signingKeyStore.getEntry(Configuration.SIGNING_KEYSTORE_ALIAS, new KeyStore.PasswordProtection(signingKeyStorePassword));
                    signingPublicKey = (ECPublicKey) entry.getCertificateChain()[0].getPublicKey();
                    signingPrivateKey = (ECPrivateKey) entry.getPrivateKey();

                    signingCert = (X509Certificate) entry.getCertificateChain()[0];
                    issuerName = SecurityHelper.getSubjectCNFromX509Certificate(signingCert);
                    break;
                } catch (Exception e) {
                    System.out.println("Wrong Password");
                    continue;
                }
            }


            KeyStore connectionKeyStore = KeyStore.getInstance("JKS");
            connectionKeyStore.load(null);
            char[] connectionKeyStorePassword;


            System.out.print("Please enter your blockchain connection public key file name: ");
            String blockPubString = sc.next();
            while (!new File(blockPubString).exists()) {
                System.out.print("File doesn't exist, please enter again: ");
                blockPubString = sc.next();
            }
            PublicKey connectionPublickey = SecurityHelper.getPublicKeyFromPEM(blockPubString, "RSA");

            System.out.print("Please enter your blockchain connection private key file name: ");
            String blockPrivString = sc.next();
            while (!new File(blockPrivString).exists()) {
                System.out.print("File doesn't exist, please enter again: ");
                blockPrivString = sc.next();
            }

            PrivateKey connectionPrivateKey = SecurityHelper.getPrivateFromPEM(blockPrivString, "RSA");

            Date noAfter;
            String subjectName;
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            while (true) {
                try {
                    System.out.print("Please enter expiry date in yyyy-MM-dd format: ");
                    noAfter = formatter.parse(sc.next());
                } catch (ParseException e) {
                    System.out.println("Wrong Format");
                    continue;
                }
                break;
            }

            System.out.print("Please enter subject name: ");
            subjectName = sc.next();


            if (console != null) {
                do {
                    connectionKeyStorePassword = console.readPassword("Please enter keystore password: ");
                }
                while (!Arrays.equals(connectionKeyStorePassword, console.readPassword("Please re-enter keystore password: ")));
            } else {
                do {
                    System.out.print("Please enter keystore password: ");
                    connectionKeyStorePassword = sc.next().toCharArray();
                    System.out.print("Please re-enter keystore password: ");
                }
                while (!Arrays.equals(connectionKeyStorePassword, sc.next().toCharArray()));
            }


            X509Certificate blockCert = SecurityHelper.issueCertificate(connectionPublickey, signingPublicKey, signingPrivateKey
                    , noAfter, subjectName, issuerName,null, BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(signingPublicKey),
                    null, Configuration.CONNECTION_CERTIFICATE_SIGNATURE_ALGORITHM, false);
            connectionKeyStore.setKeyEntry(Configuration.CONNECTION_KEYSTORE_ALIAS, connectionPrivateKey, connectionKeyStorePassword, new Certificate[]{blockCert, signingCert});
            connectionKeyStore.store(new FileOutputStream(Configuration.CONNECTION_KEYSTORE_FILE), connectionKeyStorePassword);
            System.out.println("Successful: Blockchain connection keystore created.");
        }
    }

    public static void genesisLoad() throws IOException, ClassNotFoundException, BlockChainObjectParsingException, FileCorruptionException {
        // create index file with the genesisfile
        if (!Configuration.GENESISBLOCK_FILE.exists()) {

            System.out.println("File genesisBlock doesn't exist");
            return;
        }
        byte[] genesisAllByte = Files.readAllBytes(Configuration.GENESISBLOCK_FILE.toPath());
        ByteArrayReader byteArrayReader = new ByteArrayReader();
        byteArrayReader.set(genesisAllByte);

        Block genesisBlock = new Block();
        genesisBlock.setHeader(BlockHeader.parse(byteArrayReader));
        genesisBlock.setContent(BlockContent.parse(genesisBlock.getHeader().getStructureIndicator(),byteArrayReader));

        if(!byteArrayReader.isFinished())
            throw new FileCorruptionException();


        byte[] blockHash = genesisBlock.calculateHash();


        if (!Configuration.BLOCKCHAIN_DATA_FOLDER.exists()) {
            genesisBlockSave(genesisBlock);
            genesisAuthoritySave(blockHash, genesisBlock.getContent().getInitialAuthorities());
            genesisBestChainSave(new Status(0, blockHash));
            genesisChainInfoSave(genesisBlock);
            genesisStateInfoSave(genesisBlock);
            genesisVotingSave(blockHash);
            System.out.println("Successful: Genesis block is processed.");
        } else {
            System.out.println("Error: Data folder already exists, if genesis block needs to be loaded again, please delete the folder first.");
            return;
        }
    }


    public static void genesisBlockSave(Block block) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File blockHeaderFile = new File(blockFolder, "header");
        File blockContentFile = new File(blockFolder, "content");

        blockFolder.mkdirs();
        blockHeaderFile.createNewFile();
        blockContentFile.createNewFile();

        try (FileOutputStream os = new FileOutputStream(blockHeaderFile);) {
            os.write(block.getHeader().getRaw());
        }

        try (FileOutputStream os = new FileOutputStream(blockContentFile);) {
            os.write(block.getContent().getRaw());

        }
    }


    public static void genesisVotingSave(byte[] blockHash) throws IOException {
        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File votingListFile = new File(blockFolder, "voting");

        blockFolder.mkdirs();
        votingListFile.createNewFile(); //empty file
    }

    public static void genesisStateInfoSave(Block block) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File stateInfoFile = new File(blockFolder, "stateInfo");

        if (!blockFolder.exists())
            blockFolder.mkdirs();
        StateInfo blockStateInfo = new StateInfo(block.calculateHash(), block.calculateHash(), 0, block.getContent().getInitialAuthorities().length);
        try (FileOutputStream os = new FileOutputStream(stateInfoFile)) {
            os.write(blockStateInfo.getRaw());
        }
    }

    private static void genesisChainInfoSave(Block block) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(block.calculateHash());
        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString + "/");
        File chainInfoFile = new File(blockFolder, "chainInfo");

        if (!blockFolder.exists())
            blockFolder.mkdirs();
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(chainInfoFile));) {
            bos.write(block.getHeader().getPrevHash());
            bos.write(1); //true
        }

    }

    public static void genesisBestChainSave(Status status) throws IOException {

        if (!Configuration.BLOCKCHAIN_DATA_FOLDER.exists()) {
            Configuration.BLOCKCHAIN_DATA_FOLDER.mkdirs();
        }
        try (FileOutputStream os = new FileOutputStream(Configuration.BEST_CHAIN_FILE);) {
            os.write(status.getRaw());
        }
    }

    public static void genesisAuthoritySave(byte[] blockHash, AuthorityInfo[] initialAuthorityInfos) throws IOException {

        String blockHashString = GeneralHelper.bytesToStringHex(blockHash);

        File blockFolder = new File(Configuration.BLOCK_FOLDER, blockHashString.charAt(0) + "/" + blockHashString.charAt(1) + "/"
                + blockHashString.charAt(2) + "/" + blockHashString.charAt(3) + "/" + blockHashString.charAt(4) + "/" + blockHashString);

        File authoritiesFile = new File(blockFolder, "authorities");

        if (!blockFolder.exists()) {
            blockFolder.mkdirs();
        }

        //write overall list
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(authoritiesFile));
        for (AuthorityInfo authorityInfo : initialAuthorityInfos) {
            bos.write(authorityInfo.getIdentifier());
        }
        bos.close();

        //process each authority
        for (AuthorityInfo authorityInfo : initialAuthorityInfos) {

            String authorityIdentifierString = GeneralHelper.bytesToStringHex(authorityInfo.getIdentifier());

            File authorityFolder = new File(Configuration.AUTHORITY_FOLDER, authorityIdentifierString.charAt(0) + "/" + authorityIdentifierString.charAt(1) + "/" + authorityIdentifierString + "/");
            File trustFile = new File(authorityFolder, "trust");

            if (!authorityFolder.exists()) {
                authorityFolder.mkdirs();
            }
            try (FileOutputStream os = new FileOutputStream(trustFile, true)) {
                os.write(blockHash);
            }
        }


    }


}
