import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class MailServer extends JFrame {
    private static final int PORT = 9876;
    private JTextArea textArea;
    private DefaultMutableTreeNode root;
    private JTree userTree;
    private JTextArea userInfoArea;

    public MailServer() {
        setTitle("Mail Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        textArea = new JTextArea();
        textArea.setEditable(false);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Tạo cây thư mục
        root = new DefaultMutableTreeNode("Users");
        userTree = new JTree(root);
        userTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) userTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode != root) {
                    // Hiện thông tin người dùng hoặc mail khi nhấp vào thư mục
                    showUserInfo(selectedNode);
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(userTree), BorderLayout.CENTER);

        userInfoArea = new JTextArea();
        userInfoArea.setEditable(false);
        panel.add(new JScrollPane(userInfoArea), BorderLayout.SOUTH);

        add(panel, BorderLayout.EAST);

        setVisible(true);

        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(PORT)) {
                textArea.append("Mail Server is running...\n");

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    new Thread(new MailHandler(socket, packet, this)).start(); // Truyền tham chiếu đến MailServer
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    class MailHandler implements Runnable {
        private DatagramSocket socket;
        private DatagramPacket packet;
        private MailServer server;

        public MailHandler(DatagramSocket socket, DatagramPacket packet, MailServer server) {
            this.socket = socket;
            this.packet = packet;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                String message = new String(packet.getData(), 0, packet.getLength());
                String[] parts = message.split(",", 5);
                String command = parts[0];

                switch (command) {
                    case "REGISTER":
                        registerUser(parts[1], parts[2], parts[3]);
                        break;
                    case "LOGIN":
                        loginUser(parts[1], parts[2]);
                        break;
                    case "SENDMAIL":
                        sendMail(parts[1], parts[2], parts[3], parts[4]);
                        break;
                    case "GETMAIL":
                        getMail(parts[1]);
                        break;
                    default:
                        sendResponse("Unknown command!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void registerUser(String username, String password, String name) {
            try {
                Path userDir = Paths.get("users/" + username);
                Files.createDirectories(userDir);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(userDir.resolve("info.txt").toFile()))) {
                    writer.write("Username: " + username);
                    writer.newLine();
                    writer.write("Password: " + password);
                    writer.newLine();
                    writer.write("Name: " + name);
                }

                sendResponse("User registered successfully!");
                textArea.append("User " + username + " registered.\n");

                // Thêm thư mục cho người dùng
                DefaultMutableTreeNode userNode = new DefaultMutableTreeNode(username);
                DefaultMutableTreeNode infoNode = new DefaultMutableTreeNode("Info");
                DefaultMutableTreeNode mailNode = new DefaultMutableTreeNode("Mails");

                userNode.add(infoNode);
                userNode.add(mailNode);
                root.add(userNode);
                ((DefaultTreeModel) userTree.getModel()).reload();

            } catch (IOException e) {
                sendResponse("Registration failed!");
                e.printStackTrace();
            }
        }

        private void loginUser(String username, String password) {
            try {
                List<String> lines = Files.readAllLines(Paths.get("users/" + username + "/info.txt"));
                String storedPassword = lines.get(1).split(": ")[1];

                if (storedPassword.equals(password)) {
                    sendResponse("Login successful!");
                } else {
                    sendResponse("Invalid password!");
                }
            } catch (IOException e) {
                sendResponse("User not found!");
            }
        }

        private void sendMail(String from, String to, String subject, String content) {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                String mail = "From: " + from + "\nSubject: " + subject + "\nContent: " + content + "\nDate: " + timestamp;
                Files.createDirectories(Paths.get("users/" + to + "/mails"));
                Files.write(Paths.get("users/" + to + "/mails/" + System.currentTimeMillis() + ".txt"), mail.getBytes(), StandardOpenOption.CREATE);
                sendResponse("Mail sent successfully!");
                textArea.append("Mail sent from " + from + " to " + to + ".\n");
            } catch (IOException e) {
                sendResponse("Failed to send mail!");
            }
        }

        private void getMail(String username) {
            try {
                List<String> mails = new ArrayList<>();
                Files.list(Paths.get("users/" + username + "/mails")).forEach(path -> {
                    try {
                        mails.add(new String(Files.readAllBytes(path)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                StringBuilder response = new StringBuilder("Mails:\n");
                for (String mail : mails) {
                    response.append(mail).append("\n\n");
                }
                sendResponse(response.toString());
            } catch (IOException e) {
                sendResponse("No mails found!");
            }
        }

        private void sendResponse(String response) {
            try {
                byte[] buffer = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Phương thức hiển thị thông tin người dùng
    public void showUserInfo(DefaultMutableTreeNode selectedNode) {
        DefaultMutableTreeNode userNode = (DefaultMutableTreeNode) selectedNode.getParent();
        String username = userNode.toString(); // Lấy tên người dùng từ nút cha

        if (selectedNode.toString().equals("Info")) {
            // Hiển thị thông tin người dùng
            try {
                Path userDir = Paths.get("users/" + username);
                List<String> lines = Files.readAllLines(userDir.resolve("info.txt"));
                StringBuilder userInfo = new StringBuilder();
                for (String line : lines) {
                    userInfo.append(line).append("\n");
                }
                userInfoArea.setText(userInfo.toString());
            } catch (IOException e) {
                userInfoArea.setText("Error retrieving user info.");
            }
        } else if (selectedNode.toString().equals("Mails")) {
            // Hiển thị danh sách các client đã gửi mail cho Client A
            try {
                Path mailDir = Paths.get("users/" + username + "/mails");
                Map<String, List<Path>> clientMails = new HashMap<>();

                Files.list(mailDir).forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        String from = content.split("\n")[0].split(": ")[1]; // Lấy tên client gửi
                        clientMails.putIfAbsent(from, new ArrayList<>());
                        clientMails.get(from).add(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                // Hiển thị danh sách client
                StringBuilder clientList = new StringBuilder("Clients who sent mails:\n");
                for (String client : clientMails.keySet()) {
                    clientList.append(client).append("\n");
                    // Thêm vào cây
                    selectedNode.add(new DefaultMutableTreeNode(client));
                }
                userInfoArea.setText(clientList.toString());
                ((DefaultTreeModel) userTree.getModel()).reload();

                // Lắng nghe sự kiện chọn client
                userTree.addTreeSelectionListener(new TreeSelectionListener() {
                    @Override
                    public void valueChanged(TreeSelectionEvent e) {
                        DefaultMutableTreeNode selectedClientNode = (DefaultMutableTreeNode) userTree.getLastSelectedPathComponent();
                        if (selectedClientNode != null && clientMails.containsKey(selectedClientNode.toString())) {
                            showClientMailDetails(selectedClientNode.toString(), clientMails);
                        }
                    }
                });

            } catch (IOException e) {
                userInfoArea.setText("Error retrieving mails.");
            }
        }
    }


    // Hiển thị nội dung email từ một client cụ thể
    private void showClientMailDetails(String clientName, Map<String, List<Path>> clientMails) {
        StringBuilder mailDetails = new StringBuilder("Mails from " + clientName + ":\n");

        // Hiển thị tất cả các mail từ client
        try {
            for (Path mailPath : clientMails.get(clientName)) {
                String content = new String(Files.readAllBytes(mailPath));
                String[] mailParts = content.split("\n");
                String subject = Arrays.stream(mailParts).filter(line -> line.startsWith("Subject:")).findFirst().orElse("No Subject");
                String date = Arrays.stream(mailParts).filter(line -> line.startsWith("Date:")).findFirst().orElse("No Date");

                mailDetails.append("Subject: ").append(subject).append("\n");
                mailDetails.append("Date: ").append(date).append("\n");
                mailDetails.append("Content: ").append(content).append("\n\n");
            }
            userInfoArea.setText(mailDetails.toString());
        } catch (IOException e) {
            userInfoArea.setText("No mails found from " + clientName);
        }
    }


    // Thêm phương thức để hiển thị nội dung mail chi tiết
    public void showMailDetail(String mailContent) {
        userInfoArea.setText(mailContent);
    }

    public static void main(String[] args) {
        new MailServer();
    }
}
