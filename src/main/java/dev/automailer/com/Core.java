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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class Core extends JFrame {

    private JTextField txtFilePath;
    private JTextField txtSubject;
    private JTextArea txtMessage;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JProgressBar progressBar;
    private Session session;

    public Core() {
        createUI();
    }

    private void createUI() {
        setTitle("Automatsko slanje mailova - Marin Dujmović");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/logo.png"))).getImage());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

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

        JLabel lblFilePath = new JLabel("Popis adresa:");
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
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtMessage);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        JButton btnSend = new JButton("Posalji mail");
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(btnSend, gbc);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        panel.add(progressBar, gbc);

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

            new Thread(() -> {
                try {
                    session = authenticate(email, password);
                    List<String> sentEmails = sendEmailsFromFile(session, email, filePath, subject, messageBody);

                    SwingUtilities.invokeLater(() -> {
                        if (!sentEmails.isEmpty()) {
                            txtSubject.setText("");
                            txtMessage.setText("");
                            showSentEmailsDialog(sentEmails);
                        }
                        progressBar.setValue(0);  // Reset progress bar
                    });
                } catch (Exception ex) {
                    ex.fillInStackTrace();
                    SwingUtilities.invokeLater(() -> showErrorDialog(ex));
                }
            }).start();
        });

        // Set a modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            showErrorDialog(ex);
        }
    }

    private Session authenticate(String email, String password) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        // Test the authentication
        Transport transport = session.getTransport("smtp");
        transport.connect("smtp.gmail.com", email, password);
        transport.close();

        return session;
    }

    private List<String> sendEmailsFromFile(Session session, String email, String filePath, String subject, String messageBody) {
        List<String> sentEmails = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String recipient;
            List<Future<?>> futures = new ArrayList<>();
            List<String> allEmails = new ArrayList<>();
            while ((recipient = br.readLine()) != null) {
                allEmails.add(recipient);
            }

            SwingUtilities.invokeLater(() -> {
                progressBar.setMaximum(allEmails.size());
                progressBar.setValue(0);
            });

            for (String recipientEmail : allEmails) {
                futures.add(executor.submit(() -> {
                    try {
                        sendEmail(session, email, recipientEmail, subject, messageBody);
                        synchronized (sentEmails) {
                            sentEmails.add(recipientEmail);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(sentEmails.size()));
                        }
                    } catch (MessagingException e) {
                        e.fillInStackTrace();
                        // Log the error and continue with the next email
                        System.err.println("Failed to send email to: " + recipientEmail + " - " + e.getMessage());
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();  // wait for all tasks to complete
                } catch (ExecutionException | InterruptedException e) {
                    e.fillInStackTrace();
                }
            }

        } catch (IOException e) {
            e.fillInStackTrace();
            SwingUtilities.invokeLater(() -> showErrorDialog(e));
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                SwingUtilities.invokeLater(() -> showErrorDialog(e));
            }
        }
        return sentEmails;
    }

    private void sendEmail(Session session, String fromEmail, String recipient, String subject, String messageBody) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setText(messageBody);

        Transport.send(message);
        System.out.println("Email sent to: " + recipient);
    }

    private void showErrorDialog(Throwable throwable) {
        JDialog dialog = new JDialog(this, "Error", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        textArea.setText(sw.toString());

        dialog.add(new JScrollPane(textArea));
        dialog.setVisible(true);
    }

    private void showSentEmailsDialog(List<String> sentEmails) {
        JFrame frame = new JFrame("Poslani mailovi");
        frame.setSize(300, 200);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);

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