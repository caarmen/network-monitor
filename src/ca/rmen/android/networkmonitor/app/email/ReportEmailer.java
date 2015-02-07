/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org) //TODO <- replace with *your* name/email
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import android.content.Context;

import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.db.export.CSVExport;
import ca.rmen.android.networkmonitor.app.db.export.DBExport;
import ca.rmen.android.networkmonitor.app.db.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.db.export.FileExport;
import ca.rmen.android.networkmonitor.app.db.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.db.export.SummaryExport;
import ca.rmen.android.networkmonitor.app.db.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailConfig;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailSecurity;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
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
                Session mailSession = getMailSession(emailConfig);
                Message message = createMessage(mailSession, emailConfig);
                if (message != null) sendMail(mailSession, message);
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
     * @return an SMTP Session that we can open to send a message on.
     */
    private static Session getMailSession(final EmailConfig emailConfig) {
        Log.v(TAG, "getMailSession: emailConfig = " + emailConfig);
        // Set up properties for mail sending.
        Properties props = new Properties();
        String propertyPrefix, transportProtocol;
        if (emailConfig.security == EmailSecurity.SSL) {
            propertyPrefix = "mail.smtps.";
            transportProtocol = "smtps";
        } else {
            propertyPrefix = "mail.smtp.";
            transportProtocol = "smtp";
        }
        props.put(propertyPrefix + "host", emailConfig.server);
        props.put(propertyPrefix + "port", String.valueOf(emailConfig.port));
        props.put(propertyPrefix + "auth", "true");
        props.put(propertyPrefix + "timeout", "15000");
        props.put(propertyPrefix + "connectiontimeout", "15000");
        props.put(propertyPrefix + "writetimeout", "15000");
        if (emailConfig.security == EmailSecurity.TLS) props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", String.valueOf(BuildConfig.DEBUG));
        props.put("mail.transport.protocol", transportProtocol);

        // Create the session with the properties.
        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailConfig.user, emailConfig.password);
            }
        });
        Log.v(TAG, "getMailSession, created session " + mailSession);
        return mailSession;
    }

    /**
     * Actually send the e-mail.
     */
    private void sendMail(Session mailSession, Message message) {
        Log.v(TAG, "sendMail");
        Transport transport = null;
        try {
            transport = mailSession.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            Log.v(TAG, "sent message");
            // The mail was sent file.  Dismiss the e-mail error notification if it's showing.
            NetMonNotification.dismissEmailFailureNotification(mContext);
            EmailPreferences.getInstance(mContext).setLastEmailSent(System.currentTimeMillis());
        } catch (MessagingException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
            // There was an error sending the mail. Show an error notification.
            NetMonNotification.showEmailFailureNotification(mContext);
        } finally {
            try {
                if (transport != null) transport.close();
            } catch (MessagingException e) {
                Log.e(TAG, "onHandleIntent Could not close the transport", e);
            }
        }
    }


    /**
     * @return a Message we can send using the given mailSession, or null if there was a problem creating the message.
     */
    private Message createMessage(Session mailSession, EmailConfig emailConfig) {
        Log.v(TAG, "createMessage: emailConfig = " + emailConfig);

        try {
            MimeMessage message = new MimeMessage(mailSession);
            // Set the subject, from, and to fields.
            String subject = mContext.getString(R.string.export_subject_send_log);
            message.setSubject(MimeUtility.encodeText(subject, ENCODING, "Q"));
            String from = getFromAddress(emailConfig);
            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emailConfig.recipients));

            // Get the plain text of the mail body.
            String messageText = getMessageBody(emailConfig);

            // Case 1: No file attachment, just send a plain text mail with the summary.
            if (emailConfig.reportFormats.isEmpty()) {
                message.setHeader("Content-Transfer-Encoding", "quoted-printable");
                message.setText(messageText, ENCODING);
            }
            // Case 2: We have file attachments, so we need to do a multi-part mail.
            else {
                // Add the plain text version of the mail
                Multipart mp = new MimeMultipart("mixed");
                BodyPart bp = new MimeBodyPart();
                bp.setContent(messageText, "text/plain;charset=" + ENCODING);
                bp.setHeader("Content-Transfer-Encoding", "quoted-printable");
                mp.addBodyPart(bp);

                // Now add the file attachments.
                for (String fileType : emailConfig.reportFormats) {
                    bp = createBodyPart(fileType);
                    mp.addBodyPart(bp);
                }
                message.setContent(mp);
            }
            Log.v(TAG, "createMessage: created message " + message);
            return message;
        } catch (MessagingException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
        }
        return null;
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
    private BodyPart createBodyPart(String fileType) throws MessagingException, FileNotFoundException, UnsupportedEncodingException {
        Log.v(TAG, "createBodyPart: fileType = " + fileType);
        // Get the FileExport instance which can export mContext file type.
        final FileExport fileExport;
        if ("csv".equals(fileType)) fileExport = new CSVExport(mContext, null);
        else if ("html".equals(fileType)) fileExport = new HTMLExport(mContext, true, null);
        else if ("excel".equals(fileType)) fileExport = new ExcelExport(mContext, null);
        else if ("kml".equals(fileType)) fileExport = new KMLExport(mContext, null, NetMonColumns.SOCKET_CONNECTION_TEST);
        else
            /*if ("db".equals(fileType)) */fileExport = new DBExport(mContext, null);
        BodyPart bp = new MimeBodyPart();
        File file = fileExport.export();
        DataSource dataSource = new FileDataSource(file);
        bp.setDataHandler(new DataHandler(dataSource));
        bp.setFileName(file.getName());
        Log.v(TAG, "created body part for " + fileExport);
        return bp;
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
