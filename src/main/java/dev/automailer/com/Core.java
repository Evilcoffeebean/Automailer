package dev.automailer.com;

import javax.activation.DataHandler;
import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import javax.swing.*;
import javax.swing.text.BadLocationException;
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

    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbSavedEmails;
    private JTextField txtFilePath;
    private JTextField txtSubject;
    private JTextPane txtMessage;
    private JProgressBar progressBar;
    private JComboBox<String> cmbEmailProvider;
    private Session session;

    private static final Map<String, String[]> SMTP_SETTINGS = new HashMap<>();
    private static final List<File> attachments = new ArrayList<>();
    private static final List<ImageIcon> images = new ArrayList<>();
    private static final String PREF_MESSAGE_SUBJECT = "message_subject";
    private static final String PREF_MESSAGE_BODY = "message_body";
    private final Preferences preferences;

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
        preferences = Preferences.userRoot().node(this.getClass().getName());
        createUI();
        loadMessageContent();
    }

    private void createUI() {
        setTitle("Automatsko slanje mailova - Marin Dujmović");
        setSize(600, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/logo.png"))).getImage());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel lblEmailProvider = new JLabel("Posluzitelj:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(lblEmailProvider, gbc);

        cmbEmailProvider = new JComboBox<>(SMTP_SETTINGS.keySet().toArray(new String[0]));
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(cmbEmailProvider, gbc);

        JLabel lblSavedEmails = new JLabel("Spremljeni Racuni:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(lblSavedEmails, gbc);

        cmbSavedEmails = new JComboBox<>();
        loadSavedEmails();
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(cmbSavedEmails, gbc);

        JButton btnAddAccount = new JButton("Dodaj racun");
        gbc.gridx = 2;
        gbc.gridy = 1;
        panel.add(btnAddAccount, gbc);

        JButton btnRemoveAccount = new JButton("Ukloni racun");
        gbc.gridx = 3;
        gbc.gridy = 1;
        panel.add(btnRemoveAccount, gbc);

        JLabel lblEmail = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(lblEmail, gbc);

        txtEmail = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(txtEmail, gbc);

        JLabel lblPassword = new JLabel("Lozinka:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(lblPassword, gbc);

        txtPassword = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(txtPassword, gbc);

        JLabel lblFilePath = new JLabel("Popis adresa:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(lblFilePath, gbc);

        txtFilePath = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        panel.add(txtFilePath, gbc);

        JButton btnBrowse = new JButton("Trazilica");
        gbc.gridx = 2;
        gbc.gridy = 4;
        panel.add(btnBrowse, gbc);

        JLabel lblSubject = new JLabel("Subjekt:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(lblSubject, gbc);

        txtSubject = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 5;
        panel.add(txtSubject, gbc);

        JLabel lblMessage = new JLabel("Sadrzaj:");
        gbc.gridx = 0;
        gbc.gridy = 6;
        panel.add(lblMessage, gbc);

        txtMessage = new JTextPane();
        txtMessage.setContentType("text/html");

        JScrollPane scrollPane = new JScrollPane(txtMessage);
        scrollPane.setPreferredSize(new Dimension(50, 50)); // Set the preferred width and height

        gbc.gridx = 0; // Starting at column 0
        gbc.gridy = 7; // Starting at row 6
        gbc.gridwidth = 4; // Span across 4 columns
        gbc.gridheight = 2; // Span across 2 rows
        gbc.fill = GridBagConstraints.BOTH; // Fill the available space both horizontally and vertically
        gbc.weightx = 1.0; // Allow the text area to expand horizontally
        gbc.weighty = 1.0; // Allow the text area to expand vertically

        panel.add(scrollPane, gbc);

        JButton btnSend = new JButton("Posalji mail");
        gbc.gridy = 9;
        panel.add(btnSend, gbc);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 4;
        panel.add(progressBar, gbc);

        add(panel);

        // Load email and password when a saved email is selected
        cmbSavedEmails.addActionListener(e -> loadEmailAndPassword());

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

        btnAddAccount.addActionListener(e -> showAddAccountDialog());

        btnRemoveAccount.addActionListener(e -> removeSelectedAccount());

        // Set a modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            showErrorDialog(ex);
        }

        // Enable drag and drop for attachments and images
        enableDragAndDropForAttachmentsAndImages();

        // Save the message content and subject when closing the application
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveMessageContent();
            }
        });
    }

    private void showAddAccountDialog() {
        JDialog dialog = new JDialog(this, "Dodaj novi racun", true);
        dialog.setSize(400, 200);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel lblEmail = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(lblEmail, gbc);

        JTextField txtNewEmail = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        dialog.add(txtNewEmail, gbc);

        JLabel lblPassword = new JLabel("App Password:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        dialog.add(lblPassword, gbc);

        JPasswordField txtNewPassword = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        dialog.add(txtNewPassword, gbc);

        JButton btnSave = new JButton("Spremi");
        gbc.gridx = 1;
        gbc.gridy = 2;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            String email = txtNewEmail.getText();
            String password = new String(txtNewPassword.getPassword());
            if (!email.isEmpty() && !password.isEmpty()) {
                saveEmailAndPassword(email, password);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Email i lozinka ne smiju biti prazni.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveEmailAndPassword(String email, String password) {
        preferences.put(email, password);
        cmbSavedEmails.addItem(email); // Add to the dropdown
    }

    private void loadEmailAndPassword() {
        String email = (String) cmbSavedEmails.getSelectedItem();
        if (email != null) {
            txtEmail.setText(email);
            txtPassword.setText(preferences.get(email, ""));
        }
    }

    private void loadSavedEmails() {
        try {
            for (String key : preferences.keys()) {
                cmbSavedEmails.addItem(key);
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void removeSelectedAccount() {
        String selectedEmail = (String) cmbSavedEmails.getSelectedItem();
        if (selectedEmail != null) {
            // Remove from preferences
            preferences.remove(selectedEmail);

            // Remove from combo box
            cmbSavedEmails.removeItem(selectedEmail);

            // Clear email and password fields
            txtEmail.setText("");
            txtPassword.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Nije odabran racun za brisanje.", "Error", JOptionPane.ERROR_MESSAGE);
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
                    e.printStackTrace();
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

        // Create a multipart message for attachment and images
        Multipart multipart = new MimeMultipart();

        // Add text part
        MimeBodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(messageBody);
        multipart.addBodyPart(textBodyPart);

        // Add image parts
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

        // Add attachments
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

        Transport.send(message);
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

    private void saveMessageContent() {
        // Save the message subject and body to preferences
        preferences.put(PREF_MESSAGE_SUBJECT, txtSubject.getText());
        preferences.put(PREF_MESSAGE_BODY, txtMessage.getText());
    }

    private void loadMessageContent() {
        // Load the message subject and body from preferences
        txtSubject.setText(preferences.get(PREF_MESSAGE_SUBJECT, ""));
        txtMessage.setText(preferences.get(PREF_MESSAGE_BODY, ""));
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
