package dev.automailer.com;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.ArrayList;

public class Core extends JFrame {
    private final JTextField apiKeyField;
    private final JTextField fromEmailField;
    private final JTextField subjectField;
    private final JTextArea messageContentArea;
    private final JTextField recipientsFileField;
    private File recipientsFile;
    private final JProgressBar progressBar;
    private final List<File> attachedFiles = new ArrayList<>();

    private static final String CONFIG_FILE = "email_config.txt";

    public Core() {
        setTitle("E-Mail Robot za Boostiro.com");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel apiKeyLabel = new JLabel("Ključ:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(apiKeyLabel, gbc);

        apiKeyField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridy = 0;
        add(apiKeyField, gbc);

        JLabel fromEmailLabel = new JLabel("E-Mail:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(fromEmailLabel, gbc);

        fromEmailField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(fromEmailField, gbc);

        JLabel recipientsLabel = new JLabel("Primatelji:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(recipientsLabel, gbc);

        recipientsFileField = new JTextField(20);
        recipientsFileField.setEditable(false);
        gbc.gridx = 1;
        gbc.gridy = 2;
        add(recipientsFileField, gbc);

        JButton browseButton = new JButton("Tražilica");
        gbc.gridx = 2;
        gbc.gridy = 2;
        add(browseButton, gbc);

        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(Core.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                recipientsFile = fileChooser.getSelectedFile();
                recipientsFileField.setText(recipientsFile.getAbsolutePath());
            }
        });

        JLabel subjectLabel = new JLabel("Subjekt:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(subjectLabel, gbc);

        subjectField = new JTextField(30);
        gbc.gridx = 1;
        gbc.gridy = 3;
        add(subjectField, gbc);

        JLabel messageLabel = new JLabel("Sadržaj:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.NORTH;
        add(messageLabel, gbc);

        messageContentArea = new JTextArea(10, 30);
        messageContentArea.setTransferHandler(new FileDropHandler());
        messageContentArea.setDragEnabled(true);
        messageContentArea.setLineWrap(true);
        messageContentArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JScrollPane(messageContentArea), gbc);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        add(progressBar, gbc);

        JButton sendButton = new JButton("Pošalji:");
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(sendButton, gbc);

        sendButton.addActionListener(e -> new Thread(this::sendEmails).start());
        loadConfig();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveConfig();
            }
        });

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        setLocationRelativeTo(null);
    }

    private void sendEmails() {
        String apiKey = apiKeyField.getText();
        String fromEmail = fromEmailField.getText();
        String subject = subjectField.getText();
        String messageContent = messageContentArea.getText();

        if (recipientsFile == null || !recipientsFile.exists()) {
            showError("Nije pronađena lista primatelja.", null);
            return;
        }

        try {
            List<String> recipients = Files.readAllLines(Paths.get(recipientsFile.getAbsolutePath()));
            progressBar.setMaximum(recipients.size());

            Properties properties = System.getProperties();
            properties.put("mail.smtp.host", "smtp.sendgrid.net");
            properties.put("mail.smtp.port", "587");
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication("apikey", apiKey);
                        }
                    });

            int sentCount = 0;
            for (String recipient : recipients) {
                try {
                    MimeMessage message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(fromEmail));
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
                    message.setSubject(subject);

                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(messageContent);

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);

                    for (File file : attachedFiles) {
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        attachmentPart.attachFile(file);
                        multipart.addBodyPart(attachmentPart);
                    }

                    message.setContent(multipart);

                    Transport.send(message);

                    sentCount++;
                    int progress = (int) ((double) sentCount / recipients.size() * 100);
                    SwingUtilities.invokeLater(() -> progressBar.setValue(progress));

                } catch (MessagingException | IOException mex) {
                    showError("Problem sa slanjem maila na " + recipient, mex);
                }
            }

            JOptionPane.showMessageDialog(this, "Uspješno poslani mailovi.");

        } catch (IOException ex) {
            showError("Neuspjelo učitavanje popisa primatelja.", ex);
        }
    }

    private void showError(String message, Exception ex) {
        JFrame errorFrame = new JFrame("Problem");
        errorFrame.setSize(400, 300);
        errorFrame.setLayout(new BorderLayout());

        JTextArea errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setText(message);
        if (ex != null) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            errorArea.append("\n\n" + sw);
        }
        errorFrame.add(new JScrollPane(errorArea), BorderLayout.CENTER);

        JButton closeButton = new JButton("Zatvori");
        closeButton.addActionListener(e -> errorFrame.dispose());
        errorFrame.add(closeButton, BorderLayout.SOUTH);

        errorFrame.setLocationRelativeTo(this);
        errorFrame.setVisible(true);
    }

    private void loadConfig() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
            apiKeyField.setText(reader.readLine());
            fromEmailField.setText(reader.readLine());
            recipientsFileField.setText(reader.readLine());
            subjectField.setText(reader.readLine());
            messageContentArea.setText(reader.readLine());
        } catch (IOException e) {
            System.out.println("Nije pronađena konfiguracijska datoteka. Krećem iz početka.");
        }
    }

    private void saveConfig() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(apiKeyField.getText() + "\n");
            writer.write(fromEmailField.getText() + "\n");
            writer.write(recipientsFileField.getText() + "\n");
            writer.write(subjectField.getText() + "\n");
            writer.write(messageContentArea.getText() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class FileDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }
            boolean copySupported = (COPY & support.getSourceDropActions()) == COPY;
            if (!copySupported) {
                return false;
            }
            support.setDropAction(COPY);
            return true;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            try {
                Transferable transferable = support.getTransferable();
                List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : droppedFiles) {
                    if (file.isFile()) {
                        attachedFiles.add(file);
                        messageContentArea.append("\nDodano: " + file.getName());
                    }
                }
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Core gui = new Core();
            gui.setVisible(true);
        });
    }
}