/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.smtp.AuthenticatingSMTPClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.smtp.SimpleSMTPHeader;

import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.CSVExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.DBExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.SummaryExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailConfig;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailSecurity;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.IoUtil;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Sends a mail to a recipient or recipients, including (or not) some file attachments with exports of the log.
 */
public class ReportEmailer {

    private static final String TAG = Constants.TAG + ReportEmailer.class.getSimpleName();
    private static final String ENCODING = "UTF-8";
    private final Context mContext;

    public ReportEmailer(Context context) {
        Log.v(TAG, "Constructor");
        mContext = context.getApplicationContext();
    }

    /**
     * Send the e-mail report.
     */
    public synchronized void send() {
        Log.v(TAG, "send");
        if (shouldSendMail()) {
            final EmailConfig emailConfig = EmailPreferences.getInstance(mContext).getEmailConfig();
            if (emailConfig.isValid()) {
                sendEmail(emailConfig);
            } else {
                Log.w(TAG, "Cannot send mail with the current email settings: " + emailConfig);
            }
        } else {
            Log.v(TAG, "Won't send mail");
        }
    }

    /**
     * @return true if it has been longer than our report interval since we last successfully sent a mail.
     */
    private boolean shouldSendMail() {
        Log.v(TAG, "shouldSendMail");
        int reportInterval = EmailPreferences.getInstance(mContext).getEmailReportInterval();
        if (reportInterval == 0) {
            Log.v(TAG, "shouldSendMail: mails not enabled");
            return false;
        }
        long lastEmailSent = EmailPreferences.getInstance(mContext).getLastEmailSent();
        long now = System.currentTimeMillis();
        Log.v(TAG, "shouldSendMail: sent mail " + (now - lastEmailSent) + " ms ago, vs report duration = " + reportInterval + " ms");
        return now - lastEmailSent > reportInterval;
    }

    /**
     * Send the e-mail with the given e-mail settings.
     */
    private void sendEmail(final EmailConfig emailConfig) {
        Log.v(TAG, "sendEmail: emailConfig = " + emailConfig);
        // Prepare the file attachments before we start to send the e-mail.
        Set<File> attachments = new HashSet<>();
        for (String fileType : emailConfig.reportFormats) {
            attachments.add(createAttachment(fileType));
        }

        // Set up the mail connectivity
        AuthenticatingSMTPClient client = null;
        try {
            if(emailConfig.security == EmailSecurity.TLS)
                client = new AuthenticatingSMTPClient("TLS");
            else if(emailConfig.security == EmailSecurity.SSL)
                client = new AuthenticatingSMTPClient("SSL");
            else
                client = new AuthenticatingSMTPClient();
            if(BuildConfig.DEBUG) {
                client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
            }
            client.setDefaultTimeout(15000);
            client.connect(emailConfig.server, emailConfig.port);
            checkReply(client);
            client.helo("[" + client.getLocalAddress().getHostAddress()+"]");
            checkReply(client);
            if(emailConfig.security == EmailSecurity.TLS) {
                if(!client.execTLS()) {
                    checkReply(client);
                    throw new RuntimeException("Could not start tls");
                }
            }
            client.auth(AuthenticatingSMTPClient.AUTH_METHOD.LOGIN, emailConfig.user, emailConfig.password);
            checkReply(client);

            // Set up the mail participants
            String from = getFromAddress(emailConfig);
            client.setSender(from);
            checkReply(client);
            String[] recipients = TextUtils.split(emailConfig.recipients, "[,; ]+");
            for(String recipient : recipients) {
                client.addRecipient(recipient);
                checkReply(client);
            }

            // Set up the mail content
            client.setCharset(Charset.forName(ENCODING));
            checkReply(client);
            Writer writer = client.sendMessageData();
            String subject = mContext.getString(R.string.export_subject_send_log);
            SimpleSMTPHeader header = new SimpleSMTPHeader(from, recipients[0], subject);
            for(int i=1; i < recipients.length; i++)
                header.addCC(recipients[i]);
            String messageText = getMessageBody(emailConfig);
            // Just plain text mail: no attachments
            if(emailConfig.reportFormats.isEmpty()) {
                // Weird bug: have to add the first header twice
                header.addHeaderField("Content-Type", "text/plain; charset=" + ENCODING);
                header.addHeaderField("Content-Type", "text/plain; charset=" + ENCODING);
                writer.write(header.toString());
                writer.write(messageText);
            } else {
                String boundary = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 28);
                // Weird bug: have to add the first header twice
                header.addHeaderField("Content-Type", "multipart/mixed; boundary=" + boundary);
                header.addHeaderField("Content-Type", "multipart/mixed; boundary=" + boundary);
                writer.write(header.toString());

                // Write the main text message
                writer.write("--" + boundary + "\n");
                writer.write("Content-Type: text/plain; charset=" + ENCODING + "\n\n");
                writer.write(messageText);
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
            Log.v(TAG, "sent message");
            // The mail was sent file.  Dismiss the e-mail error notification if it's showing.
            NetMonNotification.dismissEmailFailureNotification(mContext);
            EmailPreferences.getInstance(mContext).setLastEmailSent(System.currentTimeMillis());
        } catch (Exception e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
            // There was an error sending the mail. Show an error notification.
            NetMonNotification.showEmailFailureNotification(mContext);
        }

    }

