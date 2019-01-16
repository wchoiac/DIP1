package general.security;

import config.Configuration;
import general.utility.GeneralHelper;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;


public class SecurityHelper {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    static final Random random = new SecureRandom();



    //reference https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa
    public static byte[] hash(byte[] data, String algo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algo);
        return digest.digest(data);

    }

    // SHA256withECDSA for EC & SHA256withRSA for RSA
    //Check whether the encrypt(signature) equals to the hash of content
    //reference: http://www.java2s.com/Tutorial/Java/0490__Security/SimpleDigitalSignatureExample.htm
    public static boolean checkDigitalSignature(PublicKey signerPublicKey, byte[] signature, byte[] content, String algo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        boolean valid = false;
        Signature sig = Signature.getInstance(algo);
        sig.initVerify(signerPublicKey);
        sig.update(content);
        valid = sig.verify(signature);

        return valid;
    }

    // SHA256withECDSA for EC & SHA256withRSA for RSA
    //For now, just convert the byte values of the content into string (May be changed later)
    public static byte[] createDigitalSignature(PrivateKey signerPrivateKey, byte[] content, String algo) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {

        Signature sig = Signature.getInstance(algo);
        sig.initSign(signerPrivateKey);
        sig.update(content);

        return sig.sign();

    }


    // SHA256withECDSA implementation
    // input content unlike createECDSASignatureWithHash - automatically hashes
    public static boolean verifyECDSASignatureWithContent(ECPublicKey signerPublicKey, byte[] content, byte[] signature, String hashAlgo) throws NoSuchAlgorithmException {

        byte[] hash = hash(content,hashAlgo);

        byte[] r = Arrays.copyOfRange(signature, 0, signature.length / 2);
        byte[] s = Arrays.copyOfRange(signature, signature.length / 2, signature.length);

        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(Configuration.ELIPTIC_CURVE);
        BigInteger affineX = signerPublicKey.getW().getAffineX();
        BigInteger affineY = signerPublicKey.getW().getAffineY();
        ECCurve curve = ecParameterSpec.getCurve();

        ECDomainParameters domainParameters = new ECDomainParameters(curve, ecParameterSpec.getG(), ecParameterSpec.getN(), ecParameterSpec.getH());
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(curve.createPoint(affineX,affineY), domainParameters);

        ECDSASigner ecdsaSigner = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsaSigner.init(false, publicKeyParameters);

        return ecdsaSigner.verifySignature(hash, new BigInteger(1, r), new BigInteger(1, s));

    }


    // SHA256withECDSA implementation
    // input content unlike createECDSASignatureWithHash - automatically hashes
    public static byte[] createECDSASignatureWithContent(ECPrivateKey signerPrivateKey, byte[] content, String hashAlgo) throws NoSuchAlgorithmException {

        byte[] hash = hash(content, hashAlgo);
        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(Configuration.ELIPTIC_CURVE);
        ECCurve curve = ecParameterSpec.getCurve();
        ECDomainParameters domainParameters = new ECDomainParameters(curve, ecParameterSpec.getG(), ecParameterSpec.getN(), ecParameterSpec.getH());
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(signerPrivateKey.getS(), domainParameters);

        ECDSASigner ecdsaSigner = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsaSigner.init(true, privateKeyParameters);

        BigInteger[] bigIntegers = ecdsaSigner.generateSignature(hash);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            for (BigInteger bigInteger : bigIntegers) {
                byte[] tempBytes = bigInteger.toByteArray();
                if (tempBytes.length == 31) {
                    byteArrayOutputStream.write(0);
                } else if (tempBytes.length == 32) {
                    byteArrayOutputStream.write(tempBytes);
                } else {
                    byteArrayOutputStream.write(tempBytes, tempBytes.length - 32, 32);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();

    }

    // NONEwithECDSA implementation
    // input "hash" not the content
    public static byte[] createECDSASignatureWithHash(ECPrivateKey signerPrivateKey, byte[] hash) throws IOException {

        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(Configuration.ELIPTIC_CURVE);
        ECCurve curve = ecParameterSpec.getCurve();
        ECDomainParameters domainParameters = new ECDomainParameters(curve, ecParameterSpec.getG(), ecParameterSpec.getN(), ecParameterSpec.getH());
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(signerPrivateKey.getS(), domainParameters);

        ECDSASigner ecdsaSigner = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsaSigner.init(true, privateKeyParameters);

        BigInteger[] bigIntegers = ecdsaSigner.generateSignature(hash);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (BigInteger bigInteger : bigIntegers) {
            byte[] tempBytes = bigInteger.toByteArray();
            System.out.println(tempBytes.length);
            if (tempBytes.length == 31) {
                byteArrayOutputStream.write(0);
            } else if (tempBytes.length == 32) {
                byteArrayOutputStream.write(tempBytes);
            } else {
                byteArrayOutputStream.write(tempBytes, tempBytes.length - 32, 32);
            }

        }

        return byteArrayOutputStream.toByteArray();

    }

    public static SecretKey getAESKeyFromFile(String fileName) throws IOException {

        SecretKey aesKey = new SecretKeySpec(Files.readAllBytes(new File(fileName).toPath()), "AES");
        return aesKey;
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException
    {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        return secretKey;
    }

    public static byte[] generateAESIV()
    {
        int ivSize = 16;
        byte[] iv = new byte[ivSize];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }
    //referece: https://www.programcreek.com/java-api-examples/?api=org.bouncycastle.util.io.pem.PemReader
    public static X509Certificate getX509FromBytes(byte[] bytes) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        X509Certificate cert = (X509Certificate)certificateFactory.generateCertificate(bis);
        bis.close();

        return cert;
    }

    public static byte[] encryptAES(byte[] content, SecretKey secretKey, byte[] iv) throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {

        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec);
        return cipher.doFinal(content);
    }

    public static KeyPair generateECKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec(curve), new SecureRandom());
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }
    //reference: https://stackoverflow.com/questions/5127379/how-to-generate-a-rsa-keypair-with-a-privatekey-encrypted-with-password
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }

    public static String getSubjectCNFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }

    public static String getIssuerCNFromX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(cert).getIssuer();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }

    //reference: https://stackoverflow.com/questions/16412315/creating-custom-x509-v3-extensions-in-java-with-bouncy-castle
    public static X509Certificate issueCertificate(PublicKey newPublicKey, ECPublicKey issuerPublicKey, PrivateKey issuerPrivateKey, Date noAfter, String subjectName, String authorityName
            ,byte[] subjectKeyIdentifierBytes,byte[] authorityKeyIdentifierBytes ,InetAddress ipAddress, String algo, boolean isForSigning) throws CertificateException, OperatorCreationException, IOException {
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                new X500Name("CN=" + authorityName),
                BigInteger.valueOf(new Random().nextInt()),
                new Date(),
                noAfter,
                new X500Name("CN=" + subjectName),
                SubjectPublicKeyInfo.getInstance(newPublicKey.getEncoded()));

        AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifier(authorityKeyIdentifierBytes);
        builder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
        if(isForSigning) {
            SubjectKeyIdentifier subjectKeyIdentifier = new SubjectKeyIdentifier(subjectKeyIdentifierBytes);
            builder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        }

        if (isForSigning) {
            KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature);
            builder.addExtension(Extension.keyUsage, false, usage.getEncoded());
        } else {
            KeyUsage usage = new KeyUsage(KeyUsage.digitalSignature);
            builder.addExtension(Extension.keyUsage, false, usage.getEncoded());
        }


