package dev.automailer.com;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class Core extends JFrame {

    private JTextField txtFilePath;
    private JTextField txtSubject;
    private JTextArea txtMessage;
    private JTextField txtEmail;
    private JPasswordField txtPassword;

    public Core() {
        createUI();
    }

    private void createUI() {
        setTitle("Automatsko slanje mailova - Marin Dujmović");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/logo.png"))).getImage());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblEmail = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblEmail, gbc);

        txtEmail = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(txtEmail, gbc);

        JLabel lblPassword = new JLabel("Lozinka:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(txtPassword, gbc);

        JLabel lblFilePath = new JLabel("Put do adresa:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblFilePath, gbc);

        txtFilePath = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(txtFilePath, gbc);

        JButton btnBrowse = new JButton("Trazilica");
        gbc.gridx = 2;
        gbc.gridy = 2;
        panel.add(btnBrowse, gbc);

        JLabel lblSubject = new JLabel("Subjekt:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(lblSubject, gbc);

        txtSubject = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(txtSubject, gbc);

        JLabel lblMessage = new JLabel("Sadrzaj:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(lblMessage, gbc);

        txtMessage = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(txtMessage);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(scrollPane, gbc);

        JButton btnSend = new JButton("Posalji mail");
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(btnSend, gbc);

        add(panel);

        btnBrowse.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                txtFilePath.setText(selectedFile.getAbsolutePath());
            }
        });

        btnSend.addActionListener(e -> {
            String email = txtEmail.getText();
            String password = new String(txtPassword.getPassword());
            String filePath = txtFilePath.getText();
            String subject = txtSubject.getText();
            String messageBody = txtMessage.getText();
            List<String> sentEmails = sendEmailsFromFile(email, password, filePath, subject, messageBody);

            if (!sentEmails.isEmpty()) {
                txtSubject.setText("");
                txtMessage.setText("");
                showSentEmailsDialog(sentEmails);
            }
        });
    }

    private List<String> sendEmailsFromFile(String email, String password, String filePath, String subject, String messageBody) {
        List<String> sentEmails = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String recipient;
            while ((recipient = br.readLine()) != null) {
                sendEmail(email, password, recipient, subject, messageBody);
                sentEmails.add(recipient);
            }
        } catch (IOException e) {
            e.fillInStackTrace();
        }
        return sentEmails;
    }

    private void sendEmail(String username, String password, String recipient, String subject, String messageBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);

            System.out.println("Email poslan na: " + recipient);

        } catch (MessagingException e) {
            e.fillInStackTrace();
        }
    }

    private void showSentEmailsDialog(List<String> sentEmails) {
        JFrame frame = new JFrame("Poslani mailovi");
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        StringBuilder emailList = new StringBuilder();
        for (String email : sentEmails) {
            emailList.append(email).append("\n");
        }
        textArea.setText(emailList.toString());

        frame.add(new JScrollPane(textArea));
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Core().setVisible(true));
    }
}