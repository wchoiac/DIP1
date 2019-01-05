package rest.server.manager;

import config.Configuration;
import general.security.SecurityHelper;
import rest.pojo.UserInfoPojo;
import rest.server.exception.InvalidUserInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SessionManager {

    public static String login(UserInfoPojo userInfoPojo) throws IOException, InvalidUserInfo {
        File userInfoFile = new File(Configuration.USERINFO_FOLDER, userInfoPojo.getUsername());
        if (!userInfoFile.exists()) {
            throw new InvalidUserInfo();
        } else {

            byte[] userInfoAllBytes = Files.readAllBytes(userInfoFile.toPath());//level(1byte) | salt(16 bytes) | password hash| secureToken()

            byte level = userInfoAllBytes[0];
            byte[] salt = Arrays.copyOfRange(userInfoAllBytes,1,1+Configuration.PASSWORD_SALT_LENGTH);
            byte[] passwordHash = Arrays.copyOfRange(userInfoAllBytes,1+Configuration.PASSWORD_SALT_LENGTH,
                    1+Configuration.PASSWORD_SALT_LENGTH+Configuration.PASSWORD_HASH_LENGTH);

            byte[] passwordHashBasedOnUserInput = null;
            try {
                passwordHashBasedOnUserInput = SecurityHelper.passwordHash(userInfoPojo.getPassword(),salt,Configuration.PASSWORD_HASH_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace(); // not expected
            }

            if (Arrays.equals(passwordHash, passwordHashBasedOnUserInput)) {
                if (userInfoAllBytes.length != 1+Configuration.PASSWORD_SALT_LENGTH+Configuration.PASSWORD_HASH_LENGTH) {
                    String preToken = new String(Arrays.copyOfRange(userInfoAllBytes
                            ,1+Configuration.PASSWORD_SALT_LENGTH+Configuration.PASSWORD_HASH_LENGTH
                            ,userInfoAllBytes.length));
                    File prevSessionFile = new File(Configuration.SESSION_FOLDER, preToken);

                    if (prevSessionFile.exists())
                        prevSessionFile.delete();
                }

                String newToken = SecurityHelper.generateSecureToken();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(Configuration.SESSION_FOLDER, newToken)));
                bos.write(level);
                bos.write(userInfoPojo.getUsername().getBytes()); // level | username
                bos.close();

                bos = new BufferedOutputStream(new FileOutputStream(userInfoFile));
                bos.write(level);
                bos.write(salt);
                bos.write(passwordHash);
                bos.write(newToken.getBytes());
                bos.close();

                return newToken;
            } else {
                throw new InvalidUserInfo();
            }
        }
    }


    public static String getUserName(String token) throws IOException {

        File tokenFile = new File(Configuration.SESSION_FOLDER, token);

        if (!tokenFile.exists())
            return null;

        byte[] sessionInfoAllBytes = Files.readAllBytes(tokenFile.toPath()); // level | username

        return new String(Arrays.copyOfRange(sessionInfoAllBytes,1,sessionInfoAllBytes.length));

    }

    public static boolean isValidLevelToken(String token, byte level) throws IOException {
        File sessionFile = new File(Configuration.SESSION_FOLDER, token);
        if (!sessionFile.exists())
            return false;
        byte[] sessionInfoAllBytes = Files.readAllBytes(sessionFile.toPath()); // level | username

        return sessionInfoAllBytes[0]==level;
    }
}
