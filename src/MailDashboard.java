import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MailDashboard extends JFrame {
    private static final int SERVER_PORT = 9876;
    private static final String SERVER_ADDRESS = "localhost";
    private DatagramSocket socket;
    private String username;

    public MailDashboard(String username) {
        this.username = username;
        setTitle("Mail Dashboard");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        JTextField inputField = new JTextField();
        JTextField recipientField = new JTextField();
        JTextField subjectField = new JTextField();

        JButton sendMailButton = new JButton("Send Mail");
        JButton getMailButton = new JButton("Get Mail");

        sendMailButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String recipient = recipientField.getText();
                String subject = subjectField.getText();
                String content = inputField.getText();
                sendMail(recipient, subject, content);
            }
        });

        getMailButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getMail(username);
            }
        });

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("To:"));
        panel.add(recipientField);
        panel.add(new JLabel("Subject:"));
        panel.add(subjectField);
        panel.add(new JLabel("Content:"));
        panel.add(inputField);
        panel.add(sendMailButton);
        panel.add(getMailButton);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        setVisible(true);
    }

    private void sendMail(String to, String subject, String content) {
        String mailMessage = "SENDMAIL," + username + "," + to + "," + subject + "," + content;
        sendMessage(mailMessage);
    }

    private void getMail(String username) {
        String message = "GETMAIL," + username;
        sendMessage(message);
    }

    private void sendMessage(String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(SERVER_ADDRESS), SERVER_PORT);
            socket.send(packet);

            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            JOptionPane.showMessageDialog(this, "Server response: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
