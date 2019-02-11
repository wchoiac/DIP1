import general.security.SecurityHelper;
import general.utility.GeneralHelper;
import general.utility.Helper;
import org.bouncycastle.math.ec.ScaleYPointMap;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;

public class Login {
    public static void main(String[] args) throws Exception {
        InetAddress inetAddress = InetAddress.getByName("25.43.79.11");
        GlobalVar.fullNodeRestClient = new FullNodeRestClient(inetAddress, SecurityHelper.getX509FromDER(new File("med0.cer")));
        JFrame frame = new JFrame("Testing Application");
        frame.setSize(300, 200);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        Login swing = new Login();
        swing.placeComponents(panel, frame);
        frame.add(panel);
        frame.setVisible(true);
    }

    public void placeComponents(JPanel panel, JFrame frame) throws Exception {
        panel.setLayout(null);
        JLabel userLabel = new JLabel("User:");
        userLabel.setBounds(10,20,80,25);
        panel.add(userLabel);
        JTextField userText = new JTextField(20);
        userText.setBounds(100,20,165,25);
        panel.add(userText);
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(10,50,80,25);
        panel.add(passwordLabel);
        JPasswordField passwordText = new JPasswordField(20);
        passwordText.setBounds(100,50,165,25);
        panel.add(passwordText);
        JButton loginButton = new JButton("login");
        loginButton.setBounds(10, 80, 80, 25);
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    GlobalVar.fullNodeRestClient.login(userText.getText(), passwordText.getText().toCharArray());
                    //GlobalVar.fullNodeRestClient.login("user", "1234".toCharArray());
                    System.out.println(GeneralHelper.bytesToStringHex(GlobalVar.fullNodeRestClient.getMedicalOrgIdentifier()));
                    frame.dispose();
                    Receptionist receptionist = new Receptionist();
                    receptionist.main(null);
                } catch (Exception e1){
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(null,"Invalid user name or password!");
                }
            }
        });
        panel.add(loginButton);
    }
}
