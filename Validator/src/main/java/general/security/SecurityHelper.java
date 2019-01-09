package general.security;

import config.Configuration;
import general.utility.GeneralHelper;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
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

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;


public class SecurityHelper {


    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    ;
    static final Random random = new SecureRandom();


    //----------------------------------------------for hash start------------------------------------------------

    //reference https://medium.com/programmers-blockchain/create-simple-blockchain-java-tutorial-from-scratch-6eeed3cb03fa
    public static byte[] hash(byte[] data, String algo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algo);
        return digest.digest(data);

    }

    //----------------------------------------------for hash end------------------------------------------------

    //----------------------------------------------for file handling start------------------------------------------------
    public static void writePublicKeyToPEM(PublicKey publicKey, String fileName) throws IOException {
        Writer writer = new FileWriter(fileName);
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        pemWriter.close();
    }

    //referece: https://www.programcreek.com/java-api-examples/?api=org.bouncycastle.util.io.pem.PemReader
    public static PublicKey getPublicKeyFromPEM(String fileName, String algo) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
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

    public static PrivateKey getPrivateFromPEM(String fileName, String algo) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
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

    public static X509Certificate getX509FromDER(File file) throws IOException, CertificateException, NoSuchProviderException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        FileInputStream fis = new FileInputStream(file);
        ByteArrayInputStream bis = new ByteArrayInputStream(fis.readAllBytes());
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(bis);

        bis.close();

        return cert;
    }
    //----------------------------------------------for file handling end------------------------------------------------

    //----------------------------------------------for certificate start------------------------------------------------
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

    //referece: https://www.programcreek.com/java-api-examples/?api=org.bouncycastle.util.io.pem.PemReader
    public static X509Certificate getX509FromBytes(byte[] bytes) throws IOException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(bis);
        bis.close();

        return cert;
    }


    //reference: https://stackoverflow.com/questions/16412315/creating-custom-x509-v3-extensions-in-java-with-bouncy-castle
    public static X509Certificate issueCertificate(PublicKey newPublicKey, ECPublicKey issuerPublicKey, PrivateKey issuerPrivateKey, Date noAfter, String subjectName, String authorityName
            , byte[] subjectKeyIdentifierBytes, byte[] authorityKeyIdentifierBytes, InetAddress ipAddress, String algo, boolean isForSigning) throws CertificateException, OperatorCreationException, IOException {
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                new X500Name("CN=" + authorityName),
                BigInteger.valueOf(new Random().nextInt()),
                new Date(),
                noAfter,
                new X500Name("CN=" + subjectName),
                SubjectPublicKeyInfo.getInstance(newPublicKey.getEncoded()));

        AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifier(authorityKeyIdentifierBytes);
        builder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyIdentifier);
        if (isForSigning) {
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

        if (ipAddress != null) {
            GeneralName[] generalName = {new GeneralName(GeneralName.iPAddress, ipAddress.getHostAddress())};
            builder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(generalName));
        }

        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(builder
                .build(new JcaContentSignerBuilder(algo).setProvider("BC").
                        build(issuerPrivateKey)));


        return cert;

    }


    public static byte[] getIssuerIdentifierFromX509Cert(X509Certificate cert) {
        byte[] bytes = cert.getExtensionValue(Extension.authorityKeyIdentifier.getId());
        byte[] octets = ASN1OctetString.getInstance(bytes).getOctets();
        AuthorityKeyIdentifier authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(octets);
        return authorityKeyIdentifier.getKeyIdentifier();
    }

    //----------------------------------------------for certificate end------------------------------------------------

    //----------------------------------------------for api start------------------------------------------------

    // reference: https://www.baeldung.com/java-password-hashing
    public static byte[] passwordHash(char[] password, byte[] salt, String algo) throws NoSuchAlgorithmException {
        KeySpec spec = new PBEKeySpec(password, salt, 65536, Configuration.PASSWORD_HASH_LENGTH * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(algo);

        byte[] result = null;
        try {
            result = factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace(); // not expected
        }
        return result;
    }

    public static String generateSecureToken() {
        return GeneralHelper.bytesToBase64UrlString(new BigInteger(130, random).toByteArray());
    }

    //----------------------------------------------for api end------------------------------------------------

    //----------------------------------------------for key start------------------------------------------------

    public static KeyPair generateECKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec(curve), new SecureRandom());
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }

    //reference: https://stackoverflow.com/questions/5127379/how-to-generate-a-rsa-keypair-with-a-privatekey-encrypted-with-password
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();
        return pair;
    }


    //https://stackoverflow.com/questions/33218674/how-to-make-a-bouncy-castle-ecpublickey
    public static byte[] getCompressedRawECPublicKey(ECPublicKey publicKey, String curveName) {

        BigInteger affineX = publicKey.getW().getAffineX();
        BigInteger affineY = publicKey.getW().getAffineY();
        ECCurve curve = ECNamedCurveTable.getParameterSpec(curveName).getCurve();

        return curve.createPoint(affineX, affineY).getEncoded(true);
    }

    //https://bitcoin.stackexchange.com/questions/44024/get-uncompressed-public-key-from-compressed-form
    public static ECPublicKey getECPublicKeyFromCompressedRaw(byte[] raw, String curveName) throws InvalidKeySpecException {
        ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECPoint point = parameterSpec.getCurve().decodePoint(raw);

        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, parameterSpec));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null; // not expected
    }

    //https://bitcoin.stackexchange.com/questions/44024/get-uncompressed-public-key-from-compressed-form
    public static ECPublicKey getECPublicKeyFromEncoded(byte[] encoded) throws InvalidKeySpecException {


        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();// not expected
        }
        return (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    public static boolean checkCurve(byte[] ecPublicKeyEncoded, ASN1ObjectIdentifier curveOID) {
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(ecPublicKeyEncoded));
        ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) subjectPublicKeyInfo.getAlgorithm().getParameters();
        return oid.equals(curveOID);
    }

    //----------------------------------------------for key end------------------------------------------------

    //----------------------------------------------for signature start------------------------------------------------

    //reference: http://www.java2s.com/Tutorial/Java/0490__Security/SimpleDigitalSignatureExample.htm
    public static boolean verifyDEREncodedSignature(PublicKey signerPublicKey, byte[] signature, byte[] content, String algo) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        boolean valid = false;
        Signature sig = Signature.getInstance(algo);
        sig.initVerify(signerPublicKey);
        sig.update(content);
        valid = sig.verify(signature);

        return valid;
    }


    public static byte[] createDEREncodedSignature(PrivateKey signerPrivateKey, byte[] content, String algo) throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {

        Signature sig = Signature.getInstance(algo);
        sig.initSign(signerPrivateKey);
        sig.update(content);

        return sig.sign();

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


    // SHA256withECDSA implementation
    // input content unlike createRawECDSASignatureWithHash - automatically hashes
    public static byte[] createRawECDSASignatureWithContent(ECPrivateKey signerPrivateKey, byte[] content, String hashAlgo, String curveName, int coordinateLength) throws NoSuchAlgorithmException {

        byte[] hash = hash(content, hashAlgo);
        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECCurve curve = ecParameterSpec.getCurve();
        ECDomainParameters domainParameters = new ECDomainParameters(curve, ecParameterSpec.getG(), ecParameterSpec.getN(), ecParameterSpec.getH());
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(signerPrivateKey.getS(), domainParameters);

        ECDSASigner ecdsaSigner = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsaSigner.init(true, privateKeyParameters);

        BigInteger[] bigIntegers = ecdsaSigner.generateSignature(hash);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            // for adjusting length so that each point is 32 byte (pad or remove leading zero)
            for (BigInteger bigInteger : bigIntegers) {
                byte[] tempBytes = bigInteger.toByteArray();
                if (tempBytes.length < coordinateLength) {
                    for (int i = 0; i < coordinateLength - tempBytes.length; ++i)
                        byteArrayOutputStream.write(0);
                    byteArrayOutputStream.write(tempBytes);
                } else if (tempBytes.length == coordinateLength) {
                    byteArrayOutputStream.write(tempBytes);
                } else {//leading zero
                    byteArrayOutputStream.write(tempBytes, tempBytes.length - coordinateLength, coordinateLength);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();

    }

    // NONEwithECDSA implementation
    // input "hash" not the content
    public static byte[] createRawECDSASignatureWithHash(ECPrivateKey signerPrivateKey, byte[] hash, String curveName, int coordinateLength) throws IOException {

        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);
        ECCurve curve = ecParameterSpec.getCurve();
        ECDomainParameters domainParameters = new ECDomainParameters(curve, ecParameterSpec.getG(), ecParameterSpec.getN(), ecParameterSpec.getH());
        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(signerPrivateKey.getS(), domainParameters);

        ECDSASigner ecdsaSigner = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsaSigner.init(true, privateKeyParameters);

        BigInteger[] bigIntegers = ecdsaSigner.generateSignature(hash);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            // for adjusting length so that each point is 32 byte (pad or remove leading zero)
            for (BigInteger bigInteger : bigIntegers) {
                byte[] tempBytes = bigInteger.toByteArray();
                if (tempBytes.length < coordinateLength) {
                    for (int i = 0; i < coordinateLength - tempBytes.length; ++i)
                        byteArrayOutputStream.write(0);
                    byteArrayOutputStream.write(tempBytes);
                } else if (tempBytes.length == coordinateLength) {
                    byteArrayOutputStream.write(tempBytes);
                } else {//leading zero
                    byteArrayOutputStream.write(tempBytes, tempBytes.length - coordinateLength, coordinateLength);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();

    }


    //test reference:https://stackoverflow.com/questions/49825455/ecdsa-signature-java-vs-go
    public static byte[] getRawFromDERECDSASignature(byte[] signature, int coordinateLength) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int startR = (signature[1] & 0x80) != 0 ? 3 : 2;
        int lengthR = signature[startR + 1];

        int startS = startR + 2 + lengthR;
        int lengthS = signature[startS + 1];

        try {
            if (lengthR < coordinateLength) {
                for (int i = 0; i < coordinateLength - lengthR; ++i)
                    byteArrayOutputStream.write(0);
                byteArrayOutputStream.write(Arrays.copyOfRange(signature, startR + 2, startR + 2 + lengthR));
            } else if (lengthR == coordinateLength) {
                byteArrayOutputStream.write(Arrays.copyOfRange(signature, startR + 2, startR + 2 + lengthR));
            } else {//leading zero
                byteArrayOutputStream.write(Arrays.copyOfRange(signature, startR + 2 + lengthR - coordinateLength, startR + 2 + lengthR));
            }

            if (lengthS < coordinateLength) {
                for (int i = 0; i < coordinateLength - lengthS; ++i)
                    byteArrayOutputStream.write(0);
                byteArrayOutputStream.write(Arrays.copyOfRange(signature, startS + 2, startS + 2 + lengthS));
            } else if (lengthS == coordinateLength) {
                byteArrayOutputStream.write(Arrays.copyOfRange(signature, startS + 2, startS + 2 + lengthS));
            } else {//leading zero
                byteArrayOutputStream.write(Arrays.copyOfRange(signature, startS + 2 + lengthS - coordinateLength, startS + 2 + lengthS));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] rawToDERECDSASignature(byte[] raw) // points are expected to have the same length
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int pointLength = raw.length / 2;
        try {
            byteArrayOutputStream.write(0x30); // sequence
            byteArrayOutputStream.write(2 * pointLength + 4); // length
            byteArrayOutputStream.write(0x02); // integer
            byteArrayOutputStream.write(pointLength); // point length
            byteArrayOutputStream.write(Arrays.copyOfRange(raw, 0, pointLength));
            byteArrayOutputStream.write(0x02);
            byteArrayOutputStream.write(pointLength);
            byteArrayOutputStream.write(Arrays.copyOfRange(raw, pointLength, raw.length));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }
    //----------------------------------------------for signature end------------------------------------------------


    public static void main(String[] args) throws Exception {

//        KeyPair keyPair = generateECKeyPair(Configuration.ELIPTIC_CURVE);
//        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
//        byte[] hash = hash(new byte[]{0}, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
//        byte[] sig = createRawECDSASignatureWithHash(privateKey, hash, Configuration.ELIPTIC_CURVE, Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
//        byte[] sig2 = createRawECDSASignatureWithContent(privateKey, new byte[]{0}, "SHA256", Configuration.ELIPTIC_CURVE, Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
//        byte[] sig3 = createDEREncodedSignature(privateKey, new byte[]{0}, "SHA256withECDSA");
//        byte[] sig4 = createDEREncodedSignature(privateKey, hash, "NONEWithECDSA"); // <= client-side hashing
//        System.out.println(GeneralHelper.bytesToStringHex(sig));
//        System.out.println(GeneralHelper.bytesToStringHex(sig2));
//        System.out.println(GeneralHelper.bytesToStringHex(sig3));
//        System.out.println(GeneralHelper.bytesToStringHex(sig4));
//
//        byte[] extractedSig = getRawFromDERECDSASignature(sig3, Configuration.ELIPTIC_CURVE_COORDINATE_LENGTH);
//        System.out.println(GeneralHelper.bytesToStringHex(extractedSig));
//
//
//        System.out.println(verifyRawECDSASignatureWithContent((ECPublicKey) keyPair.getPublic(), new byte[]{0}, sig, "SHA256", Configuration.ELIPTIC_CURVE));
//        System.out.println(verifyRawECDSASignatureWithContent((ECPublicKey) keyPair.getPublic(), new byte[]{0}, extractedSig, "SHA256", Configuration.ELIPTIC_CURVE));
//        System.out.println(verifyDEREncodedSignature(keyPair.getPublic(), sig3, new byte[]{0}, "SHA256withECDSA"));
//        System.out.println(verifyDEREncodedSignature(keyPair.getPublic(), sig4, new byte[]{0}, "SHA256withECDSA"));
//
//
//        byte[] derSig3 = rawToDERECDSASignature(extractedSig);
//        System.out.println(GeneralHelper.bytesToStringHex(derSig3));
//        System.out.println(verifyDEREncodedSignature(keyPair.getPublic(), derSig3, new byte[]{0}, "SHA256withECDSA"));
//        // SHA256withECDSA for EC & SHA256withRSA for RSA
//        //For now, just convert the byte values of the content into string (May be changed later)
//
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        System.out.println(GeneralHelper.bytesToStringHex(byteArrayOutputStream.toByteArray()));
//        byte[] hash2 = hash(byteArrayOutputStream.toByteArray(), Configuration.BLOCKCHAIN_HASH_ALGORITHM);
//        System.out.println((byte) ((1 << Configuration.INITIAL_AUTHORITIES_BIT_POSITION)));

        KeyPair keyPair = generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        byte[] content =new byte[]{1,2,3,4};
        byte[] hash = hash(new byte[]{1,2,3,4}, Configuration.BLOCKCHAIN_HASH_ALGORITHM);
        byte[] sig = createDEREncodedSignature(privateKey, hash, "NONEWithECDSA"); // <= client-side hashing
        System.out.println(verifyDEREncodedSignature(keyPair.getPublic(), sig, content, "SHA256withECDSA"));

    }

}
