/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Carmen Alvarez (c@rmen.ca)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.rmen.android.networkmonitor.app.email;

import android.util.Base64;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SimpleSMTPHeader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.util.IoUtil;

/**
 * Sends a mail to a recipient or recipients, including (or not) some file attachments.
 * This class attempts to have the fewest dependencies possible on Android or this project.
 */
class Emailer {

    private static final String ENCODING = "UTF-8";
    private static final int SMTP_TIMEOUT_MS = 15000;

    /**
     * Sends an e-mail in UTF-8 encoding.
     *
     * @param protocol    this has been tested with "TLS", "SSL", and null.
     * @param attachments optional attachments to include in the mail.
     */
    static void sendEmail(String protocol, String server, int port,
                          String user, String password,
                          String from, String[] recipients,
                          String subject,
                          String body, Set<File> attachments) throws Exception {

        // Set up the mail connectivity
        final AuthenticatingSMTPClient client;
        if (protocol == null) client = new AuthenticatingSMTPClient();
        else client = new AuthenticatingSMTPClient(protocol);

        if (BuildConfig.DEBUG) client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));

        client.setDefaultTimeout(SMTP_TIMEOUT_MS);
        client.setCharset(Charset.forName(ENCODING));
        client.connect(server, port);
        checkReply(client);
        client.helo("[" + client.getLocalAddress().getHostAddress() + "]");
        checkReply(client);
        if ("TLS".equals(protocol)) {
            if (!client.execTLS()) {
                checkReply(client);
                throw new RuntimeException("Could not start tls");
            }
        }
        client.auth(AuthenticatingSMTPClient.AUTH_METHOD.LOGIN, user, password);
        checkReply(client);

        // Set up the mail participants
        client.setSender(from);
        checkReply(client);
        for (String recipient : recipients) {
            client.addRecipient(recipient);
            checkReply(client);
        }

        // Set up the mail content
        Writer writer = client.sendMessageData();
        SimpleSMTPHeader header = new SimpleSMTPHeader(from, recipients[0], subject);
        for (int i = 1; i < recipients.length; i++)
            header.addCC(recipients[i]);

        // Just plain text mail: no attachments
        if (attachments == null || attachments.isEmpty()) {
            header.addHeaderField("Content-Type", "text/plain; charset=" + ENCODING);
            writer.write(header.toString());
            writer.write(body);
        }
        // Mail with attachments
        else {
            String boundary = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 28);
            header.addHeaderField("Content-Type", "multipart/mixed; boundary=" + boundary);
            writer.write(header.toString());

            // Write the main text message
            writer.write("--" + boundary + "\n");
            writer.write("Content-Type: text/plain; charset=" + ENCODING + "\n\n");
            writer.write(body);
            writer.write("\n");

            // Write the attachments
            appendAttachments(writer, boundary, attachments);
            writer.write("--" + boundary + "--\n\n");
        }

        writer.close();
        if (!client.completePendingCommand()) {
            throw new RuntimeException("Could not send mail");
        }
        client.logout();
        client.disconnect();
    }

    /**
     * Append the given attachments to the message which is being written by the given writer.
     *
     * @param boundary separates each file attachment
     */
    private static void appendAttachments(Writer writer, String boundary, Collection<File> attachments) throws IOException {
        for (File attachment : attachments) {
            ByteArrayOutputStream fileOs = new ByteArrayOutputStream((int) attachment.length());
            FileInputStream fileIs = new FileInputStream(attachment);
            try {
                IoUtil.copy(fileIs, fileOs);
            } finally {
                IoUtil.closeSilently(fileIs, fileOs);
            }
            final String mimeType = attachment.getName().substring(attachment.getName().indexOf(".") + 1);
            writer.write("--" + boundary + "\n");
            writer.write("Content-Type: application/" + mimeType + "; name=\"" + attachment.getName() + "\"\n");
            writer.write("Content-Disposition: attachment; filename=\"" + attachment.getName() + "\"\n");
            writer.write("Content-Transfer-Encoding: base64\n\n");
            String encodedFile = Base64.encodeToString(fileOs.toByteArray(), Base64.DEFAULT);
            writer.write(encodedFile);
            writer.write("\n");
        }
    }

    // http://blog.dahanne.net/2013/06/17/sending-a-mail-in-java-and-android-with-apache-commons-net/
    private static void checkReply(SMTPClient sc) throws IOException {
        if (SMTPReply.isNegativeTransient(sc.getReplyCode())) {
            throw new RuntimeException("Transient SMTP error " + sc.getReply() + sc.getReplyString());
        } else if (SMTPReply.isNegativePermanent(sc.getReplyCode())) {
            throw new RuntimeException("Permanent SMTP error " + sc.getReply() + sc.getReplyString());
        }
    }

}
