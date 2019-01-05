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

    public static PublicKey getPublicKeyFromPEM(String fileName, String algo) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        FileReader reader = new FileReader(fileName);
        PemReader pemReader = new PemReader(reader);
        KeyFactory keyFactory = KeyFactory.getInstance(algo);
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();

        return keyFactory.generatePublic(new X509EncodedKeySpec(content));
    }
    //----------------------------------------------for file handling end------------------------------------------------


    //----------------------------------------------for key start------------------------------------------------

    public static KeyPair generateECKeyPair(String curve) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec(curve), new SecureRandom());
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

    //----------------------------------------------for key end------------------------------------------------

    //----------------------------------------------for signature start------------------------------------------------


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

    //----------------------------------------------for signature end------------------------------------------------



}
