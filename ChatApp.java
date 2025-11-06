import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;
import java.security.MessageDigest;

public class ChatApp extends JFrame {

    // PostgreSQL connection details (from Render environment variables)
    static final String URL = System.getenv("DB_URL");
    static final String USER = System.getenv("DB_USER");
    static final String PASS = System.getenv("DB_PASS");

    private int userId = -1;
    private String username = "";

    // Swing components
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField usernameField, recipientField;
    private JPasswordField passwordField;
    private JTextArea messageArea, inboxArea;
    private JList<String> usersList;

    public ChatApp() {
        setTitle("Chat App - Dark Mode");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(loginPanel(), "Login");
        mainPanel.add(chatPanel(), "Chat");

        add(mainPanel);
        cardLayout.show(mainPanel, "Login");
        setVisible(true);
    }

    private JPanel loginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        applyDark(panel);

        GridBagConstraints gbc = new GridBagConstraints();

        JLabel userLabel = new JLabel("Username:");
        applyDark(userLabel);
        usernameField = new JTextField(20);
        usernameField.setBackground(Color.DARK_GRAY);
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);

        JLabel passLabel = new JLabel("Password:");
        applyDark(passLabel);
        passwordField = new JPasswordField(20);
        passwordField.setBackground(Color.DARK_GRAY);
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        applyDark(loginButton);
        applyDark(registerButton);

        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(userLabel, gbc);
        gbc.gridx = 1; panel.add(usernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(passLabel, gbc);
        gbc.gridx = 1; panel.add(passwordField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(loginButton, gbc);
        gbc.gridx = 1; panel.add(registerButton, gbc);

        return panel;
    }

    private JPanel chatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        applyDark(panel);

        JLabel userLabel = new JLabel();
        applyDark(userLabel);
        panel.add(userLabel, BorderLayout.NORTH);

        inboxArea = new JTextArea();
        inboxArea.setEditable(false);
        inboxArea.setBackground(Color.BLACK);
        inboxArea.setForeground(Color.WHITE);
        JScrollPane inboxScroll = new JScrollPane(inboxArea);
        panel.add(inboxScroll, BorderLayout.CENTER);

        usersList = new JList<>();
        usersList.setBackground(Color.DARK_GRAY);
        usersList.setForeground(Color.WHITE);
        JScrollPane usersScroll = new JScrollPane(usersList);
        usersScroll.setPreferredSize(new Dimension(150, 0));
        panel.add(usersScroll, BorderLayout.EAST);

        JPanel sendPanel = new JPanel(new BorderLayout());
        applyDark(sendPanel);

        recipientField = new JTextField(10);
        recipientField.setBackground(Color.DARK_GRAY);
        recipientField.setForeground(Color.WHITE);
        recipientField.setCaretColor(Color.WHITE);

        messageArea = new JTextArea(3, 30);
        messageArea.setBackground(Color.BLACK);
        messageArea.setForeground(Color.WHITE);
        messageArea.setCaretColor(Color.WHITE);

        JButton sendButton = new JButton("Send");
        JButton refreshButton = new JButton("Refresh");
        JButton signOutButton = new JButton("Sign Out");
        applyDark(sendButton);
        applyDark(refreshButton);
        applyDark(signOutButton);

        sendButton.addActionListener(e -> sendMessage());
        refreshButton.addActionListener(e -> loadInboxAndUsers());
        signOutButton.addActionListener(e -> {
            userId = -1;
            username = "";
            messageArea.setText("");
            recipientField.setText("");
            inboxArea.setText("");
            passwordField.setText("");
            cardLayout.show(mainPanel, "Login");
        });

        JPanel inputPanel = new JPanel();
        applyDark(inputPanel);
        inputPanel.add(new JLabel("To:"));
        inputPanel.getComponent(0).setForeground(Color.WHITE);
        inputPanel.add(recipientField);
        inputPanel.add(sendButton);
        inputPanel.add(refreshButton);
        inputPanel.add(signOutButton);

        sendPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        sendPanel.add(inputPanel, BorderLayout.SOUTH);
        panel.add(sendPanel, BorderLayout.SOUTH);

        panel.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                userLabel.setText("Logged in as: " + username);
                userLabel.setForeground(Color.WHITE);
                loadInboxAndUsers();
            }
        });

        return panel;
    }

    private void applyDark(JComponent comp) {
        comp.setBackground(Color.DARK_GRAY);
        comp.setForeground(Color.WHITE);
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void register() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword()).trim();
        if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter username and password"); return; }

        String hashed = hashPassword(p);

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password) VALUES(?,?)")) {
            ps.setString(1, u);
            ps.setString(2, hashed);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Registered successfully!");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void login() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword()).trim();
        if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter username and password"); return; }

        String hashed = hashPassword(p);

        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username=? AND password=?")) {
            ps.setString(1, u);
            ps.setString(2, hashed);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                userId = rs.getInt(1);
                username = u;
                cardLayout.show(mainPanel, "Chat");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void sendMessage() {
        String toUser = recipientField.getText().trim();
        String msg = messageArea.getText().trim();
        if (toUser.isEmpty() || msg.isEmpty()) { JOptionPane.showMessageDialog(this, "Fill recipient and message"); return; }

        try (Connection c = connect()) {
            int rid = getUserId(toUser, c);
            if (rid == -1) { JOptionPane.showMessageDialog(this, "User not found!"); return; }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages(sender_id,receiver_id,content) VALUES(?,?,?)")) {
                ps.setInt(1, userId);
                ps.setInt(2, rid);
                ps.setString(3, msg);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Message sent!");
                messageArea.setText("");
                loadInboxAndUsers();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private int getUserId(String name, Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username=?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private void loadInboxAndUsers() {
        try (Connection c = connect()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT m.id,u.username,m.content,m.sent_at FROM messages m JOIN users u ON m.sender_id=u.id WHERE receiver_id=? ORDER BY m.sent_at DESC")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    sb.append("[").append(rs.getTimestamp("sent_at")).append("] ")
                      .append(rs.getString("username")).append(": ")
                      .append(rs.getString("content")).append("\n");
                }
                inboxArea.setText(sb.toString());
            }

            try (PreparedStatement ps = c.prepareStatement("SELECT username FROM users")) {
                ResultSet rs = ps.executeQuery();
                Vector<String> userVector = new Vector<>();
                while (rs.next()) userVector.add(rs.getString(1));
                usersList.setListData(userVector);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName("org.postgresql.Driver");
            SwingUtilities.invokeLater(ChatApp::new);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null, "PostgreSQL JDBC Driver not found!");
        }
    }
}