//		builder.addExtension(Extension.keyUsage,true,new KeyUsage(KeyUsage.digitalSignature|KeyUsage.keyEncipherment));

        if (ipAddress != null) {
            GeneralName[] generalName = {new GeneralName(GeneralName.iPAddress, ipAddress.getHostAddress())};
            builder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(generalName));
        }

        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(builder
                .build(new JcaContentSignerBuilder(algo).setProvider("BC").
                        build(issuerPrivateKey)));


        return cert;

    }


    public static void writePublicKeyToPEM(PublicKey publicKey, String fileName) throws IOException {
        Writer writer = new FileWriter(fileName);
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        pemWriter.close();
    }

    //referece: https://www.programcreek.com/java-api-examples/?api=org.bouncycastle.util.io.pem.PemReader
    public static PublicKey getPublicKeyFromPEM(String fileName, String algo) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileReader reader = new FileReader(fileName);
        PemReader pemReader = new PemReader(reader);
        KeyFactory keyFactory = KeyFactory.getInstance(algo);
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();

        return keyFactory.generatePublic(new X509EncodedKeySpec(content));
    }

    public static void writePrivateKeyToPEM(PrivateKey privateKey, String fileName, String algo) throws IOException {
        Writer writer = new FileWriter(fileName);
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject(algo + " PRIVATE KEY", privateKey.getEncoded()));
        pemWriter.close();
    }

    public static PrivateKey getPrivateFromPEM(String fileName, String algo) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        FileReader reader = new FileReader(fileName);
        PemReader pemReader = new PemReader(reader);
        KeyFactory keyFactory = KeyFactory.getInstance(algo);
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(content));
    }

    public static void writeX509ToDER(X509Certificate cert, File file) throws IOException, CertificateEncodingException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(cert.getEncoded());
        fos.close();
    }

    public static X509Certificate getX509FromDER(File file) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
