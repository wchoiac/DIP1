package node.validator;

import java.net.*;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.*;
import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import blockchain.BlockChain;
import blockchain.internal.ChainInfo;
import blockchain.Status;
import blockchain.internal.Voting;
import blockchain.block.*;
import blockchain.block.transaction.Transaction;
import blockchain.manager.*;
import blockchain.manager.datastructure.Location;
import blockchain.manager.datastructure.MedicalOrgShortInfo;
import blockchain.manager.datastructure.PatientShortInfo;
import blockchain.utility.BlockChainSecurityHelper;
import blockchain.utility.RawTranslator;
import config.Configuration;
import exception.BlockChainObjectParsingException;
import exception.FileCorruptionException;
import exception.InvalidBlockChainException;
import exception.InvalidBlockChainMessageException;
import general.utility.GeneralHelper;
import node.ConnectionManager;
import node.Message;
import node.PeerInfo;
import org.bouncycastle.operator.OperatorCreationException;
import general.security.SecurityHelper;
import rest.server.exception.BadRequest;
import rest.server.exception.NotFound;

import javax.net.ssl.*;


public class Validator {

    // authorities may have doubt on each other
    // medical organizations and patients fully trust every authorities
    // medical organizations and patients may have doubt on each other


    // receive block -> if requested and continuous to my current chain -> store and add block -> check if orphan block could be added and add if can
    //              -> if requested but not continuous to my current chain
    //               -> if not requested -> if better than the one requested -> store and add block, then, remove requested list
    //                                  -> if prev block not known -> request for header -> if likely to be better chain than mine and the currently requested one -> request for the blocks

    //better chain greater total score

    private static Validator runningValidator = null;


    private Logger blockChainLogger = Logger.getLogger(Validator.class.getName());

    // For validator
    private SSLServerSocketFactory sslServerSocketFactory;
    private SSLSocketFactory sslSocketFactory;
    private final ArrayList<MedicalOrgInfo> medicalOrgAuthorizationList = new ArrayList<>();
    private final SortedSet<byte[]> medicalOrgRevocationList = new TreeSet<>(new GeneralHelper.byteArrayComparator());
    private final ArrayList<PatientInfo> patientInfoList = new ArrayList<>();

    //for any full node
    private int port;
    private ECPrivateKey myPrivateKey;
    private ECPublicKey myPublicKey;
    private byte[] myIdentifier;
    private String myName;

    private final BlockChain myMainChain = new BlockChain();

    private SSLServerSocket serverSocket;
    private final ArrayList<PeerInfo> outBoundConnectionTobeRemoved = new ArrayList<>();
    private final ArrayList<PeerInfo> inBoundConnectionTobeRemoved = new ArrayList<>();
    private final LinkedHashMap<PeerInfo, ConnectionManager> outBoundConnectionList = new LinkedHashMap<>();
    private final LinkedHashMap<PeerInfo, ConnectionManager> inBoundConnectionList = new LinkedHashMap<>();


    private ArrayList<ArrayList<BlockHeader>> pendingHeadersList = new ArrayList<>();
    private ArrayList<BlockHeader> toBeRequestedHeaders = new ArrayList<>();
    private final ArrayList<BlockHeader> requestedHeaders = new ArrayList<>();
    private BlockHeader latestBlockHeaderRequested = null;
    private HashMap<BlockHeader, RequestInfo> requestedHeaderMap = new HashMap<>(); // for re-transmission
    private ArrayList<InetAddress> potentialPeerPool = new ArrayList<>();

    private final ArrayList<Transaction> transactionPool = new ArrayList<>();
    private final ArrayList<Vote> myVotes = new ArrayList<>();
    private final ArrayList<Block> orphanBlockList = new ArrayList<>();

    private boolean isTerminated = false;

    private Random rand = new Random();

    private long lastBroadCastHeadersRequestTime = -1;

    // Lock
    private static final ReentrantReadWriteLock chainInfoFilesLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock myChainLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock pendingHeadersListLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock toBeRequestedHeadersLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock requestedHeaderLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock orphanBlockLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock transactionLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock potentialPeerLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock myVotesLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock registrationLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock authorizationLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock revocationLock = new ReentrantReadWriteLock();


    private class RequestInfo {

        long timeRequested;
        long timeFirstRequested;
        int sentToIndex;

        private RequestInfo(int sentToIndex, long timeRequested) {
            this.sentToIndex = sentToIndex;
            this.timeRequested = timeRequested;
            this.timeFirstRequested = timeRequested;
        }
    }

    public static Validator create(KeyStore connectionKeyStore, char[] connectionKeyStorePassword, KeyStore signingKeyStore, char[] signingKeyStorePassword, int port, String logFileName) throws Exception {
        return new Validator(connectionKeyStore, connectionKeyStorePassword, signingKeyStore, signingKeyStorePassword
                , port, logFileName);
    }

    public static Validator getRunningValidator() {
        return runningValidator;
    }

    public Logger getBlockChainLogger() {
        return this.blockChainLogger;
    }


    public void start(InetAddress[] initialPeerNodes) throws Exception {

        if (runningValidator == null) {

            runningValidator = this;

            if (initialPeerNodes != null)
                connectWithPeers(initialPeerNodes);

            startAcceptingConnectionRequest(port);
            System.out.println("startAcceptingConnectionRequest called");//## for debug

            startRequestingPeerNodes();
            System.out.println("startRequestingPeerNodes called");//## for debug

            startRequestingConnection();
            System.out.println("startRequestingConnection called");//## for debug

            startRequestingBlocksAndBlockHeaders();
            System.out.println("startRequestingBlockHeaders called");//## for debug

            startValidation();
            System.out.println("startValidation called");//## for debug
        } else if (runningValidator != this) {
            throw new Exception("A validator is already running, please shutdown the running validator to start it");
        }
    }

    public boolean shutdown() {
        if (runningValidator == this) {
            System.out.println("shutdown from validator start");
            ReadLock readMyChainLock = myChainLock.readLock();
            readMyChainLock.lock();
            isTerminated = true;
            readMyChainLock.unlock();
            System.out.println("shutdown from validator end");
            runningValidator = null;
            return true;
        } else {
            return false;
        }
    }


    private Validator(KeyStore connectionKeyStore, char[] connectionKeyStorePassword, KeyStore signingKeyStore, char[] signingKeyStorePassword, int port, String logFileName) throws Exception {
        this.port = port;

        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) signingKeyStore.getEntry(Configuration.SIGNING_KEYSTORE_ALIAS, new KeyStore.PasswordProtection(signingKeyStorePassword));
        this.myPrivateKey = (ECPrivateKey) entry.getPrivateKey();
        this.myPublicKey = (ECPublicKey) entry.getCertificateChain()[0].getPublicKey();
        this.myIdentifier = BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(myPublicKey);
        this.myName = SecurityHelper.getSubjectCNFromX509Certificate((X509Certificate) entry.getCertificateChain()[0]);

        initializeSocketFactories(connectionKeyStore, connectionKeyStorePassword);
        System.out.println("Initialized SSL socket Factories");

        myMainChain.initializeChainWithBestChain(transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
        System.out.println("Chain successfully loaded.\nCurrent latest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore());//## for debug
        blockChainLogger.info("Chain successfully loaded.\nCurrent latest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore());


        FileHandler blockChainLogFileHandler = new FileHandler(logFileName, true);
        SimpleFormatter formatter = new SimpleFormatter();
        blockChainLogger.addHandler(blockChainLogFileHandler);
        blockChainLogger.setUseParentHandlers(false);
        blockChainLogFileHandler.setFormatter(formatter);

    }

    //cert chain = {cert for tls | cert of authority or medical org}
    private void initializeSocketFactories(KeyStore keyStore, char[] password) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {

                        ArrayList<Lock> usingLockList = new ArrayList<>();
                        ReadLock readMyChainLock = myChainLock.readLock();

                        try {
                            System.out.println("Start checking client's cert");

                            for (X509Certificate cert : certs) {
                                cert.checkValidity();
                            }

                            if (certs.length != 2)
                                throw new CertificateException("Not sufficient chain length");

                            if (certs[1].getPublicKey().equals(myPublicKey))
                                throw new CertificateException("It's me");

                            if (certs[0].getKeyUsage()[5])
                                throw new CertificateException("The certificate is for signing");

                            GeneralHelper.lockForMe(usingLockList, readMyChainLock);
                            byte[] issuedAuthorityIdentifier = SecurityHelper.getIssuerIdentifierFromX509Cert(certs[1]);
                            boolean hasIssuedAuthority = myMainChain.hasAuthority(issuedAuthorityIdentifier);
                            AuthorityInfo issuerInfo = null;
                            if (hasIssuedAuthority)
                                issuerInfo = myMainChain.getAuthority(issuedAuthorityIdentifier);
                            GeneralHelper.unLockForMe(usingLockList);

                            if (issuerInfo != null) {
                                try {
                                    certs[1].verify(issuerInfo.getPublicKey());
                                } catch (SignatureException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException e) {
                                    throw new CertificateException("The signing cert isn't issued by the issuer");
                                }

                                try {
                                    certs[0].verify(certs[1].getPublicKey());
                                } catch (SignatureException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException e) {
                                    throw new CertificateException("The connection cert isn't issued by the issuer");
                                }

                            } else
                                throw new CertificateException("The signing cert's issuer is not a valid authority");


                            System.out.println("Finished checking client's cert");
                        } catch (CertificateException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new CertificateException("Not valid chain");
                        } finally {
                            GeneralHelper.unLockForMe(usingLockList);
                        }
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {

                        ArrayList<Lock> usingLockList = new ArrayList<>();
                        ReadLock readMyChainLock = myChainLock.readLock();

                        try {
                            System.out.println("Start checking client's cert");

                            for (X509Certificate cert : certs) {
                                cert.checkValidity();
                            }

                            if (certs.length != 2)
                                throw new CertificateException("Not sufficient chain length");

                            if (certs[1].getPublicKey().equals(myPublicKey))
                                throw new CertificateException("It's me");

                            if (certs[0].getKeyUsage()[5])
                                throw new CertificateException("The certificate is for signing");

                            GeneralHelper.lockForMe(usingLockList, readMyChainLock);
                            byte[] issuedAuthorityIdentifier = SecurityHelper.getIssuerIdentifierFromX509Cert(certs[1]);
                            boolean hasIssuedAuthority = myMainChain.hasAuthority(issuedAuthorityIdentifier);
                            AuthorityInfo issuerInfo = null;
                            if (hasIssuedAuthority)
                                issuerInfo = myMainChain.getAuthority(issuedAuthorityIdentifier);
                            GeneralHelper.unLockForMe(usingLockList);

                            if (issuerInfo != null) {
                                try {
                                    certs[1].verify(issuerInfo.getPublicKey());
                                } catch (SignatureException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException e) {
                                    throw new CertificateException("The signing cert isn't issued by the issuer");
                                }

                                try {
                                    certs[0].verify(certs[1].getPublicKey());
                                } catch (SignatureException | NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException e) {
                                    throw new CertificateException("The connection cert isn't issued by the issuer");
                                }

                            } else
                                throw new CertificateException("The signing cert's issuer is not a valid authority");

                            System.out.println("Finished checking client's cert");
                        } catch (CertificateException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new CertificateException("Not valid chain");
                        } finally {
                            GeneralHelper.unLockForMe(usingLockList);
                        }
                    }
                }
        };

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, password);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

