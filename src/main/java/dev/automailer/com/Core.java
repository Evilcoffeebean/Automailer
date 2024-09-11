package dev.automailer.com;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Core extends JFrame {
    private final JTextField apiKeyField;
    private final JTextField fromEmailField;
    private final JTextField subjectField;
    private final JTextArea messageContentArea;
    private final JTextField recipientsFileField;
    private File recipientsFile;
    private final JProgressBar progressBar;
    private final List<File> attachedFiles = new ArrayList<>();
    private final JComboBox<String> templateDropdown;
    private final Map<String, EmailTemplate> templates = new HashMap<>();

    private static final String CONFIG_FILE = "email_config.txt";
    private static final String TEMPLATES_FILE = "email_templates.txt";

    public Core() {
        setTitle("E-Mail Robot za Boostiro.com");
        setSize(800, 500);
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
        messageContentArea.setLineWrap(true);
        messageContentArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JScrollPane(messageContentArea), gbc);

        JLabel templateLabel = new JLabel("Predlošci:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        add(templateLabel, gbc);

        templateDropdown = new JComboBox<>();
        templateDropdown.addActionListener(e -> loadSelectedTemplate());
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        add(templateDropdown, gbc);

        JButton manageTemplatesButton = new JButton("Uredi predloške");
        manageTemplatesButton.addActionListener(e -> manageTemplates());
        gbc.gridx = 3;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        add(manageTemplatesButton, gbc);


        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        add(progressBar, gbc);

        JButton sendButton = new JButton("Pošalji:");
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(sendButton, gbc);

        sendButton.addActionListener(e -> new Thread(this::sendEmails).start());
        loadConfig();
        loadTemplates();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveConfig();
                saveTemplates();
            }
        });

        messageContentArea.setTransferHandler(new PlainTextTransferHandler(messageContentArea));

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        setLocationRelativeTo(null);
    }

    private void loadSelectedTemplate() {
        String selectedTemplateName = (String) templateDropdown.getSelectedItem();
        if (selectedTemplateName != null) {
            EmailTemplate template = templates.get(selectedTemplateName);
            if (template != null) {
                subjectField.setText(template.getSubject());
                messageContentArea.setText(template.getBody());
            }
        }
    }

    private void manageTemplates() {
        TemplateManagerDialog dialog = new TemplateManagerDialog(this, templates);
        dialog.setVisible(true);
        updateTemplateDropdown();
    }

    void updateTemplateDropdown() {
        templateDropdown.removeAllItems();
        for (String templateName : templates.keySet()) {
            templateDropdown.addItem(templateName);
        }
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

    private void loadTemplates() {
        try (BufferedReader reader = new BufferedReader(new FileReader(TEMPLATES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    String name = parts[0];
                    String content = parts[1];
                    String[] contentParts = content.split("\\|", 2);
                    if (contentParts.length == 2) {
                        String subject = contentParts[0];
                        String body = contentParts[1];
                        templates.put(name, new EmailTemplate(subject, body));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No template file found, starting fresh.");
        }
        updateTemplateDropdown();
    }

    private void saveTemplates() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEMPLATES_FILE))) {
            for (Map.Entry<String, EmailTemplate> entry : templates.entrySet()) {
                String name = entry.getKey();
                EmailTemplate template = entry.getValue();
                writer.write(name + "|" + template.getSubject() + "|" + template.getBody() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Core gui = new Core();
            gui.setVisible(true);
        });
    }
}

class EmailTemplate {
    private String subject;
    private String body;

    public EmailTemplate(String subject, String body) {
        this.subject = subject;
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }
}

class TemplateManagerDialog extends JDialog {
    private final Map<String, EmailTemplate> templates;
    private final JList<String> templateList;
    private final JTextField subjectField;
    private final JTextArea bodyArea;

    public TemplateManagerDialog(JFrame parent, Map<String, EmailTemplate> templates) {
        super(parent, "Uredi Predloške", true);
        this.templates = templates;

        setLayout(new GridBagLayout());
        setSize(600, 400);
        setLocationRelativeTo(parent);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);  // Padding between elements
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        templateList = new JList<>(templates.keySet().toArray(new String[0]));
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateList.setFixedCellWidth(150);
        templateList.addListSelectionListener(e -> loadSelectedTemplate());

        JScrollPane templateScrollPane = new JScrollPane(templateList);
        templateScrollPane.setPreferredSize(new Dimension(150, 300));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.3;
        add(templateScrollPane, gbc);

        JPanel editPanel = new JPanel(new GridBagLayout());

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel subjectLabel = new JLabel("Subjekt:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        editPanel.add(subjectLabel, gbc);

        subjectField = new JTextField();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        editPanel.add(subjectField, gbc);

        JLabel bodyLabel = new JLabel("Sadržaj poruke:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        editPanel.add(bodyLabel, gbc);

        bodyArea = new JTextArea(10, 30);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);

        JScrollPane bodyScrollPane = new JScrollPane(bodyArea);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        editPanel.add(bodyScrollPane, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.7;
        add(editPanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton saveButton = new JButton("Spremi");
        saveButton.addActionListener(e -> saveTemplate());
        buttonPanel.add(saveButton);

        JButton deleteButton = new JButton("Ukloni");
        deleteButton.addActionListener(e -> deleteTemplate());
        buttonPanel.add(deleteButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);

        setMinimumSize(new Dimension(600, 400));
    }


    private void loadSelectedTemplate() {
        String selectedTemplateName = templateList.getSelectedValue();
        if (selectedTemplateName != null) {
            EmailTemplate template = templates.get(selectedTemplateName);
            if (template != null) {
                subjectField.setText(template.getSubject());
                bodyArea.setText(template.getBody());
            }
        }
    }

    private void saveTemplate() {
        String selectedTemplateName = templateList.getSelectedValue();
        String subject = subjectField.getText();
        String body = bodyArea.getText();

        if (subject.isEmpty() || body.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subject and body cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedTemplateName != null) {
            EmailTemplate template = templates.get(selectedTemplateName);
            if (template != null) {
                template.setSubject(subject);
                template.setBody(body);
            }
        } else {
            String newTemplateName = JOptionPane.showInputDialog(this, "Enter a name for the new template:", "New Template", JOptionPane.PLAIN_MESSAGE);
            if (newTemplateName != null && !newTemplateName.trim().isEmpty()) {
                EmailTemplate newTemplate = new EmailTemplate(subject, body);
                templates.put(newTemplateName, newTemplate);
            }
        }

        updateTemplateList();
        ((Core) getParent()).updateTemplateDropdown();
    }


    private void deleteTemplate() {
        String selectedTemplateName = templateList.getSelectedValue();
        if (selectedTemplateName != null) {
            templates.remove(selectedTemplateName);
            updateTemplateList();
        }
    }

    private void updateTemplateList() {
        templateList.setListData(templates.keySet().toArray(new String[0]));
    }
}

class PlainTextTransferHandler extends TransferHandler {
    private final JTextArea textArea;

    public PlainTextTransferHandler(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        try {
            String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            textArea.append(data);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clipboard, int action) throws IllegalStateException {
        StringSelection selection = new StringSelection(textArea.getSelectedText());
        clipboard.setContents(selection, null);
    }
}

