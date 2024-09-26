import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MailClient extends JFrame {
    private static final int SERVER_PORT = 9876;
    private static final String SERVER_ADDRESS = "localhost";
    private DatagramSocket socket;

    private JTextArea textArea;
    private JTextField usernameField;
    private JTextField passwordField;

    private String username; // Khai báo biến username

    public MailClient() {
        setTitle("Mail Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textArea = new JTextArea();
        textArea.setEditable(false);

        usernameField = new JTextField();
        passwordField = new JTextField();

        JButton registerButton = new JButton("Register");
        JButton loginButton = new JButton("Login");

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                registerUser(usernameField.getText(), passwordField.getText());
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                username = usernameField.getText(); // Gán username khi login
                loginUser(username, passwordField.getText());
            }
        });

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(registerButton);
        panel.add(loginButton);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        setVisible(true);

        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerUser(String username, String password) {
        String message = "REGISTER," + username + "," + password + "," + username; // Name same as username
        sendMessage(message);
    }

    private void loginUser(String username, String password) {
        String message = "LOGIN," + username + "," + password;
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
            textArea.append("Server response: " + response + "\n");

            if (response.contains("Login successful")) {
                new MailDashboard(username); // Mở dashboard gửi mail
                this.dispose(); // Đóng cửa sổ đăng nhập
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MailClient();
    }
}