//        FileInputStream fis = new FileInputStream(file);
//        ByteArrayInputStream bis = new ByteArrayInputStream(fis.read()); JAVA 9 AND ABOVE
        ByteArrayInputStream bis = new ByteArrayInputStream(Files.readAllBytes(file.toPath()));
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(bis);

        bis.close();

        return cert;
    }


    public static byte[] getIssuerIdentifierFromX509Cert(X509Certificate cert) {
        byte[] bytes = cert.getExtensionValue(Extension.authorityKeyIdentifier.getId());
        byte[] octets = ASN1OctetString.getInstance(bytes).getOctets();
        AuthorityKeyIdentifier authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(octets);
        return authorityKeyIdentifier.getKeyIdentifier();
    }
    // SHA256withECDSA implementation
    // input content unlike createRawECDSASignatureWithHash - automatically hashes
    public static boolean verifyRawECDSASignatureWithContent(ECPublicKey signerPublicKey, byte[] content, byte[] signature, String hashAlgo, String curveName) throws NoSuchAlgorithmException {

        byte[] hash = hash(content, hashAlgo);

        byte[] r = Arrays.copyOfRange(signature, 0, signature.length / 2);
        byte[] s = Arrays.copyOfRange(signature, signature.length / 2, signature.length);

        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        BigInteger affineX = signerPublicKey.getW().getAffineX();
        BigInteger affineY = signerPublicKey.getW().getAffineY();
        ECCurve curve = ecParameterSpec.getCurve();

        ECDomainParameters domainParameters = new ECDomainParameters(curve, ecParameterSpec.getG(), ecParameterSpec.getN(), ecParameterSpec.getH());
        ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(curve.createPoint(affineX, affineY), domainParameters);

        ECDSASigner ecdsaSigner = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsaSigner.init(false, publicKeyParameters);

        return ecdsaSigner.verifySignature(hash, new BigInteger(1, r), new BigInteger(1, s));

    }

    //https://stackoverflow.com/questions/33218674/how-to-make-a-bouncy-castle-ecpublickey
    public static byte[] getCompressedRawECPublicKey(ECPublicKey publicKey) {

        BigInteger affineX = publicKey.getW().getAffineX();
        BigInteger affineY = publicKey.getW().getAffineY();
        ECCurve curve = ECNamedCurveTable.getParameterSpec(Configuration.ELIPTIC_CURVE).getCurve();

        return curve.createPoint(affineX,affineY).getEncoded(true);
    }

    //https://bitcoin.stackexchange.com/questions/44024/get-uncompressed-public-key-from-compressed-form
    public static ECPublicKey getECPublicKeyFromCompressedRaw(byte[] raw) throws InvalidKeySpecException {
        ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec(Configuration.ELIPTIC_CURVE);
        ECPoint point = parameterSpec.getCurve().decodePoint(raw);

        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, parameterSpec));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null; // not expected
    }

    public static void main(String[] args) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException, SignatureException, InvalidKeyException {

        KeyPair keyPair = generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        byte[] hash = hash(new byte[]{0}, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        byte[] sig = createECDSASignatureWithHash(privateKey, hash);
        byte[] sig2 = createECDSASignatureWithContent(privateKey, new byte[]{0},"SHA256");
        byte[] sig3 = createDigitalSignature(privateKey, new byte[]{0}, "SHA256withECDSA");

        System.out.println(GeneralHelper.bytesToStringHex(sig));
        System.out.println(GeneralHelper.bytesToStringHex(sig2));
        System.out.println(GeneralHelper.bytesToStringHex(sig3));
        System.out.println(verifyECDSASignatureWithContent((ECPublicKey) keyPair.getPublic(), new byte[]{0}, sig,"SHA256"));

        System.out.println(System.currentTimeMillis());
    }

}
