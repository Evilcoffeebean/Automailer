package dev.automailer.com;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledEditorKit;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Core extends JFrame {

    private JComboBox<String> cmbSavedEmails;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JTextField txtPort;
    private JComboBox<String> cmbSMTPServer;
    private JTextField txtFilePath;
    private JTextField txtSubject;
    private JTextPane txtMessage;
    private JProgressBar progressBar;
    private Session session;

    private JComboBox<String> cmbSavedMessages;
    private JButton btnAddMessage;
    private JButton btnRemoveMessage;

    private static final Map<String, String[]> SMTP_SERVERS = new HashMap<>();
    private static final List<File> attachments = new ArrayList<>();
    private static final List<ImageIcon> images = new ArrayList<>();
    private final Preferences preferences;

    static {
        SMTP_SERVERS.put("Gmail", new String[]{"smtp.gmail.com", "587"});
        SMTP_SERVERS.put("Yahoo", new String[]{"smtp.mail.yahoo.com", "587"});
        SMTP_SERVERS.put("Outlook", new String[]{"smtp.office365.com", "587"});
        SMTP_SERVERS.put("iCloud", new String[]{"smtp.mail.me.com", "587"});
        SMTP_SERVERS.put("AOL", new String[]{"smtp.aol.com", "587"});
        SMTP_SERVERS.put("Zoho", new String[]{"smtp.zoho.com", "587"});
        SMTP_SERVERS.put("Yandex", new String[]{"smtp.yandex.com", "465"});
    }

    public Core() {
        preferences = Preferences.userRoot().node(this.getClass().getName());
        createUI();
        loadSavedEmails();
        loadSavedMessages();  // Load saved messages
    }

    private void createUI() {
        setTitle("Automatsko slanje mailova - Marin Dujmović");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/logo.png"))).getImage());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        gbc.weightx = 1.0;  // Ensure equal resizing

        JLabel lblSavedEmails = new JLabel("Spremljeno:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panel.add(lblSavedEmails, gbc);

        cmbSavedEmails = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(cmbSavedEmails, gbc);

        JButton btnAddAccount = new JButton("Dodaj");
        gbc.gridx = 3;
        gbc.gridy = 0;
        panel.add(btnAddAccount, gbc);

        JButton btnRemoveAccount = new JButton("Ukloni");
        gbc.gridx = 4;
        gbc.gridy = 0;
        panel.add(btnRemoveAccount, gbc);

        JLabel lblEmail = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(lblEmail, gbc);

        txtEmail = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;  // Spanning across 3 columns
        panel.add(txtEmail, gbc);

        JLabel lblPassword = new JLabel("Lozinka:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        panel.add(txtPassword, gbc);

        JLabel lblSMTPServer = new JLabel("SMTP Posluzitelj:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(lblSMTPServer, gbc);

        cmbSMTPServer = new JComboBox<>(SMTP_SERVERS.keySet().toArray(new String[0]));
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(cmbSMTPServer, gbc);

        JLabel lblPort = new JLabel("Port:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(lblPort, gbc);

        txtPort = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        panel.add(txtPort, gbc);

        JLabel lblFilePath = new JLabel("Popis adresa:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(lblFilePath, gbc);

        txtFilePath = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(txtFilePath, gbc);

        JButton btnBrowse = new JButton("Trazilica");
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        panel.add(btnBrowse, gbc);

        JLabel lblSavedMessages = new JLabel("Spremljene Poruke:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(lblSavedMessages, gbc);

        cmbSavedMessages = new JComboBox<>();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(cmbSavedMessages, gbc);

        btnAddMessage = new JButton("Dodaj Poruku");
        gbc.gridx = 3;
        gbc.gridy = 6;
        panel.add(btnAddMessage, gbc);

        btnRemoveMessage = new JButton("Ukloni Poruku");
        gbc.gridx = 4;
        gbc.gridy = 6;
        panel.add(btnRemoveMessage, gbc);

        JLabel lblSubject = new JLabel("Subjekt:");
        gbc.gridx = 0;
        gbc.gridy = 7;
        panel.add(lblSubject, gbc);

        txtSubject = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        panel.add(txtSubject, gbc);

        JLabel lblMessage = new JLabel("Sadrzaj:");
        gbc.gridx = 0;
        gbc.gridy = 8;
        panel.add(lblMessage, gbc);

        txtMessage = new JTextPane();
        txtMessage.setContentType("text/plain");

        JScrollPane scrollPane = new JScrollPane(txtMessage);
        scrollPane.setPreferredSize(new Dimension(50, 50));

        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        panel.add(scrollPane, gbc);

        JButton btnSend = new JButton("Posalji");
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        panel.add(btnSend, gbc);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        gbc.gridx = 1;
        gbc.gridy = 10;
        gbc.gridwidth = 3;
        panel.add(progressBar, gbc);

        add(panel);

        cmbSMTPServer.addActionListener(e -> updateSMTPAndPort());

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
            String smtpServer = SMTP_SERVERS.get(cmbSMTPServer.getSelectedItem())[0];
            String port = txtPort.getText();
            String filePath = txtFilePath.getText();
            String subject = txtSubject.getText();
            String messageBody = txtMessage.getText();

            new Thread(() -> {
                try {
                    session = getOrCreateSession(email, password, smtpServer, port);
                    List<String> recipients = loadRecipientsFromFile(filePath);

                    // Set up progress bar
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setMaximum(recipients.size());
                        progressBar.setValue(0);
                    });

                    sendEmailsWithThrottling(session, email, recipients, subject, messageBody, 10000);

                    SwingUtilities.invokeLater(() -> {
                        if (!recipients.isEmpty()) {
                            txtSubject.setText("");
                            txtMessage.setText("");
                            showSentEmailsDialog(recipients);
                        }
                        progressBar.setValue(0);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();  // Log the exception (optional)
                    SwingUtilities.invokeLater(() -> showErrorDialog(ex));  // Show the error dialog and stop the program
                }
            }).start();
        });


        btnAddAccount.addActionListener(e -> saveEmailAccount());

        btnRemoveAccount.addActionListener(e -> removeEmailAccount());

        cmbSavedEmails.addActionListener(e -> loadSelectedEmailAccount());

        btnAddMessage.addActionListener(e -> addMessage());

        btnRemoveMessage.addActionListener(e -> removeMessage());

        cmbSavedMessages.addActionListener(e -> loadSelectedMessage());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            showErrorDialog(ex);
        }

        enableDragAndDropForAttachmentsAndImages();

        txtMessage.setEditorKit(new StyledEditorKit() {
            public void paste() {
                Transferable content = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (content != null) {
                    try {
                        String text = (String) content.getTransferData(DataFlavor.stringFlavor);
                        txtMessage.getDocument().insertString(txtMessage.getCaretPosition(), text, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveMessageContent();
            }
        });

        // Initial setup for SMTP server and port
        updateSMTPAndPort();
    }

    private void updateSMTPAndPort() {
        String selectedServer = (String) cmbSMTPServer.getSelectedItem();
        String[] serverDetails = SMTP_SERVERS.get(selectedServer);
        txtPort.setText(serverDetails[1]);
    }

    // Implement session reuse
    private Session getOrCreateSession(String email, String password, String smtpServer, String port) throws MessagingException {
        if (session == null) {
            session = authenticateWithSMTP(email, password, smtpServer, port);
        }
        return session;
    }

    // Implement exponential backoff for authentication
    private Session authenticateWithSMTP(String email, String password, String smtpServer, String port) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", port);

        int attempts = 0;
        int maxAttempts = 5;
        long waitTime = 1000; // Initial wait time in milliseconds

        while (attempts < maxAttempts) {
            try {
                attempts++;
                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(email, password);
                    }
                });

                // Test the session by connecting to the SMTP server
                Transport transport = session.getTransport("smtp");
                transport.connect(smtpServer, email, password);
                transport.close();

                return session; // Successful authentication, return session

            } catch (AuthenticationFailedException e) {
                System.err.println("Authentication failed: " + e.getMessage());
                if (attempts >= maxAttempts) {
                    // Show a pop-up and terminate the program
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Max authentication attempts reached. The program will now exit.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                    });
                }

                // Exponential backoff
                try {
                    System.out.println("Waiting for " + waitTime + "ms before next attempt...");
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                waitTime *= 2; // Double the wait time for the next attempt
            } catch (MessagingException e) {
                throw new MessagingException("Messaging exception occurred", e);
            }
        }

        throw new MessagingException("Failed to authenticate after " + maxAttempts + " attempts");
    }

    private List<String> loadRecipientsFromFile(String filePath) throws IOException {
        List<String> recipients = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                recipients.add(line.trim());
            }
        }
        return recipients;
    }

    private void sendEmailsWithThrottling(Session session, String fromEmail, List<String> recipients, String subject, String messageBody, int delayMs) {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (String recipient : recipients) {
            executor.submit(() -> {
                try {
                    sendEmail(session, fromEmail, recipient, subject, messageBody);
                    // Update progress bar after each email is sent or attempt is made
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
                    Thread.sleep(delayMs);
                } catch (MessagingException | InterruptedException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getValue() + 1));
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendEmail(Session session, String fromEmail, String recipient, String subject, String messageBody) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);

        MimeMultipart multipart = new MimeMultipart();

        addComplianceContent(multipart, messageBody);

        for (ImageIcon imageIcon : images) {
            MimeBodyPart imageBodyPart = new MimeBodyPart();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                ImageIO.write((BufferedImage) imageIcon.getImage(), "png", os);
                imageBodyPart.setContentID("<" + UUID.randomUUID() + ">");
                imageBodyPart.setDisposition(MimeBodyPart.INLINE);
                imageBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(os.toByteArray(), "image/png")));
                multipart.addBodyPart(imageBodyPart);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (File attachment : attachments) {
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            try {
                attachmentBodyPart.attachFile(attachment);
                multipart.addBodyPart(attachmentBodyPart);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        message.setContent(multipart);

        try {
            Transport.send(message);
        } catch (AuthenticationFailedException e) {
            // Show a pop-up and terminate the program
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null, "Max authentication attempts reached. The program will now exit.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            });
            return;
        }
    }

    private void addComplianceContent(MimeMultipart multipart, String messageBody) throws MessagingException {
        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(messageBody + "\n\nTo unsubscribe, click here.");
        multipart.addBodyPart(textBodyPart);
    }

    private void enableDragAndDropForAttachmentsAndImages() {
        txtMessage.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = evt.getTransferable();
                    List<File> droppedFiles = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        if (isImageFile(file)) {
                            BufferedImage image = ImageIO.read(file);
                            if (image != null) {
                                ImageIcon imageIcon = new ImageIcon(image);
                                images.add(imageIcon);
                                insertImageInTextPane(txtMessage, imageIcon);
                            }
                        } else {
                            attachments.add(file);
                        }
                    }
                    JOptionPane.showMessageDialog(null, "Datoteke dodane.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private boolean isImageFile(File file) {
        try {
            String mimetype = Files.probeContentType(file.toPath());
            return mimetype != null && mimetype.split("/")[0].equals("image");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void insertImageInTextPane(JTextPane textPane, ImageIcon imageIcon) {
        try {
            textPane.getDocument().insertString(textPane.getDocument().getLength(), "\n", null);
            textPane.insertIcon(imageIcon);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void saveEmailAccount() {
        String email = txtEmail.getText();
        String password = new String(txtPassword.getPassword());
        if (!email.isEmpty() && !password.isEmpty()) {
            preferences.put(email, password);
            if (((DefaultComboBoxModel<String>) cmbSavedEmails.getModel()).getIndexOf(email) == -1) {
                cmbSavedEmails.addItem(email);
            }
            JOptionPane.showMessageDialog(this, "Racun spremljen.", "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Email i lozinka ne mogu biti prazni.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeEmailAccount() {
        String selectedEmail = (String) cmbSavedEmails.getSelectedItem();
        if (selectedEmail != null) {
            preferences.remove(selectedEmail);
            cmbSavedEmails.removeItem(selectedEmail);
            txtEmail.setText("");
            txtPassword.setText("");
            JOptionPane.showMessageDialog(this, "Racun uklonjen.", "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Nema racuna za ukloniti.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedEmailAccount() {
        String selectedEmail = (String) cmbSavedEmails.getSelectedItem();
        if (selectedEmail != null) {
            txtEmail.setText(selectedEmail);
            txtPassword.setText(preferences.get(selectedEmail, ""));
        }
    }

    private void loadSavedEmails() {
        try {
            for (String key : preferences.keys()) {
                if (!key.equals("last_message")) {
                    cmbSavedEmails.addItem(key);
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void loadSavedMessages() {
        try {
            for (String key : preferences.keys()) {
                if (!key.equals("last_message") && !key.contains("@")) { // Avoid loading email keys
                    cmbSavedMessages.addItem(key);
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void loadSelectedMessage() {
        String selectedMessage = (String) cmbSavedMessages.getSelectedItem();
        if (selectedMessage != null) {
            txtMessage.setText(preferences.get(selectedMessage, ""));
            txtSubject.setText(selectedMessage);
        }
    }

    private void addMessage() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextField subjectField = new JTextField(20);
        JTextArea messageField = new JTextArea(10, 20);
        messageField.setLineWrap(true);
        messageField.setWrapStyleWord(true);
        panel.add(new JLabel("Subjekt poruke:"), BorderLayout.NORTH);
        panel.add(subjectField, BorderLayout.NORTH);
        panel.add(new JLabel("Sadrzaj poruke:"), BorderLayout.CENTER);
        panel.add(new JScrollPane(messageField), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "Dodaj Poruku", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newMessageName = subjectField.getText().trim();
            String newMessageBody = messageField.getText().trim();
            if (!newMessageName.isEmpty()) {
                preferences.put(newMessageName, newMessageBody);
                cmbSavedMessages.addItem(newMessageName);
                JOptionPane.showMessageDialog(this, "Poruka spremljena.", "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Naziv poruke ne može biti prazan.", "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeMessage() {
        String selectedMessage = (String) cmbSavedMessages.getSelectedItem();
        if (selectedMessage != null) {
            preferences.remove(selectedMessage);
            cmbSavedMessages.removeItem(selectedMessage);
            txtSubject.setText("");
            txtMessage.setText("");
            JOptionPane.showMessageDialog(this, "Poruka uklonjena.", "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Nema poruke za ukloniti.", "Greška", JOptionPane.ERROR_MESSAGE);
        }
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

    private void showErrorDialog(Throwable throwable) {
        // Create a modal dialog to display the error
        JDialog dialog = new JDialog(this, "Problem", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);

        // Create a text area to display the error message and stack trace
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        textArea.setText(sw.toString());

        // Add the text area to a scroll pane in the dialog
        dialog.add(new JScrollPane(textArea));

        // Add a button to close the dialog and terminate the program
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            dialog.dispose();
            System.exit(1);  // Exit the application with a status code of 1 (indicating an error)
        });

        // Add the button to the dialog
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Show the dialog
        dialog.setVisible(true);
    }


    private void saveMessageContent() {
        String lastSubject = txtSubject.getText();
        String lastMessage = txtMessage.getText();

        if (!lastSubject.isEmpty()) {
            preferences.put("last_message_subject", lastSubject);
            preferences.put("last_message_body", lastMessage);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Core().setVisible(true));
    }
}