        sslServerSocketFactory = sslContext.getServerSocketFactory();
        sslSocketFactory = sslContext.getSocketFactory();
    }


    private void removeFromConnectionList(PeerInfo peerInfo) {
        if (peerInfo == null)
            return;
        outBoundConnectionTobeRemoved.remove(peerInfo);
        inBoundConnectionTobeRemoved.remove(peerInfo);

        ConnectionManager connectionManager = null;
        if (inBoundConnectionList.containsKey(peerInfo)) {
            connectionManager = inBoundConnectionList.remove(peerInfo);
            connectionManager.close();
        } else if (outBoundConnectionList.containsKey(peerInfo)) {
            connectionManager = outBoundConnectionList.remove(peerInfo);
            connectionManager.close();
        }

        if (connectionManager != null) {

            blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Successful disconnection");
            System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Successful disconnection"); //## for debug
        }
    }

    private void connectWithPeers(InetAddress[] peers) {


        for (InetAddress peer : peers) {
            try {
                SSLSocket newConnection = (SSLSocket) sslSocketFactory.createSocket(peer, Configuration.NODE_SERVER_PORT);
                newConnection.setEnabledCipherSuites(Configuration.CONNECTION_TLS_CIPHER_SUITE);
                newConnection.setSoTimeout(5000);
                newConnection.addHandshakeCompletedListener((ex) -> {
                            verifyAndHandleConnection(newConnection, true);
                        }
                );
                newConnection.startHandshake();

            } catch (IOException e) {
                blockChainLogger.info(peer.getHostAddress() + ": Not responding");
                System.out.println(peer.getHostAddress() + ": Not responding"); //for debug
            }

        }


    }

    private void startAcceptingConnectionRequest(int server_port) throws IOException {


        serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(server_port, 500);
        serverSocket.setEnabledCipherSuites(Configuration.CONNECTION_TLS_CIPHER_SUITE);
        serverSocket.setNeedClientAuth(true);
        new Thread(() -> {
            while (!isTerminated) {
                try {
                    SSLSocket newConnection = (SSLSocket) serverSocket.accept();
                    newConnection.setSoTimeout(5000);
                    newConnection.addHandshakeCompletedListener((ex) -> {
                        verifyAndHandleConnection(newConnection, false);
                    });
                    newConnection.startHandshake();
                } catch (IOException e) {
                    blockChainLogger.info("Server Socket accept error");
                    e.printStackTrace(); //for debug
                }
            }
        }).start();

    }

    private void verifyAndHandleConnection(SSLSocket newConnection, boolean isOutBound) {
        new Thread(() -> {

            PeerInfo peerInfo = null;
            WriteLock writeConnectionLock = connectionLock.writeLock();
            ReadLock readMyChainLock = myChainLock.readLock();
            ArrayList<Lock> usingLockList = new ArrayList<>();

            try (newConnection) {

                ECPublicKey peerPublicKey = (ECPublicKey) newConnection.getSession().getPeerCertificates()[1].getPublicKey();
                byte[] connectionRequesterIdentifier = BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(peerPublicKey);

                if (Arrays.equals(connectionRequesterIdentifier, myIdentifier))
                    return;

                byte[] issuerKeyIdentifier = SecurityHelper.getIssuerIdentifierFromX509Cert((X509Certificate) newConnection.getSession().getPeerCertificates()[1]);


                GeneralHelper.lockForMe(usingLockList, readMyChainLock, writeConnectionLock);

                // signature checked during handshake
                if (!myMainChain.hasAuthority(issuerKeyIdentifier))
                    return;

                boolean isAuthorizedMedicalOrg = myMainChain.hasMedicalOrg(connectionRequesterIdentifier);
                boolean isAuthority = Arrays.equals(issuerKeyIdentifier, connectionRequesterIdentifier);
                Message myStatusMessage = new Message(Configuration.MESSAGE_STATUS, myMainChain.getCurrentStatus().getRaw());

                peerInfo = new PeerInfo(connectionRequesterIdentifier, issuerKeyIdentifier);

                if (checkIfConnectionExists(peerInfo)) {
                    GeneralHelper.unLockForMe(usingLockList);
                    newConnection.close();
                    blockChainLogger.info(newConnection.getInetAddress().getHostAddress() + ": Connection already exists");
                    System.out.println(newConnection.getInetAddress().getHostAddress() + ": Connection already exists"); //for debug
                    return;
                }

                boolean isConfirmed;
                if (!(isAuthorizedMedicalOrg || isAuthority)) {
                    isConfirmed = false;
                    if (System.currentTimeMillis() - myMainChain.getTimeStampForValidatorSyncCheck(myIdentifier) < Configuration.SYNC_PERIOD) {
                        GeneralHelper.unLockForMe(usingLockList);
                        newConnection.close();
                        blockChainLogger.info(newConnection.getInetAddress().getHostAddress() + ": Neither authorized medical org nor trusted authority");
                        System.out.println(newConnection.getInetAddress().getHostAddress() + ": Neither authorized medical org nor trusted authority"); //for debug
                        return;
                    }
                } else {
                    isConfirmed = true;
                }

                ConnectionManager newConnectionManager = new ConnectionManager(newConnection, isConfirmed);
                if (!isOutBound) {
                    inBoundConnectionList.put(peerInfo, newConnectionManager);
                } else {
                    outBoundConnectionList.put(peerInfo, newConnectionManager);
                }

                GeneralHelper.unLockForMe(usingLockList);


                System.out.println("Connected to " + newConnectionManager.getSocket().getInetAddress().getHostAddress());

                Status peerStatus = null;
                if (isOutBound) {
                    newConnectionManager.write(myStatusMessage);
                    blockChainLogger.info("Sent status: " + newConnectionManager.getSocket().getInetAddress().getHostAddress());
                    System.out.println("Sent status to " + newConnectionManager.getSocket().getInetAddress().getHostAddress()); //## debug
                    peerStatus = (Status) newConnectionManager.read().parse();
                    blockChainLogger.info("Received status: " + newConnectionManager.getSocket().getInetAddress().getHostAddress());
                    System.out.println("Received status from " + newConnectionManager.getSocket().getInetAddress().getHostAddress()); //## debug

                } else {
                    peerStatus = (Status) newConnectionManager.read().parse();
                    blockChainLogger.info("Received status: " + newConnectionManager.getSocket().getInetAddress().getHostAddress());
                    System.out.println("Received status: " + newConnectionManager.getSocket().getInetAddress().getHostAddress()); //## debug
                    newConnectionManager.write(myStatusMessage);
                    blockChainLogger.info("Sent status: " + newConnectionManager.getSocket().getInetAddress().getHostAddress());
                    System.out.println("Sent status to " + newConnectionManager.getSocket().getInetAddress().getHostAddress()); //## debug
                }

                newConnectionManager.getSocket().setSoTimeout(0);
                startHandleConnection(newConnectionManager, peerInfo, peerStatus, isOutBound);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                GeneralHelper.unLockForMe(usingLockList);
            }


            GeneralHelper.lockForMe(usingLockList, writeConnectionLock);

            removeFromConnectionList(peerInfo);

            GeneralHelper.unLockForMe(usingLockList);

        }).start();
    }

    // if exception, just disconnect
    private void startHandleConnection(ConnectionManager connectionManager, PeerInfo peerInfo, Status peerStatus, boolean isOutBound) {

        /*Message number
         * 0: status
         * 1: peer nodes list request
         * 2: block header request - byte[][] hash locator - from greater height to smaller height - dense to sparse
         * 3: block request - content =byte[] blockHash
         * 4: peer node list
         * 5: block header list - from smaller height to greater height
         * 6: transaction
         * 7: block
         * */


        //chain info

        WriteLock writeChainInfoFilesLock = chainInfoFilesLock.writeLock();

        //chain
        ReadLock readMyChainLock = myChainLock.readLock();
        WriteLock writeMyChainLock = myChainLock.writeLock();

        //potential peer
        WriteLock writePotentialPeerLock = potentialPeerLock.writeLock();

        //transaction
        WriteLock writeTransactionLock = transactionLock.writeLock();

        //pending header
        WriteLock writePendingHeadersLock = pendingHeadersListLock.writeLock();

        //to be requested header
        WriteLock writePendingHeaderLock = toBeRequestedHeadersLock.writeLock();

        //requested header
        WriteLock writeRequestedHeaderLock = requestedHeaderLock.writeLock();

        //orphan
        WriteLock writeOrphanBlockLock = orphanBlockLock.writeLock();

        //vote
        WriteLock writeVoteLock = myVotesLock.writeLock();

        //registration
        WriteLock writeRegistrationLock = registrationLock.writeLock();

        //authorization
        WriteLock writeAuthorizationLock = authorizationLock.writeLock();

        //revocation
        WriteLock writeRevocationLock = revocationLock.writeLock();

        //connection
        ReadLock readConnectionLock = connectionLock.readLock();


        System.out.println("startHandleConnection entered"); //debug


        ArrayList<Lock> usingLockList = new ArrayList<>();
        try {
            //send peer list right after connection establishment

            GeneralHelper.lockForMe(usingLockList, readConnectionLock);

            InetAddress[] peerAddresses = getPeerList(peerInfo);

            if (peerAddresses != null && peerAddresses.length != 0)
                connectionManager.write(new Message(Configuration.MESSAGE_PEER_NODE_LIST, RawTranslator.translateAddressesToBytes(peerAddresses)));

            //check if pool reached max
            if (!isOutBound) {
                if (inBoundConnectionList.size() - inBoundConnectionTobeRemoved.size() > Configuration.MAX_IN_BOUND_CONNECTION) {
                    inBoundConnectionTobeRemoved.add(peerInfo);
                    blockChainLogger.info("Maximum connection exceeded");
                    GeneralHelper.unLockForMe(usingLockList);
                    return;
                }
            }
            GeneralHelper.unLockForMe(usingLockList);


            System.out.println("My status:\n Score: " + myMainChain.getCurrentStatus().getTotalScore() + "\nLatest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getCurrentStatus().getLatestBlockHash())); //debug

            System.out.println("Peer status:\n Score: " + peerStatus.getTotalScore() + "\nLatest block hash: " + GeneralHelper.bytesToStringHex(peerStatus.getLatestBlockHash())); //debug

            if (peerStatus.getTotalScore() > myMainChain.getTotalScore()) {
                connectionManager.write(new Message(Configuration.MESSAGE_HEADER_REQUEST, myMainChain.getCurrentChainHashLocator(null, Configuration.MAX_HASH_LOCATOR_LENGTH)));
            }
            GeneralHelper.unLockForMe(usingLockList);


            String peerAddressString = connectionManager.getSocket().getInetAddress().getHostAddress();

            while (!isTerminated) {

                if (!connectionManager.isConfirmed()
                        && System.currentTimeMillis() - myMainChain.getTimeStampForValidatorSyncCheck(myIdentifier) < Configuration.SYNC_PERIOD)
                    break;

                Message message = connectionManager.read();
                int messageNumber = message.number;
                Object parsedContent;
                try {
                    parsedContent = message.parse();
                } catch (BlockChainObjectParsingException ps) {
                    ps.printStackTrace();
                    throw new InvalidBlockChainMessageException(peerAddressString, messageNumber);
                }

                System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": message " + messageNumber + " received"); //debug

                switch (messageNumber) {

                    case Configuration.MESSAGE_PEER_NODE_REQUEST: // peer node request
                        blockChainLogger.info(peerAddressString + ": Received peer node list request");
                        connectionManager.write(new Message(Configuration.MESSAGE_PEER_NODE_LIST, RawTranslator.translateAddressesToBytes(getPeerList(peerInfo))));
                        blockChainLogger.info(peerAddressString + ": Sent peer node list request");
                        break;

                    case Configuration.MESSAGE_HEADER_REQUEST: // block headers request
                        blockChainLogger.info(peerAddressString + ": Received block headers request");
                        if (!connectionManager.isConfirmed())
                            break;

                        GeneralHelper.lockForMe(usingLockList, readMyChainLock);
                        byte[][] hashLocator = (byte[][]) parsedContent;
                        if (Arrays.equals(myMainChain.getLatestBlockHash(), hashLocator[0])) {
                            GeneralHelper.unLockForMe(usingLockList);
                            break;
                        }

                        ByteArrayOutputStream headers = new ByteArrayOutputStream();
                        for (byte[] hash : hashLocator) {
                            ChainInfo info = ChainInfoManager.load(hash);
                            if (info == null)
                                continue;
                            if (info.isBestChain()) {
                                int i = 0;
                                headers.write((byte) 0);
                                byte[] curBlockHash = info.getNextBlockHash();
                                while (curBlockHash != null && i <= Configuration.MAX_HEADER_NUMBER_PER_REQUEST) {
                                    headers.write(BlockManager.loadBlockHeader(curBlockHash).getRaw());
                                    curBlockHash = ChainInfoManager.load(curBlockHash).getNextBlockHash();
                                    ++i;
                                }
                                break;
                            }
                        }

                        GeneralHelper.unLockForMe(usingLockList);

                        if (headers.size() == 0) {
                            headers.write((byte) 1);
                            headers.write(hashLocator[hashLocator.length - 1]);
                        }

                        connectionManager.write(new Message(Configuration.MESSAGE_HEADER_REQUEST_REPLY, headers.toByteArray()));
                        blockChainLogger.info(peerAddressString + ": Sent block headers");
                        break;

                    case Configuration.MESSAGE_BLOCK_REQUEST:  // block request
                        byte[] blockHash = (byte[]) parsedContent;

                        blockChainLogger.info(peerAddressString + ": Received block(" + GeneralHelper.bytesToStringHex(blockHash) + ") request");

                        if (!connectionManager.isConfirmed())
                            break;

                        Block block = BlockManager.loadBlock(blockHash);
                        if (block != null) {
                            connectionManager.write(new Message(Configuration.MESSAGE_BLOCK, block.getRaw()));
                            blockChainLogger.info(peerAddressString + ": Sent block(" + GeneralHelper.bytesToStringHex(blockHash) + ")");
                        }
                        break;

                    case Configuration.MESSAGE_PEER_NODE_LIST: //peer node list
                        InetAddress[] receivedAddresses = (InetAddress[])(parsedContent);
                        ArrayList<String> newPotentialAddressStrings = new ArrayList<>();

                        for(InetAddress receivedAddress: receivedAddresses)
                        {
                            newPotentialAddressStrings.add(receivedAddress.getHostAddress());
                        }

                        GeneralHelper.lockForMe(usingLockList, readConnectionLock, writePotentialPeerLock);

                        for (ConnectionManager inboundConnectionManager : inBoundConnectionList.values()) {
                            newPotentialAddressStrings.remove(inboundConnectionManager.getSocket().getInetAddress().getHostAddress());
                        }

                        for (ConnectionManager outboundConnectionManager : outBoundConnectionList.values()) {
                            newPotentialAddressStrings.remove(outboundConnectionManager.getSocket().getInetAddress().getHostAddress());
                        }

                        for (InetAddress potentialPeerAddress : potentialPeerPool) {
                            newPotentialAddressStrings.remove(potentialPeerAddress.getHostAddress());
                        }

                        if(!newPotentialAddressStrings.isEmpty()) {

                            for(String receivedAddressString: newPotentialAddressStrings)
                            {
                                potentialPeerPool.add(InetAddress.getByName(receivedAddressString));
                            }
                            blockChainLogger.info(peerAddressString + ": Added peer node list of size" + newPotentialAddressStrings.size());
                        }

                        GeneralHelper.unLockForMe(usingLockList);

                        break;

                    case Configuration.MESSAGE_HEADER_REQUEST_REPLY: //block headers

                        if (message.content[0] == 1) {
                            GeneralHelper.lockForMe(usingLockList, readMyChainLock);
                            byte[] lastSearchedHash = (byte[]) parsedContent;
                            byte[] newHashLocatorMerged = myMainChain.getCurrentChainHashLocator(lastSearchedHash, Configuration.MAX_HASH_LOCATOR_LENGTH);
                            if (newHashLocatorMerged == null) {
                                GeneralHelper.unLockForMe(usingLockList);
                                return;
                            }
                            connectionManager.write(new Message(Configuration.MESSAGE_HEADER_REQUEST, newHashLocatorMerged));
                        }
                        else {
                            BlockHeader[] receivedBlockHeaders = (BlockHeader[]) parsedContent;

                            blockChainLogger.info(peerAddressString + ": Received block headers");

                            GeneralHelper.lockForMe(usingLockList, readMyChainLock);


                            ArrayList<BlockHeader> blockHeaders = new ArrayList<>();


                            for (BlockHeader tempHeader : receivedBlockHeaders) {
                                if (!BlockChainManager.hasBlock(tempHeader.calculateHash())) {
                                    blockHeaders.add(tempHeader);
                                }
                            }

                            if (!blockHeaders.isEmpty()) {
                                int totalScore = myMainChain.isNextBlockHeader(blockHeaders.get(0)) ? myMainChain.checkHeaders(blockHeaders.toArray(new BlockHeader[0])) : BlockChainManager.checkBlockHeaders(blockHeaders.toArray(new BlockHeader[0]));
                                if (totalScore == -1) {
                                    throw new InvalidBlockChainException(peerAddressString);
                                }

                                GeneralHelper.lockForMe(usingLockList, writePendingHeadersLock);
                                pendingHeadersList.add(blockHeaders);
                            }

                            GeneralHelper.unLockForMe(usingLockList);
                        }
                        break;

                    case Configuration.MESSAGE_TRANSACTION:

                        Transaction transaction = (Transaction) parsedContent;

                        String transactionIdentifier = GeneralHelper.bytesToStringHex(transaction.calculateHash());

                        blockChainLogger.info(peerAddressString + ": Received transaction(" + transactionIdentifier + ")"); //# could be removed if too large

                        GeneralHelper.lockForMe(usingLockList, readMyChainLock, writeTransactionLock, readConnectionLock);

                        if (transactionPool.contains(transaction)) {
                            blockChainLogger.info("Existing transaction(" + transactionIdentifier + ") in the pool");
                        } else if (myMainChain.checkTransaction(transaction)) {
                            transactionPool.add(transaction);
                            blockChainLogger.info("Added transaction(" + transactionIdentifier + ") to the pool");
                            broadcastMessage(new Message(Configuration.MESSAGE_TRANSACTION, transaction.getRaw()), peerInfo);
                            blockChainLogger.info("Broadcasted transaction(" + transactionIdentifier + ")");
                        } else {
                            blockChainLogger.info("Invalid or existing transaction(" + transactionIdentifier + ") in the chain");
                        }

                        GeneralHelper.unLockForMe(usingLockList);

                        break;
                    case Configuration.MESSAGE_BLOCK:
                        Block block7 = (Block) parsedContent;

                        if (!Arrays.equals(block7.getHeader().getContentHash(), block7.getContent().calculateHash())) {
                            blockChainLogger.info(peerAddressString + ": Received an invalid block");
                            return;
                        }

                        blockChainLogger.info(peerAddressString + ": Received a block(" + GeneralHelper.bytesToStringHex(block7.calculateHash()) + ")\n Block number= " + block7.getHeader().getBlockNumber());

                        System.out.println("pass1"); //debug
                        GeneralHelper.lockForMe(usingLockList, writeChainInfoFilesLock, writeMyChainLock, writePendingHeaderLock, writeRequestedHeaderLock, writeOrphanBlockLock, writeTransactionLock,
                                readConnectionLock, writeVoteLock, writeRegistrationLock, writeAuthorizationLock, writeRevocationLock);

                        if (orphanBlockList.contains(block7) || myMainChain.hasBlock(block7) || BlockChainManager.hasBlock(block7.calculateHash())) {
                            GeneralHelper.unLockForMe(usingLockList);
                            break;
                        }


                        boolean requested = false;
                        System.out.println("pass2"); //debug


                        // check if it's requested block
                        if (toBeRequestedHeaders.size() != 0 || requestedHeaders.size() != 0) {
                            if (requestedHeaders.contains(block7.getHeader())) {
                                requested = true;
                                requestedHeaderMap.remove(block7.getHeader());
                                requestedHeaders.remove(block7.getHeader());
                            } else if (toBeRequestedHeaders.contains(block7.getHeader())) {
                                requested = true;
                                toBeRequestedHeaders.remove(block7.getHeader());
                            } else if (Arrays.equals(block7.getHeader().getPrevHash(), latestBlockHeaderRequested.calculateHash())) // if it is continuous to the one I have requested, put it into the orphan list
                            {
                                requested = true;
                                latestBlockHeaderRequested = block7.getHeader();
                            }
                        }


                        // if it's requested blocks
                        if (requested) {
                            // if it's next block
                            if (myMainChain.isNextBlock(block7)) {

                                // if it's invalid block (and invalid chain), disconnect and clear request lists
                                if (!myMainChain.checkNextBlock(block7, transactionPool)) {
                                    toBeRequestedHeaders.clear();
                                    requestedHeaderMap.clear();
                                    requestedHeaders.clear();
                                    orphanBlockList.clear();
                                    throw new InvalidBlockChainException(peerAddressString, block7);
                                }

                                BlockChainManager.storeBlock(block7);
                                AuthorityInfo unTrustedAuthorityInfo = myMainChain.addBlock(block7, transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                                if (unTrustedAuthorityInfo != null) {
                                    unTrustAuthority(unTrustedAuthorityInfo.getIdentifier());
                                }
                                if (block7.getContent().getMedicalOrgRevocationList() != null) {

                                    for (byte[] revokedIdentifier : block7.getContent().getMedicalOrgRevocationList())
                                        revokeMedicalOrg(revokedIdentifier, block7.getHeader().getValidatorIdentifier());
                                }
                                if (block7.getContent().getMedicalOrgAuthorizationList() != null) {
                                    for (MedicalOrgInfo medicalOrgInfo : block7.getContent().getMedicalOrgAuthorizationList())
                                        authorizeMedicalOrg(medicalOrgInfo.getIdentifier(), block7.getHeader().getValidatorIdentifier());
                                }

                                System.out.println("Stored and added block(" + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                        + ")\nCurrent total score: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber());//debug
                                blockChainLogger.info("Stored and added block(" + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                        + ")\nCurrent total score: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber());

                                if (System.currentTimeMillis() - block7.getHeader().getTimestamp() < Configuration.SYNC_PERIOD) {
                                    broadcastMessage(new Message(Configuration.MESSAGE_BLOCK, block7.getRaw()), peerInfo);
                                }


                                // process orphan blocks if possible
                                Block processedBlock = block7;
                                while (!orphanBlockList.isEmpty()) {
                                    if (Arrays.equals(orphanBlockList.get(0).getHeader().getPrevHash(), processedBlock.getHeader().calculateHash())) {

                                        if (myMainChain.checkNextBlock(orphanBlockList.get(0), transactionPool)) {

                                            processedBlock = orphanBlockList.get(0);
                                            orphanBlockList.remove(0);

                                            BlockChainManager.storeBlock(processedBlock);

                                            unTrustedAuthorityInfo = myMainChain.addBlock(processedBlock, transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                                            if (unTrustedAuthorityInfo != null) {
                                                unTrustAuthority(unTrustedAuthorityInfo.getIdentifier());
                                            }
                                            if (processedBlock.getContent().getMedicalOrgRevocationList() != null) {

                                                for (byte[] revokedIdentifier : processedBlock.getContent().getMedicalOrgRevocationList())
                                                    revokeMedicalOrg(revokedIdentifier, processedBlock.getHeader().getValidatorIdentifier());
                                            }
                                            if (processedBlock.getContent().getMedicalOrgAuthorizationList() != null) {
                                                for (MedicalOrgInfo medicalOrgInfo : processedBlock.getContent().getMedicalOrgAuthorizationList())
                                                    authorizeMedicalOrg(medicalOrgInfo.getIdentifier(), processedBlock.getHeader().getValidatorIdentifier());
                                            }


                                            System.out.println("Stored and added orphan block(" + GeneralHelper.bytesToStringHex(processedBlock.calculateHash())
                                                    + ")\nCurrent total score: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber());//debug
                                            blockChainLogger.info("Stored and added orphan block(" + GeneralHelper.bytesToStringHex(processedBlock.calculateHash())
                                                    + ")\nCurrent total score: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber());

                                            if (System.currentTimeMillis() - processedBlock.getHeader().getTimestamp() < Configuration.SYNC_PERIOD) {
                                                broadcastMessage(new Message(Configuration.MESSAGE_BLOCK, processedBlock.getRaw()), peerInfo);
                                            }

                                        } else {
                                            toBeRequestedHeaders.clear();
                                            requestedHeaderMap.clear();
                                            requestedHeaders.clear();
                                            orphanBlockList.clear();
                                            throw new InvalidBlockChainException(peerAddressString, orphanBlockList.get(0));
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            } else if (BlockChainManager.hasBlock(block7.getHeader().getPrevHash())) {

                                // if it's invalid block (and invalid chain), disconnect and clear request lists
                                if (!BlockChainManager.checkBlock(block7)) {
                                    toBeRequestedHeaders.clear();
                                    requestedHeaderMap.clear();
                                    requestedHeaders.clear();
                                    orphanBlockList.clear();
                                    throw new InvalidBlockChainException(peerAddressString, block7);
                                }

                                int newTotalScore = BlockChainManager.storeBlock(block7);
                                System.out.println("Stored block(" + GeneralHelper.bytesToStringHex(block7.calculateHash())
                                        + ")\nBlock number: " + block7.getHeader().getBlockNumber());//debug
                                blockChainLogger.info("Stored block(" + GeneralHelper.bytesToStringHex(block7.calculateHash())
                                        + ")\nBlock number: " + block7.getHeader().getBlockNumber());

                                Block processed = block7;

                                while (!orphanBlockList.isEmpty()) {
                                    if (Arrays.equals(orphanBlockList.get(0).getHeader().getPrevHash(), processed.getHeader().calculateHash())) {
                                        if (BlockChainManager.checkBlock(orphanBlockList.get(0))) {
                                            processed = orphanBlockList.get(0);
                                            orphanBlockList.remove(0);

                                            newTotalScore = BlockChainManager.storeBlock(processed);

                                            System.out.println("Stored orphan block(" + GeneralHelper.bytesToStringHex(processed.calculateHash())
                                                    + ")\nBlock number: " + processed.getHeader().getBlockNumber());//debug
                                            blockChainLogger.info("Stored orphan block(" + GeneralHelper.bytesToStringHex(processed.calculateHash())
                                                    + ")\nBlock number: " + processed.getHeader().getBlockNumber());

                                            if (System.currentTimeMillis() - processed.getHeader().getTimestamp() < Configuration.SYNC_PERIOD) {
                                                broadcastMessage(new Message(Configuration.MESSAGE_BLOCK, processed.getRaw()), peerInfo);
                                            }


                                        } else {
                                            toBeRequestedHeaders.clear();
                                            requestedHeaderMap.clear();
                                            requestedHeaders.clear();
                                            orphanBlockList.clear();
                                            throw new InvalidBlockChainException(peerAddressString, orphanBlockList.get(0));
                                        }
                                    } else {
                                        break;
                                    }
                                }

                                System.out.println("pass6"); //debug


                                if (newTotalScore > myMainChain.getTotalScore()) {
                                    System.out.println("pass7"); //debug

                                    myMainChain.loadCurrentBest(transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                                    refreshConnection(); // disconnect/connect based on current authority list and medical org list

                                    System.out.println("Changed Chain.\nCurrent latest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                            + "\nScore: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber()); //debug
                                    blockChainLogger.info("Changed Chain.\nCurrent latest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                            + "\nScore: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber());

                                }

                                System.out.println("pass8"); //debug


                            } else {

                                orphanBlockList.add(block7);
                                Collections.sort(orphanBlockList, new GeneralHelper.blockComparator());

                                System.out.println("Orphan block(" + GeneralHelper.bytesToStringHex(block7.calculateHash())
                                        + ")\nBlock number: " + block7.getHeader().getBlockNumber());//debug
                                blockChainLogger.info("Orphan block(" + GeneralHelper.bytesToStringHex(block7.calculateHash())
                                        + ")\nBlock number: " + block7.getHeader().getBlockNumber());

                            }
                        } else //if it's not a requested block
                        {
                            //if it's the next block
                            if (myMainChain.isNextBlock(block7)) {

                                //if it's invalid block, disconnect
                                if (!myMainChain.checkNextBlock(block7, transactionPool)) {
                                    throw new InvalidBlockChainException(peerAddressString, block7);
                                }


                                BlockChainManager.storeBlock(block7);
                                AuthorityInfo unTrustedAuthorityInfo = myMainChain.addBlock(block7, transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                                if (unTrustedAuthorityInfo != null) {
                                    unTrustAuthority(unTrustedAuthorityInfo.getIdentifier());
                                }
                                if (block7.getContent().getMedicalOrgRevocationList() != null) {

                                    for (byte[] revokedIdentifier : block7.getContent().getMedicalOrgRevocationList())
                                        revokeMedicalOrg(revokedIdentifier, block7.getHeader().getValidatorIdentifier());
                                }
                                if (block7.getContent().getMedicalOrgAuthorizationList() != null) {
                                    for (MedicalOrgInfo medicalOrgInfo : block7.getContent().getMedicalOrgAuthorizationList())
                                        authorizeMedicalOrg(medicalOrgInfo.getIdentifier(), block7.getHeader().getValidatorIdentifier());
                                }

                                System.out.println("Stored and added block(" + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                        + ")\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore());//debug
                                blockChainLogger.info("Stored and added block(" + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                        + ")\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore());

                                if (System.currentTimeMillis() - block7.getHeader().getTimestamp() < Configuration.SYNC_PERIOD) {
                                    broadcastMessage(new Message(Configuration.MESSAGE_BLOCK, block7.getRaw()), peerInfo);
                                }


                            } else if (BlockChainManager.hasBlock(block7.getHeader().getPrevHash())) {

                                //if it's invalid block, disconnect
                                if (!BlockChainManager.checkBlock(block7)) {
                                    throw new InvalidBlockChainException(peerAddressString, block7);
                                }

                                int processingTotal = StateInfoManager.load(block7.getHeader().getPrevHash()).getTotalScore() + block7.getHeader().getScore();

                                //if I have previous block, check its chain's score and if it is greater than that of requested one, just use it

                                if (processingTotal > myMainChain.getTotalScore()) {

                                    BlockChainManager.storeBlock(block7);

                                    myMainChain.loadCurrentBest(transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                                    refreshConnection(); // disconnect/connect based on current authority list and medical org list

                                    System.out.println("Changed Chain.\nCurrent latest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                            + "\nScore: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber()); //debug
                                    blockChainLogger.info("Changed Chain.\nCurrent latest block hash: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                            + "\nScore: " + myMainChain.getTotalScore() + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber());


                                }
                            } else {
                                // not requested and I don't hold its prevblock => send blockheaders request
                                connectionManager.write(new Message(Configuration.MESSAGE_HEADER_REQUEST, myMainChain.getCurrentChainHashLocator(null, Configuration.MAX_HASH_LOCATOR_LENGTH)));
                            }
                        }

                        GeneralHelper.unLockForMe(usingLockList);

                        break;
                    default:
                        throw new InvalidBlockChainMessageException(peerAddressString, messageNumber);

                }

            }
        } catch (SocketException | EOFException se) {
            //just connection disconnected
        } catch (InvalidBlockChainException | InvalidBlockChainMessageException bme) {
            bme.printStackTrace();
            blockChainLogger.info(bme.getMessage());
        } catch (BlockChainObjectParsingException | FileCorruptionException ps) {
            ps.printStackTrace();
            blockChainLogger.info(ps.getMessage());
            shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            GeneralHelper.unLockForMe(usingLockList);
        }
    }

    private void refreshConnection() throws IOException, BlockChainObjectParsingException, FileCorruptionException {
        for (PeerInfo peerInfo : inBoundConnectionList.keySet()) {
            if (inBoundConnectionList.get(peerInfo).isClosed())
                continue;

            if (inBoundConnectionList.get(peerInfo).isConfirmed()) {
                if (!myMainChain.hasAuthority(peerInfo.getIssuerIdentifier())) {
                    unTrustAuthority(peerInfo.getIssuerIdentifier());
                } else if (!peerInfo.isAuthority() && !myMainChain.hasMedicalOrg(peerInfo.getPeerIdentifier())) {
                    revokeMedicalOrg(peerInfo.getPeerIdentifier(), peerInfo.getIssuerIdentifier());

                }
            } else {
                if (!myMainChain.hasAuthority(peerInfo.getIssuerIdentifier())) {
                    unTrustAuthority(peerInfo.getIssuerIdentifier());
                } else if (myMainChain.hasMedicalOrg(peerInfo.getPeerIdentifier())) {
                    authorizeMedicalOrg(peerInfo.getPeerIdentifier(), peerInfo.getIssuerIdentifier());
                }
            }

        }

        for (PeerInfo peerInfo : outBoundConnectionList.keySet()) {
            if (outBoundConnectionList.get(peerInfo).isClosed())
                continue;

            if (outBoundConnectionList.get(peerInfo).isConfirmed()) {
                if (!myMainChain.hasAuthority(peerInfo.getIssuerIdentifier())) {
                    unTrustAuthority(peerInfo.getIssuerIdentifier());
                } else if (!peerInfo.isAuthority() && !myMainChain.hasMedicalOrg(peerInfo.getPeerIdentifier())) {
                    revokeMedicalOrg(peerInfo.getPeerIdentifier(), peerInfo.getIssuerIdentifier());
                }
            } else {
                if (!myMainChain.hasAuthority(peerInfo.getIssuerIdentifier())) {
                    unTrustAuthority(peerInfo.getIssuerIdentifier());
                } else if (myMainChain.hasMedicalOrg(peerInfo.getPeerIdentifier())) {
                    authorizeMedicalOrg(peerInfo.getPeerIdentifier(), peerInfo.getIssuerIdentifier());
                }
            }
        }

    }


    private void startRequestingPeerNodes() {
        new Thread(() -> {
            while (!isTerminated) {

                // get peer nodes list of the peer nodes and attempt to connect with the nodes
                // that are not connected with it

                ReadLock readConnectionLock = connectionLock.readLock();
                ReadLock readPotentialPeerLock = potentialPeerLock.readLock();
                ArrayList<Lock> usingLockList = new ArrayList<>();

                GeneralHelper.lockForMe(usingLockList, readConnectionLock, readPotentialPeerLock);

                System.out.println("startRequestingPeerNodes entered"); //debug

                int diff = Configuration.MAX_OUT_BOUND_CONNECTION - outBoundConnectionList.size();
                if (potentialPeerPool.size() < diff) {
                    broadcastMessage(new Message(Configuration.MESSAGE_PEER_NODE_REQUEST, null), null);
                    blockChainLogger.info("Sending request for other nodes' peers list");
                }

                GeneralHelper.unLockForMe(usingLockList);

                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("startRequestingPeerNodes terminated"); //debug
        }).start();
    }


    private void startRequestingConnection() {
        new Thread(() -> {
            ArrayList<Lock> usingLockList = new ArrayList<>();
            try {
                while (!isTerminated) {
                    // get peer nodes list of the peer nodes and attempt to connect with the nodes
                    // that are not connected with it


                    ReadLock readConnectionLock = connectionLock.readLock();
                    WriteLock writePotentialPeerLock = potentialPeerLock.writeLock();

                    GeneralHelper.lockForMe(usingLockList, readConnectionLock, writePotentialPeerLock);

                    System.out.println("startRequestingConnection entered"); //debug

                    int diff = Configuration.MAX_OUT_BOUND_CONNECTION - outBoundConnectionList.size();
                    int max = potentialPeerPool.size();
                    InetAddress[] potentialPeer = potentialPeerPool.subList(0, diff > max ? max : diff).toArray(new InetAddress[0]);

                    for (InetAddress address : potentialPeer)
                        potentialPeerPool.remove(address);


                    GeneralHelper.unLockForMe(usingLockList);

                    connectWithPeers(potentialPeer);

                    Thread.sleep(20000);

                }

            } catch (Exception e) {
                e.printStackTrace();
                shutdown();
            }
            finally {
                System.out.println("startRequestingConnection terminated"); //debug
                GeneralHelper.unLockForMe(usingLockList);
            }
        }).start();
    }

    private void startRequestingBlocksAndBlockHeaders() {
        new Thread(() -> {
            ArrayList<Lock> usingLockList = new ArrayList<>();
            try {
                while (!isTerminated) {

                    ReadLock readMyChainLock = myChainLock.readLock();
                    WriteLock writePendingHeadersLock = pendingHeadersListLock.writeLock();
                    WriteLock writeToBeRequestedHeadersLock = toBeRequestedHeadersLock.writeLock();
                    WriteLock writeRequestedHeaderLock = requestedHeaderLock.writeLock();
                    ReadLock readConnectionLock = connectionLock.readLock();

                    GeneralHelper.lockForMe(usingLockList, writePendingHeadersLock, writeToBeRequestedHeadersLock, writeRequestedHeaderLock, readConnectionLock);
                    if (!toBeRequestedHeaders.isEmpty() || !requestedHeaders.isEmpty()) {
                        for (BlockHeader blockHeader : requestedHeaders) {
                            RequestInfo requestInfo = requestedHeaderMap.get(blockHeader);

                            if (System.currentTimeMillis() - requestInfo.timeFirstRequested > Configuration.MAXIMUM_RESPONSE_WAITING_TIME) {
                                toBeRequestedHeaders.clear();
                                requestedHeaderMap.clear();
                                requestedHeaders.clear();
                                orphanBlockList.clear();
                                break;
                            }

                            if (System.currentTimeMillis() - requestInfo.timeRequested > Configuration.BLOCK_REQUEST_TIME_OUT) {
                                System.out.println("re-sent request for block number:" + blockHeader.getBlockNumber());
                                int newIndex = unicastMessage(new Message(Configuration.MESSAGE_BLOCK_REQUEST, blockHeader.calculateHash()), requestInfo.sentToIndex + 1);
                                requestInfo.sentToIndex = newIndex;
                                requestInfo.timeRequested = System.currentTimeMillis();
                            }
                        }

                        if (requestedHeaderMap.size() < Configuration.NUM_OF_BLOCK_REQUEST_AT_ONCE) {
                            int maxPossibleRequest = Configuration.NUM_OF_BLOCK_REQUEST_AT_ONCE - requestedHeaderMap.size();
                            int wantRequest = toBeRequestedHeaders.size();
                            int requestSize = (wantRequest > maxPossibleRequest) ? maxPossibleRequest : wantRequest;
                            if (requestSize != 0) {
                                for (int i = 0; i < requestSize; ++i) {
                                    System.out.println("sent request for block number:" + toBeRequestedHeaders.get(i).getBlockNumber());
                                    int index = randomUnicastMessage(new Message(Configuration.MESSAGE_BLOCK_REQUEST, toBeRequestedHeaders.get(i).calculateHash()));

                                    requestedHeaders.add(toBeRequestedHeaders.get(i));
                                    requestedHeaderMap.put(toBeRequestedHeaders.get(i), new RequestInfo(index, System.currentTimeMillis()));
                                }

                                if (requestSize > 0) {
                                    toBeRequestedHeaders.subList(0, requestSize).clear();
                                }

                            }

                        }
                    } else {
                        if (!pendingHeadersList.isEmpty()) {
                            while (!pendingHeadersList.isEmpty()) {
                                ArrayList<BlockHeader> tempToBeRequested = new ArrayList<>();
                                for (BlockHeader header : pendingHeadersList.get(0)) {
                                    if (!BlockChainManager.hasBlock(header.calculateHash()))
                                        tempToBeRequested.add(header);
                                }
                                pendingHeadersList.remove(0);
                                if (!tempToBeRequested.isEmpty()) {
                                    toBeRequestedHeaders.addAll(tempToBeRequested);

                                    latestBlockHeaderRequested = tempToBeRequested.get(tempToBeRequested.size() - 1);
                                    blockChainLogger.info("Requested blocks last block=" + GeneralHelper.bytesToStringHex(latestBlockHeaderRequested.calculateHash())
                                            + "\nBlock Number: " + latestBlockHeaderRequested.getBlockNumber());
                                    break;
                                }
                            }
                        } else {
                            GeneralHelper.unLockForMe(usingLockList);

                            GeneralHelper.lockForMe(usingLockList, readMyChainLock);
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastBroadCastHeadersRequestTime > Configuration.MAXIMUM_RESPONSE_WAITING_TIME && currentTime - myMainChain.getTimeStampForValidatorSyncCheck(myIdentifier) > Configuration.SYNC_PERIOD) {
                                lastBroadCastHeadersRequestTime = currentTime;
                                byte[] hashLocatorMerged =myMainChain.getCurrentChainHashLocator(null, Configuration.MAX_HASH_LOCATOR_LENGTH);
                                GeneralHelper.lockForMe(usingLockList, readConnectionLock);
                                broadcastMessage(new Message(Configuration.MESSAGE_HEADER_REQUEST, hashLocatorMerged), null);
                            }
                        }
                    }

                    GeneralHelper.unLockForMe(usingLockList);


                    Thread.sleep(2000);

                }
            } catch (InterruptedException | BlockChainObjectParsingException | IOException e) {
                e.printStackTrace();
                shutdown();
            } finally {
                System.out.println("startRequestingConnection terminated"); //debug
                GeneralHelper.unLockForMe(usingLockList);
            }
        }).start();
    }

    private void startValidation() {
        new Thread(() -> {
            ArrayList<Lock> usingLockList = new ArrayList<>();
            WriteLock writeChainInfoFilesLock = chainInfoFilesLock.writeLock();
            WriteLock writeMyChainLock = myChainLock.writeLock();
            WriteLock writeTransactionLock = transactionLock.writeLock();
            WriteLock writeVoteLock = myVotesLock.writeLock();
            WriteLock writeRegistrationLock = registrationLock.writeLock();
            WriteLock writeAuthorizationLock = authorizationLock.writeLock();
            WriteLock writeRevocationLock = revocationLock.writeLock();
            ReadLock readConnectionLock = connectionLock.readLock();
            try {
                while (!isTerminated) {


                    GeneralHelper.lockForMe(usingLockList, writeChainInfoFilesLock, writeMyChainLock);


                    if (myMainChain.canSignNext(myIdentifier) && requestedHeaders.isEmpty() && toBeRequestedHeaders.isEmpty()) {
                        if (myMainChain.isNextBlockInOrder(myIdentifier)) {

                            GeneralHelper.lockForMe(usingLockList, writeTransactionLock, readConnectionLock, writeVoteLock, writeRegistrationLock, writeAuthorizationLock, writeRevocationLock);

                            Vote vote = null;
                            if (myVotes.size() > 0 && myMainChain.getCurrentLatestBlockNumber() + 1 % Configuration.CHECK_POINT_BLOCK_INTERVAL != 0) {
                                vote = myVotes.get(0);
                            }
                            Block block = signBlock(vote, Configuration.IN_ORDER, myMainChain.getCurrentLatestBlockNumber() + 1, myMainChain.getLatestBlockHash());

                            BlockChainManager.storeBlock(block);

                            AuthorityInfo unTrustedAuthorityInfo = myMainChain.addBlock(block, transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                            if (unTrustedAuthorityInfo != null) {
                                unTrustAuthority(unTrustedAuthorityInfo.getIdentifier());
                            }

                            if (block.getContent().getMedicalOrgRevocationList() != null) {

                                for (byte[] revokedIdentifier : block.getContent().getMedicalOrgRevocationList())
                                    revokeMedicalOrg(revokedIdentifier, block.getHeader().getValidatorIdentifier());
                            }
                            if (block.getContent().getMedicalOrgAuthorizationList() != null) {
                                for (MedicalOrgInfo medicalOrgInfo : block.getContent().getMedicalOrgAuthorizationList())
                                    authorizeMedicalOrg(medicalOrgInfo.getIdentifier(), block.getHeader().getValidatorIdentifier());
                            }

                            System.out.println("In-order block signed and added: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                    + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore()); //debug
                            blockChainLogger.info("In-order block signed and added: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                    + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore());

                            broadcastMessage(new Message(Configuration.MESSAGE_BLOCK, block.getRaw()), null);

                            GeneralHelper.unLockForMe(usingLockList);

                        } else {

                            byte[] latestBlockHash = myMainChain.getLatestBlockHash();

                            if (System.currentTimeMillis() - myMainChain.getLatestBlockTimeStamp() < Configuration.MIN_OUT_ORDER_BLOCK_PERIOD) {
                                // wait for random offset and then if the block is not received, sign it
                                int randomOffset = myMainChain.generateRandomOffset();

                                GeneralHelper.unLockForMe(usingLockList);

                                Thread.sleep(randomOffset);

                                GeneralHelper.lockForMe(usingLockList,writeChainInfoFilesLock, writeMyChainLock);
                            }


                            if (Arrays.equals(latestBlockHash, myMainChain.getLatestBlockHash())) {
                                GeneralHelper.lockForMe(usingLockList, writeTransactionLock, readConnectionLock, writeVoteLock, writeRegistrationLock, writeAuthorizationLock, writeRevocationLock);

                                Vote vote = null;
                                if (myVotes.size() > 0 && myMainChain.getCurrentLatestBlockNumber() + 1 % Configuration.CHECK_POINT_BLOCK_INTERVAL != 0) {
                                    vote = myVotes.get(0);
                                }

                                Block block = signBlock(vote, Configuration.OUT_ORDER, myMainChain.getCurrentLatestBlockNumber() + 1, myMainChain.getLatestBlockHash());

                                BlockChainManager.storeBlock(block);

                                AuthorityInfo unTrustedAuthorityInfo = myMainChain.addBlock(block, transactionPool, myVotes, medicalOrgAuthorizationList, medicalOrgRevocationList, patientInfoList, myIdentifier);
                                if (unTrustedAuthorityInfo != null) {
                                    unTrustAuthority(unTrustedAuthorityInfo.getIdentifier());
                                }
                                if (block.getContent().getMedicalOrgRevocationList() != null) {

                                    for (byte[] revokedIdentifier : block.getContent().getMedicalOrgRevocationList())
                                        revokeMedicalOrg(revokedIdentifier, block.getHeader().getValidatorIdentifier());
                                }
                                if (block.getContent().getMedicalOrgAuthorizationList() != null) {
                                    for (MedicalOrgInfo medicalOrgInfo : block.getContent().getMedicalOrgAuthorizationList())
                                        authorizeMedicalOrg(medicalOrgInfo.getIdentifier(), block.getHeader().getValidatorIdentifier());
                                }

                                System.out.println("Out-order block signed and added: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                        + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore()); // debug

                                blockChainLogger.info("Out-order block signed and added: " + GeneralHelper.bytesToStringHex(myMainChain.getLatestBlockHash())
                                        + "\nCurrent latest block number: " + myMainChain.getCurrentLatestBlockNumber() + "\nScore: " + myMainChain.getTotalScore());

                                broadcastMessage(new Message(Configuration.MESSAGE_BLOCK, block.getRaw()), null);

                                GeneralHelper.unLockForMe(usingLockList);
                            } else {
                                GeneralHelper.unLockForMe(usingLockList);
                            }

                        }
                    }

                    GeneralHelper.unLockForMe(usingLockList);

                    Thread.sleep(100);


                }

            } catch (Exception e) {
                e.printStackTrace();
                shutdown();
            } finally {
                System.out.println("startValidation terminated"); //debug
                GeneralHelper.unLockForMe(usingLockList);
            }

        }
        ).

                start();

    }


    // includes unconfirmed connection since genrally used for requests
    private int unicastMessage(Message message, int index) {

        int newIndex = -1;
        int inBoundSize = inBoundConnectionList.size();
        int outBoundSize = outBoundConnectionList.size();
        int totalSize = inBoundSize + outBoundSize;
        ConnectionManager connectionManager = null;
        try {
            if (totalSize != 0) {
                newIndex = index % totalSize;

                if (newIndex >= inBoundSize) {
                    newIndex -= inBoundSize;
                    int i = 0;
                    for (PeerInfo tempPeerInfo : outBoundConnectionList.keySet()) {
                        if (i == newIndex) {
                            connectionManager = outBoundConnectionList.get(tempPeerInfo);
                            break;
                        }
                        ++i;
                    }

                } else {
                    int i = 0;
                    for (PeerInfo tempPeerInfo : inBoundConnectionList.keySet()) {
                        if (i == newIndex) {
                            connectionManager = inBoundConnectionList.get(tempPeerInfo);
                            break;
                        }
                        ++i;
                    }
                }

                connectionManager.write(message);
                blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Sent message " + message.number);
                System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Sent message " + message.number); //#debug
            }
        } catch (IOException e) {
            blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message "+ message.number+" but failed due to IOException. So, closing the connection");
            System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message"+ message.number+" but failed due to IOException. So, closing the connection"); //#debug
            connectionManager.close();
        }


        return newIndex;
    }

    private int randomUnicastMessage(Message message) {
        int random = -1;
        int inBoundSize = inBoundConnectionList.size();
        int outBoundSize = outBoundConnectionList.size();
        int totalSize = inBoundSize + outBoundSize;
        ConnectionManager connectionManager = null;
        try {
            if (totalSize != 0) {
                random = rand.nextInt(totalSize);

                if (random >= inBoundSize) { // from outbound
                    random -= inBoundSize;
                    int i = 0;
                    for (PeerInfo tempPeerInfo : outBoundConnectionList.keySet()) {
                        if (i == random) {
                            connectionManager = outBoundConnectionList.get(tempPeerInfo);
                            break;
                        }
                        ++i;
                    }

                } else {
                    int i = 0;
                    for (PeerInfo tempPeerInfo : inBoundConnectionList.keySet()) {
                        if (i == random) {
                            connectionManager = inBoundConnectionList.get(tempPeerInfo);
                            break;
                        }
                        ++i;
                    }
                }

                connectionManager.write(message);
                blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Sent message " + message.number);
                System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Sent message " + message.number); //#debug
            }
        } catch (IOException e) {
            blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message "+ message.number+" but failed due to IOException. So, closing the connection");
            System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message"+ message.number+" but failed due to IOException. So, closing the connection"); //#debug
            connectionManager.close();
        }

        return random;
    }

    //only to confirmed
    private void broadcastMessage(Message message, PeerInfo except) {

        int totalSize = 0;


        for (PeerInfo peerInfo : inBoundConnectionList.keySet()) {
            if (peerInfo.equals(except))
                continue;

            ConnectionManager connectionManager = inBoundConnectionList.get(peerInfo);
            if (!connectionManager.isConfirmed())
                continue;

            try {
                connectionManager.write(message);
                ++totalSize;
            } catch (IOException e) {
                blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message "+ message.number+" but failed due to IOException. So, closing the connection");
                System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message"+ message.number+" but failed due to IOException. So, closing the connection"); //#debug
                connectionManager.close();
            }
        }

        for (PeerInfo peerInfo : outBoundConnectionList.keySet()) {

            if (peerInfo.equals(except))
                continue;

            ConnectionManager connectionManager = outBoundConnectionList.get(peerInfo);
            if (!connectionManager.isConfirmed())
                continue;

            try {
                connectionManager.write(message);
                ++totalSize;
            } catch (IOException e) {
                blockChainLogger.info(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message"+ message.number+" but failed due to IOException. So, closing the connection");
                System.out.println(connectionManager.getSocket().getInetAddress().getHostAddress() + ": Attempted to send message"+ message.number+" but failed due to IOException. So, closing the connection"); //#debug
                connectionManager.close();
            }
        }

        if (totalSize > 0) {
            System.out.println("Broadcasted message " + message.number + " to " + totalSize + " node(s)");// debug
            blockChainLogger.info("Broadcasted message " + message.number + " to " + totalSize + " node(s)");
        }

    }


    private void unTrustAuthority(byte[] authorityIdentifier) {
        for (PeerInfo peerInfo : inBoundConnectionList.keySet())
            if (Arrays.equals(peerInfo.getIssuerIdentifier(), authorityIdentifier))
                inBoundConnectionList.get(peerInfo).close();
        for (PeerInfo peerInfo : outBoundConnectionList.keySet())
            if (Arrays.equals(peerInfo.getIssuerIdentifier(), authorityIdentifier))
                outBoundConnectionList.get(peerInfo).close();
    }

    private void revokeMedicalOrg(byte[] medicalOrgIdentifier, byte[] issuerIdentifier) {

        PeerInfo peerInfo = new PeerInfo(medicalOrgIdentifier, issuerIdentifier);
        if (inBoundConnectionList.containsKey(peerInfo)) {
            inBoundConnectionList.get(peerInfo).close();
        }

        if (outBoundConnectionList.containsKey(peerInfo)) {
            outBoundConnectionList.get(peerInfo).close();
        }
    }

    private void authorizeMedicalOrg(byte[] medicalOrgIdentifier, byte[] issuerIdentifier) {
        PeerInfo peerInfo = new PeerInfo(medicalOrgIdentifier, issuerIdentifier);
        if (inBoundConnectionList.containsKey(peerInfo)) {
            inBoundConnectionList.get(peerInfo).setConfirmed(true);
        }

        if (outBoundConnectionList.containsKey(peerInfo)) {
            outBoundConnectionList.get(peerInfo).setConfirmed(true);
        }
    }

    private Block signBlock(Vote vote, byte score, int blockNumber, byte[] prevBlockHash) {
        return new Block(myPrivateKey, vote, null, blockNumber, score, prevBlockHash, myPublicKey, getAuthorizations(Configuration.MAX_AUTHORIZATION)
                , getRevocations(Configuration.MAX_REVOCATION)
                , getRegistrations(Configuration.MAX_PATIENT_INFO)
                , getTransactions(Configuration.MAX_RECORD));
    }


    private MedicalOrgInfo[] getAuthorizations(int num) {
        if (medicalOrgAuthorizationList.size() == 0)
            return null;


        return medicalOrgAuthorizationList.subList(0, num > medicalOrgAuthorizationList.size() ? medicalOrgAuthorizationList.size() : num).toArray(new MedicalOrgInfo[0]);
    }

    private byte[][] getRevocations(int num) {
        if (medicalOrgRevocationList.size() == 0)
            return null;

        ArrayList<byte[]> revokedIdentifiers = new ArrayList<>();
        int length = num > medicalOrgRevocationList.size() ? medicalOrgRevocationList.size() : num;

        Iterator<byte[]> iterator = medicalOrgRevocationList.iterator();
        for (int i = 0; i < length; ++i) {
            revokedIdentifiers.add(iterator.next());
        }


        return revokedIdentifiers.toArray(new byte[0][0]);
    }

    private PatientInfo[] getRegistrations(int num) {
        if (patientInfoList.size() == 0)
            return null;

        return patientInfoList.subList(0, num > patientInfoList.size() ? patientInfoList.size() : num).toArray(new PatientInfo[0]);
    }


    private Transaction[] getTransactions(int num) {
        if (transactionPool.size() == 0)
            return null;


        return transactionPool.subList(0, num > transactionPool.size() ? transactionPool.size() : num).toArray(new Transaction[0]);
    }

    private InetAddress[] getPeerList(PeerInfo toWhom) {
        int inBoundSize = inBoundConnectionList.size();
        int outBoundSize = outBoundConnectionList.size();
        int totalSize = inBoundSize + outBoundSize - 1; // remove the one who asked
        InetAddress[] peerList = new InetAddress[totalSize > 0 ? totalSize : 0];

        int index = 0;
        for (PeerInfo peerInfo : inBoundConnectionList.keySet())
            if (!peerInfo.equals(toWhom))
                peerList[index++] = inBoundConnectionList.get(peerInfo).getSocket().getInetAddress();
        for (PeerInfo peerInfo : outBoundConnectionList.keySet())
            if (!peerInfo.equals(toWhom))
                peerList[index++] = outBoundConnectionList.get(peerInfo).getSocket().getInetAddress();

        return peerList;
    }

    private boolean checkIfConnectionExists(PeerInfo peerInfo) {

        if (inBoundConnectionList.containsKey(peerInfo))
            return true;
        return outBoundConnectionList.containsKey(peerInfo);
    }


    // For API begins


    public ArrayList<Vote> getMyVotes() {

        ReadLock readMyVotesLock = myVotesLock.readLock();

        readMyVotesLock.lock();

        ArrayList<Vote> votes = new ArrayList<>(myVotes);

        readMyVotesLock.unlock();

        return votes;
    }

    public byte[] getMyIdentifier() {
        return myIdentifier;
    }

    public ArrayList<AuthorityInfo> getOverallAuthorityList() throws IOException, BlockChainObjectParsingException {

        ReadLock readMyChainLock = myChainLock.readLock();


        readMyChainLock.lock();

        ArrayList<AuthorityInfo> currentOverallAuthorityList = myMainChain.getCurrentOverallAuthorityInfoList();

        readMyChainLock.unlock();


        return currentOverallAuthorityList;

    }

    public ArrayList<Voting> getVotingList() {

        return myMainChain.getCurrentVotingList();

    }


    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: cannot be prossessed
     */
    public byte castVote(Vote v) throws IOException, BlockChainObjectParsingException {
        byte result = 0;

        ReadLock readMyChainLock = myChainLock.readLock();
        WriteLock writeMyVotesLock = myVotesLock.writeLock();

        ArrayList<Lock> usingLock = new ArrayList<>();

        GeneralHelper.lockForMe(usingLock, readMyChainLock, writeMyVotesLock);

        if (myVotes.contains(v)) {
            result = 1;
        } else if (!myMainChain.checkVote(myIdentifier, v)) { // already exists, existed or duplicate, ...
            result = 2;
        }
        if (result == 0)
            myVotes.add(v);

        GeneralHelper.unLockForMe(usingLock);

        return result;

    }


    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */
    public byte addToPatientInfoListForRegistration(PatientInfo patientInfo) throws IOException, BlockChainObjectParsingException, BadRequest {


        byte result = 0;

        ReadLock readMyChainLock = myChainLock.readLock();
        WriteLock writeRegistrationLock = registrationLock.writeLock();

        ArrayList<Lock> usingLock = new ArrayList<>();

        GeneralHelper.lockForMe(usingLock, readMyChainLock, writeRegistrationLock);

        if (patientInfoList.contains(patientInfo)) {
            result = 1;
        } else if (PatientInfoManager.patientExists(myMainChain.getLatestBlockHash(), patientInfo.getPatientIdentifier())) {
            result = 2;
        } else if (!patientInfo.verify()) {
            System.out.println("This message shouldn't pop up");//debug
            throw new BadRequest();
        }

        if (result == 0)
            patientInfoList.add(patientInfo);

        GeneralHelper.unLockForMe(usingLock);

        return result;
    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: patient doesn't exist
     * 3: already updated
     */
    public byte addToPatientInfoListForUpdate(PatientInfo patientInfo) throws IOException, BlockChainObjectParsingException, BadRequest {

        byte result = 0;

        ReadLock readMyChainLock = myChainLock.readLock();
        WriteLock writeRegistrationLock = registrationLock.writeLock();


        ArrayList<Lock> usingLock = new ArrayList<>();

        GeneralHelper.lockForMe(usingLock, readMyChainLock, writeRegistrationLock);

        if (patientInfoList.contains(patientInfo)) {
            result = 1;
        } else if (!PatientInfoManager.patientExists(myMainChain.getLatestBlockHash(), patientInfo.getPatientIdentifier())) {
            result = 2;
        } else if (PatientInfoManager.patientInfoExists(myMainChain.getLatestBlockHash(), patientInfo.getPatientIdentifier(), patientInfo.calculateInfoHash())) {
            result = 3;
        } else if (!patientInfo.verify()) {
            System.out.println("This message shouldn't pop up");//debug
            throw new BadRequest();
        }

        if (result == 0)
            patientInfoList.add(patientInfo);

        GeneralHelper.unLockForMe(usingLock);


        return result;
    }

    public byte[] getPatientEncryptedInfo(Location location) throws IOException, BlockChainObjectParsingException {
        return PatientInfoManager.loadEcnryptedInfo(location);
    }

    /*
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: authorized by another authority
     */
    public byte addToRevocationList(byte[] identifier) throws IOException, BlockChainObjectParsingException, FileCorruptionException, NotFound {

        byte result = 0;

        ReadLock readMyChainLock = myChainLock.readLock();
        WriteLock writeRevocationLock = revocationLock.writeLock();

        ArrayList<Lock> usingLock = new ArrayList<>();

        GeneralHelper.lockForMe(usingLock, readMyChainLock, writeRevocationLock);

        if (!myMainChain.hasMedicalOrg(identifier)) {
            throw new NotFound();
        } else {
            if (medicalOrgRevocationList.contains(identifier)) {
                result = 1;
            } else if (!Arrays.equals(myMainChain.getMedicalOrgInfoForInternal(identifier).getAuthorityIdentifier(), myIdentifier)) {
                result = 2;
            }

            if (result == 0)
                medicalOrgRevocationList.add(identifier);

            GeneralHelper.unLockForMe(usingLock);
        }

        return result;
    }

    /*
     * return error status code(1 byte) or transaction id(32 byte) (if successful):
     *return status code:
     * 0: successful
     * 1: being processed
     * 2: already exists
     */
    public byte addToAuthorizationList(MedicalOrgInfo medicalOrgInfo) throws IOException, BlockChainObjectParsingException, FileCorruptionException {

        byte result = 0;

        ReadLock readMyChainLock = myChainLock.readLock();
        WriteLock writeAuthorizationLock = authorizationLock.writeLock();

        ArrayList<Lock> usingLock = new ArrayList<>();

        GeneralHelper.lockForMe(usingLock, readMyChainLock, writeAuthorizationLock);

        if (medicalOrgAuthorizationList.contains(medicalOrgInfo)) {
            result = 1;
        } else if (myMainChain.hasMedicalOrg(medicalOrgInfo.getIdentifier())) {
            result = 2;
        } else if (myMainChain.isMedicalOrgRevoked(medicalOrgInfo.getIdentifier())) {
            result = 2;
        }

        if (result == 0) {
            medicalOrgAuthorizationList.add(medicalOrgInfo);
        }

        GeneralHelper.unLockForMe(usingLock);

        return result;
    }


    /*
     *return status code:
     * 0: successful
     * 1: not authorized
     * 2: not authorized by me
     */
    public byte isMedicalOrgAuthorizedByMe(byte[] identifier) throws IOException, BlockChainObjectParsingException, FileCorruptionException {

        byte result = 0;

        ReadLock readMyChainLock = myChainLock.readLock();

        ArrayList<Lock> usingLock = new ArrayList<>();

        GeneralHelper.lockForMe(usingLock, readMyChainLock);


        if (!myMainChain.hasMedicalOrg(identifier))
            result = 1;
        else if (!Arrays.equals(myIdentifier, myMainChain.getMedicalOrgInfoForInternal(identifier).getAuthorityIdentifier()))
            result = 2;

        GeneralHelper.unLockForMe(usingLock);

        return result;
    }

    public X509Certificate issueCertificate(MedicalOrgInfo medicalOrgInfo, Date noAfter) throws OperatorCreationException, CertificateException, IOException {
        return SecurityHelper.issueCertificate(medicalOrgInfo.getPublicKey(), myPublicKey, myPrivateKey
                , noAfter, medicalOrgInfo.getName(), myName
                , BlockChainSecurityHelper.calculateIdentifierFromECPublicKey(medicalOrgInfo.getPublicKey())
                , myIdentifier, null, Configuration.SIGNING_CERTIFICATE_SIGNATURE_ALGORITHM, true);
    }


    public MedicalOrgInfo getMedicalOrgInfo(byte[] medicalOrgIdentifier) throws BlockChainObjectParsingException, IOException {

        ReadLock readMyChainLock = myChainLock.readLock();

        readMyChainLock.lock();

        MedicalOrgInfo medicalOrgInfo = null;
        try {
            medicalOrgInfo = myMainChain.getMedicalOrgInfoForInternal(medicalOrgIdentifier).getMedicalOrgInfo();
        } catch (FileCorruptionException e) {
            e.printStackTrace();
            shutdown();
        } finally {
            readMyChainLock.unlock();
        }

        return medicalOrgInfo;

    }

    public ArrayList<MedicalOrgShortInfo> getAllMedicalOrgShortInfoAuthorizedBy(byte[] authorityIdentifier) throws BlockChainObjectParsingException, IOException {

        ReadLock readMyChainLock = myChainLock.readLock();

        readMyChainLock.lock();

        ArrayList<MedicalOrgShortInfo> medicalOrgShortInfos = MedicalOrgInfoManager.loadEveryShortInfo(myMainChain.getLatestBlockHash(), authorityIdentifier);

        readMyChainLock.unlock();

        return medicalOrgShortInfos;

    }

    public ArrayList<PatientShortInfo> getPatientShortInfoList(byte[] patienIdentifier) throws IOException, BlockChainObjectParsingException {

        ReadLock readMyChainLock = myChainLock.readLock();

        readMyChainLock.lock();

        ArrayList<PatientShortInfo> patientShortInfos = PatientInfoManager.loadEveryShortInfo(myMainChain.getLatestBlockHash(), patienIdentifier);
        readMyChainLock.unlock();

        return patientShortInfos;
    }


}
