package blockchain.block;

import config.Configuration;
import exception.BlockChainObjectParsingException;
import general.security.SecurityHelper;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MedicalOrgInfoTest {

    @Test
    void rawAndParseTest() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, BlockChainObjectParsingException, SignatureException, InvalidKeyException {


        KeyPair testMedicalOrgKeyPair = SecurityHelper.generateECKeyPair(Configuration.ELIPTIC_CURVE);
        ECPublicKey testMedicalOrgPublicKey =(ECPublicKey) testMedicalOrgKeyPair.getPublic();
        ECPrivateKey testMedicalOrgPrivateKey =(ECPrivateKey) testMedicalOrgKeyPair.getPrivate();
        MedicalOrgInfo testMedicalOrgInfo = new MedicalOrgInfo("test", testMedicalOrgPublicKey);

        byte[] raw =testMedicalOrgInfo.getRaw();

        MedicalOrgInfo parsedMedicalOrgInfo = MedicalOrgInfo.parse(raw);
        assertEquals(testMedicalOrgInfo,parsedMedicalOrgInfo);
    }

}
