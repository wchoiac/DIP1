package viewmodel;

public class Config {

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    public static final int QRCODE_HEIGHT = 300;
    public static final int QRCODE_WIDTH = 300;

    public static final String CSS_STYLES = Config.class.getClassLoader().getResource("css/styles.css").toExternalForm();
}
