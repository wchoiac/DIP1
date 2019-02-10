import general.utility.Helper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.nio.file.FileSystems;
import java.nio.file.Path;


public class Receptionist {

    public static void main(String[] args) throws Exception {
        Path path = FileSystems.getDefault().getPath(".").toAbsolutePath().normalize();
        GlobalVar.cwd = path.toString();
        System.out.println(GlobalVar.cwd);
        JFrame frame = new JFrame("Receptionist Application");
        frame.setSize(1200, 900);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        Receptionist swing = new Receptionist();
        swing.placeComponents(panel, frame);
        frame.add(panel);
        frame.setVisible(true);


    }

    public void placeComponents(JPanel panel, JFrame frame) throws Exception {
        panel.setLayout(null);

        WebcamScan webcam = new WebcamScan();

        GlobalVar.ListMenuString.setBounds(450,20,300,25);
        panel.add(GlobalVar.ListMenuString);

        JButton RegistrateButton = new JButton("Scan QR code (Registration) :");
        RegistrateButton.setHorizontalAlignment(SwingConstants.LEFT);
        RegistrateButton.setBounds(10,20,250,25);
        RegistrateButton.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent e) {
                GlobalVar.Scantype = 1;
                webcam.setVisible(true);
            }
        });
        panel.add(RegistrateButton);

        JButton ScanAESButton = new JButton("Scan QR code (After Consultation) :");
        ScanAESButton.setBounds(10,60,250,25);
        ScanAESButton.setHorizontalAlignment(SwingConstants.LEFT);
        ScanAESButton.addActionListener(new ActionListener()  {
            public void actionPerformed(ActionEvent e) {
                GlobalVar.Scantype = 2;
                webcam.setVisible(true);
            }
        });
        panel.add(ScanAESButton);

        JButton GenHashButton = new JButton("Generate QR code :");
        GenHashButton.setBounds(10,100,250,25);
        GenHashButton.setHorizontalAlignment(SwingConstants.LEFT);
        GenHashButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = GlobalVar.ListMenuString.getSelectedIndex();
                if (GlobalVar.QRoutput.get(index) != null)
                {
                    Helper.drawQRCode(null,GlobalVar.QRoutput.get(index));
                }
                else
                    System.out.println("No Record");
            }
        });
        panel.add(GenHashButton);

        JLabel MenuLabel = new JLabel("Patients : ");
        MenuLabel.setBounds(380,20,70,25);
        panel.add(MenuLabel);

        JButton ScanSignButton = new JButton("Scan QR code (Patient Signature) :");
        ScanSignButton.setBounds(10,140,250,25);
        ScanSignButton.setHorizontalAlignment(SwingConstants.LEFT);
        ScanSignButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GlobalVar.Scantype = 3;
                webcam.setVisible(true);
            }
        });
        panel.add(ScanSignButton);
    }
}
