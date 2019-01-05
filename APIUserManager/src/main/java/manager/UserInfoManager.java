package manager;

import config.Configuration;
import general.security.SecurityHelper;
import general.utility.GeneralHelper;

import java.io.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class UserInfoManager {

    public static boolean registerAPIUser(String username, char[] password, byte level) throws IOException, NoSuchAlgorithmException {

        if (!Configuration.USERINFO_FOLDER.exists())
            Configuration.USERINFO_FOLDER.mkdirs();

        File newUserFile = new File(Configuration.USERINFO_FOLDER, username);
        if (newUserFile.exists())
        return false;

        byte[] salt = SecurityHelper.generateSalt(Configuration.PASSWORD_SALT_LENGTH);
        byte[] passwordHash = SecurityHelper.passwordHash(password,salt,Configuration.PASSWORD_HASH_ALGORITHM);

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newUserFile));
        bos.write(level);
        bos.write(salt);
        bos.write(passwordHash);
        bos.close();


        return true;
    }

    public static boolean changeAPIUserPassword(String username, char[] password) throws IOException, NoSuchAlgorithmException {
        File userFile = new File(Configuration.USERINFO_FOLDER, username);
        if (!userFile.exists())
            return false;

        byte[] userInfoAllBytes = Files.readAllBytes(userFile.toPath());

        byte level = userInfoAllBytes[0];
        byte[] salt = SecurityHelper.generateSalt(Configuration.PASSWORD_SALT_LENGTH);
        byte[] passwordHash = SecurityHelper.passwordHash(password,salt,Configuration.PASSWORD_HASH_ALGORITHM);

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(userFile));

        bos.write(level);
        bos.write(salt);
        bos.write(passwordHash);
        if(userInfoAllBytes.length != 1+Configuration.PASSWORD_SALT_LENGTH+Configuration.PASSWORD_HASH_LENGTH) {
            bos.write (Arrays.copyOfRange(userInfoAllBytes
                    ,1+Configuration.PASSWORD_SALT_LENGTH+Configuration.PASSWORD_HASH_LENGTH
            ,userInfoAllBytes.length));
        }
        bos.close();

        return true;
    }
}
