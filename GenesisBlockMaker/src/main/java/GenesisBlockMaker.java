import blockchain.block.AuthorityInfo;
import blockchain.block.Block;
import config.Configuration;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;

class GenesisBlockMaker {

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, NoSuchProviderException {

        Scanner sc = new Scanner(System.in);
        System.out.print("How many authorities: ");
        int num = sc.nextInt();

        AuthorityInfo[] initialAuthorities = new AuthorityInfo[num];

        for (int i = 0; i < num; ++i) {

            System.out.print("Authority " + i + " public key file name:");
            String curPubKeyFile = sc.next();

            String curAuthorityName;
            do {
                System.out.print("Authority " + i + " name:");
                curAuthorityName = sc.next();
                if(curAuthorityName.length()>Configuration.MAX_NAME_LENGTH)
                    System.out.println("Name cannot be larger than "+Configuration.MAX_NAME_LENGTH);
            }
            while(curAuthorityName.length()>Configuration.MAX_NAME_LENGTH);

            initialAuthorities[i] = new AuthorityInfo(curAuthorityName, (ECPublicKey) SecurityHelper.getPublicKeyFromPEM(curPubKeyFile, "EC"));
        }

        byte[] zeros = new byte[Configuration.HASH_LENGTH];
        Block genesisBlock = new Block(null, null, initialAuthorities
                , 0, (byte) 0, zeros, null
                , null, null, null,null);

        try (FileOutputStream os = new FileOutputStream(Configuration.GENESISBLOCK_FILE)){
            os.write(genesisBlock.getRaw());
            os.close();
            System.out.println("Genesis Block Created. Block Hash: " + GeneralHelper.bytesToStringHex(genesisBlock.calculateHash()));
            System.out.println("Initial Authorities:");
            for (int i = 0; i < initialAuthorities.length; ++i)
                System.out.println(i + ": " + GeneralHelper.bytesToStringHex(initialAuthorities[i].getPublicKey().getEncoded()));
        }

    }

}
