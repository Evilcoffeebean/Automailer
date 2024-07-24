# Automatic Mailing Bot

[![Java](https://img.shields.io/badge/Java-8-orange)](https://www.java.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## Description

The Automatic Mailing Bot is a Java-based application designed to automate the process of sending emails. Whether you need to send newsletters, notifications, or any other type of email in bulk, this bot provides a seamless and efficient solution. It's highly configurable, supports HTML and plain text emails, and can be easily integrated into existing systems.

## Features

- **Automated Email Sending**: Schedule and send emails automatically at specified intervals.
- **HTML and Plain Text Support**: Send both HTML and plain text emails.
- **SMTP Configuration**: Configure your SMTP server settings for secure and authenticated email sending.
- **Template Support**: Use predefined templates for consistent email formatting.
- **Logging**: Comprehensive logging to track email delivery status and errors.
- **Error Handling**: Robust error handling mechanisms to ensure reliability.
- **Extensible**: Easily extendable for additional features and customizations.

## Getting Started

### Prerequisites

- Java 8 or higher
- Maven

### Installation

1. **Clone the repository:**

    ```sh
    git clone https://github.com/Evilcoffeebean/Automailer.git
    cd automatic-mailing-bot
    ```

2. **Build the project:**

    ```sh
   mvn clean package
    ```
2. **Execute the program**

   `Automailer/target/-jar-with-dependencies.jar`

### Configuration

Before running the bot, you need to configure the SMTP settings and other parameters. Edit the `config.properties` file:

```properties
# SMTP Configuration
smtp.host=smtp.example.com
smtp.port=587
smtp.username=your-email@example.com
smtp.password=your-email-password

# Email Details
email.from=your-email@example.com
email.subject=Automatic Mailing Bot Subject
email.body=This is an automated email from the mailing bot.

# Schedule Configuration (in milliseconds)
schedule.interval=3600000  # e.g., 1 hour
