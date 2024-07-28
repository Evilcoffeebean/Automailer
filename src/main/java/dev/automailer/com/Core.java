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
    private JComboBox<String> cmbEmailProvider;
    private Session session;

    private static final Map<String, String[]> SMTP_SETTINGS = new HashMap<>();

    static {
        SMTP_SETTINGS.put("Gmail", new String[]{"smtp.gmail.com", "587"});
        SMTP_SETTINGS.put("Yahoo", new String[]{"smtp.mail.yahoo.com", "587"});
        SMTP_SETTINGS.put("iCloud", new String[]{"smtp.mail.me.com", "587"});
        SMTP_SETTINGS.put("Outlook", new String[]{"smtp.office365.com", "587"});
        SMTP_SETTINGS.put("AOL", new String[]{"smtp.aol.com", "587"});
        SMTP_SETTINGS.put("GMX", new String[]{"mail.gmx.com", "587"});
        SMTP_SETTINGS.put("Yandex", new String[]{"smtp.yandex.com", "465"});
        SMTP_SETTINGS.put("Zoho", new String[]{"smtp.zoho.com", "587"});
        SMTP_SETTINGS.put("Mail.com", new String[]{"smtp.mail.com", "587"});
        SMTP_SETTINGS.put("ProtonMail", new String[]{"127.0.0.1", "1025"});  // Use with Mailvelope
        SMTP_SETTINGS.put("Tutanota", new String[]{"smtp.tutanota.com", "587"});
        SMTP_SETTINGS.put("FastMail", new String[]{"smtp.fastmail.com", "587"});
        SMTP_SETTINGS.put("1&1", new String[]{"smtp.1and1.com", "587"});
    }

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

        JLabel lblEmailProvider = new JLabel("Provider:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblEmailProvider, gbc);

        cmbEmailProvider = new JComboBox<>(SMTP_SETTINGS.keySet().toArray(new String[0]));
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(cmbEmailProvider, gbc);

        JLabel lblEmail = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(lblEmail, gbc);

        txtEmail = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(txtEmail, gbc);

        JLabel lblPassword = new JLabel("Lozinka:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(txtPassword, gbc);

        JLabel lblFilePath = new JLabel("Popis adresa:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(lblFilePath, gbc);

        txtFilePath = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(txtFilePath, gbc);

        JButton btnBrowse = new JButton("Trazilica");
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(btnBrowse, gbc);

        JLabel lblSubject = new JLabel("Subjekt:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(lblSubject, gbc);

        txtSubject = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        panel.add(txtSubject, gbc);

        JLabel lblMessage = new JLabel("Sadrzaj:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(lblMessage, gbc);

        txtMessage = new JTextArea(5, 20);
        txtMessage.setLineWrap(true);
        txtMessage.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtMessage);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(scrollPane, gbc);

        JButton btnSend = new JButton("Posalji mail");
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        panel.add(btnSend, gbc);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 7;
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
            String provider = (String) cmbEmailProvider.getSelectedItem();

            new Thread(() -> {
                try {
                    session = authenticate(email, password, provider);
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

    private Session authenticate(String email, String password, String provider) throws MessagingException {
        String[] settings = SMTP_SETTINGS.get(provider);
        String host = settings[0];
        String port = settings[1];

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        // Test the authentication
        Transport transport = session.getTransport("smtp");
        transport.connect(host, email, password);
        transport.close();

        return session;
    }

    private List<String> sendEmailsFromFile(Session session, String email, String filePath, String subject, String messageBody) {
        List<String> sentEmails = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Semaphore semaphore = new Semaphore(5);  // Limit concurrent connections to avoid server limitations

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

            int emailCount = allEmails.size();
            int sleepTime = Math.max(5000 / emailCount, 1000); // Adjust sleep time to avoid rate limiting

            for (String recipientEmail : allEmails) {
                futures.add(executor.submit(() -> {
                    try {
                        semaphore.acquire();  // Acquire a permit to proceed
                        sendEmailWithRetries(session, email, recipientEmail, subject, messageBody, sleepTime);
                        synchronized (sentEmails) {
                            sentEmails.add(recipientEmail);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(sentEmails.size()));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        semaphore.release();  // Release the permit
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

    private void sendEmailWithRetries(Session session, String fromEmail, String recipient, String subject, String messageBody, int sleepTime) throws InterruptedException {
        int retries = 3;
        while (retries > 0) {
            try {
                sendEmail(session, fromEmail, recipient, subject, messageBody);
                System.out.println("Email poslan na: " + recipient);
                return;
            } catch (MessagingException e) {
                retries--;
                if (retries == 0) {
                    System.err.println("Neuspjelo slanje na: " + recipient + " nakon opetovanih pokusaja");
                } else {
                    System.err.println("Pokusavam ponovno slati na: " + recipient + " (" + retries + " pokusaja ostalo)");
                    Thread.sleep(sleepTime);
                }
            }
        }
    }

    private void sendEmail(Session session, String fromEmail, String recipient, String subject, String messageBody) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setText(messageBody);

        Transport.send(message);
    }

    private void showErrorDialog(Throwable throwable) {
        JDialog dialog = new JDialog(this, "Problem", true);
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