    /**
     * Append the given attachments to the message which is being written by the given writer.
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
    private static void checkReply(SMTPClient sc) throws Exception {
        if (SMTPReply.isNegativeTransient(sc.getReplyCode())) {
            throw new Exception("Transient SMTP error " + sc.getReply() + sc.getReplyString());
        } else if (SMTPReply.isNegativePermanent(sc.getReplyCode())) {
            throw new Exception("Permanent SMTP error " + sc.getReply() + sc.getReplyString());
        }
    }
    /**
     * Construct the from address based on the user name and possibly the smtp server name.
     */
    private static String getFromAddress(EmailConfig emailConfig) {
        Log.v(TAG, "getFromAddress: emailConfig = " + emailConfig);
        // We try to guess the from address.
        final String from;
        // If the user name for the smtp server is an e-mail address, we just use that.
        if (emailConfig.user.indexOf("@") > 0) {
            from = emailConfig.user;
        }
        // Otherwise we use the user@server.  We try to strip any "smtp" part of the server domain.
        else {
            String server = emailConfig.server.replaceAll("smtp[^\\.]*\\.", "");
            from = emailConfig.user + "@" + server;
        }
        Log.v(TAG, "getFromAddress: Sending mail from " + from);
        return from;
    }

    /**
     * @param fileType one of the file types the user selected in the preference
     * @return the BodyPart containing the exported file of this type.
     */
    private File createAttachment(String fileType) {
        Log.v(TAG, "createAttachment: fileType = " + fileType);
        // Get the FileExport instance which can export mContext file type.
        final FileExport fileExport;
        switch(fileType) {
            case "csv":
                fileExport = new CSVExport(mContext);
                break;
            case "html":
                fileExport = new HTMLExport(mContext, true);
                break;
            case "excel":
                fileExport = new ExcelExport(mContext);
                break;
            case "kml":
                fileExport = new KMLExport(mContext, NetMonColumns.SOCKET_CONNECTION_TEST);
                break;
            case "db":
            default:
                fileExport = new DBExport(mContext);
                break;
        }
        return fileExport.execute(null);
    }

    /**
     * @return the text of the mail that the recipient will receive.
     */
    private String getMessageBody(EmailConfig emailConfig) {
        Log.v(TAG, "getMessageBody, emailConfig =" + emailConfig);
        String reportSummary = SummaryExport.getSummary(mContext);
        String dateRange = SummaryExport.getDataCollectionDateRange(mContext);
        String messageBody = mContext.getString(R.string.export_message_text, dateRange);
        // If we're attaching files, add a sentence to the mail saying so.
        if (!emailConfig.reportFormats.isEmpty()) messageBody += mContext.getString(R.string.export_message_text_file_attached);
        messageBody += reportSummary;
        Log.v(TAG, "getMessageBody, created message body " + messageBody);
        return messageBody;
    }

}
