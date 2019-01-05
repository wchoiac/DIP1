package config;

import java.io.File;

public class Configuration {

    public static final File API_DATA_FOLDER= new File("apiData");
    public static final File USERINFO_FOLDER = new File(API_DATA_FOLDER,"userInfo");
    public static final String PASSWORD_HASH_ALGORITHM ="PBKDF2WithHmacSHA512";
    public static final int PASSWORD_HASH_LENGTH =64;
    public static final int PASSWORD_SALT_LENGTH =16;
    public static final byte ROOT_USER_LEVEL =0;
    public static final byte USER_LEVEL =1;

}
