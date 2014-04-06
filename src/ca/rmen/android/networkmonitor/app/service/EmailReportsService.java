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
package ca.rmen.android.networkmonitor.app.service;

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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.export.CSVExport;
import ca.rmen.android.networkmonitor.app.export.DBExport;
import ca.rmen.android.networkmonitor.app.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.export.FileExport;
import ca.rmen.android.networkmonitor.app.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.export.SummaryExport;
import ca.rmen.android.networkmonitor.app.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.EmailPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.EmailSecurity;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Sends a mail to a recipient or recipients, including (or not) some file attachments with exports of the log.
 */
public class EmailReportsService extends IntentService {

    private static final String TAG = Constants.TAG + EmailReportsService.class.getSimpleName();
    private static final String ENCODING = "UTF-8";
    private static final int PENDING_INTENT_REQUEST_CODE = 123;

    public EmailReportsService() {
        super(TAG);
        Log.v(TAG, "Constructor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent, intent = " + intent);

        final EmailPreferences emailPreferences = NetMonPreferences.getInstance(this).getEmailPreferences();
        if (emailPreferences.isValid()) {
            Session mailSession = getMailSession(emailPreferences);
            Message message = createMessage(mailSession, emailPreferences);
            if (message != null) sendMail(mailSession, message);
        } else {
            Log.w(TAG, "Cannot send mail with the current email settings: " + emailPreferences);
        }

        // Schedule our next run
        scheduleEmailReports();
    }

    private static void sendMail(Session mailSession, Message message) {
        Transport transport = null;
        try {
            transport = mailSession.getTransport();
            transport.connect();
            transport.sendMessage(message, message.getAllRecipients());
            Log.v(TAG, "sent message");
        } catch (MessagingException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
        } finally {
            try {
                if (transport != null) transport.close();
            } catch (MessagingException e) {
                Log.e(TAG, "onHandleIntent Could not close the transport", e);
            }
        }
    }

    private static Session getMailSession(final EmailPreferences emailPreferences) {
        // Set up properties for mail sending.
        Properties props = new Properties();
        String propertyPrefix, transportProtocol;
        if (emailPreferences.security == EmailSecurity.SSL) {
            propertyPrefix = "mail.smtps.";
            transportProtocol = "smtps";
        } else {
            propertyPrefix = "mail.smtp.";
            transportProtocol = "smtp";
        }
        props.put(propertyPrefix + "host", emailPreferences.server);
        props.put(propertyPrefix + "port", String.valueOf(emailPreferences.port));
        props.put(propertyPrefix + "auth", "true");
        props.put(propertyPrefix + "timeout", "15000");
        props.put(propertyPrefix + "connectiontimeout", "15000");
        props.put(propertyPrefix + "writetimeout", "15000");
        if (emailPreferences.security == EmailSecurity.TLS) props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", String.valueOf(true));
        props.put("mail.transport.protocol", transportProtocol);
        Log.v(TAG, "Sending mail on " + emailPreferences.server + ":" + emailPreferences.port + " using " + emailPreferences.security + " security");

        // Create the session with the properties.
        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailPreferences.user, emailPreferences.password);
            }
        });
        return mailSession;
    }

    /**
     * @return a Message we can send using the given mailSession, or null if there was a problem creating the message.
     */
    private Message createMessage(Session mailSession, EmailPreferences emailPreferences) {

        try {
            // Create the mail
            MimeMessage message = new MimeMessage(mailSession);
            // Set the subject, from, and to fields.
            String subject = getString(R.string.export_subject_send_log);
            message.setSubject(MimeUtility.encodeText(subject, ENCODING, "Q"));
            String from = getFromAddress(emailPreferences);
            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emailPreferences.recipients));

            // Now for the body of the mail...
            String messageText = getMessageBody(emailPreferences);

            // Case 1: No file attachment, just send a plain text mail with the summary.
            if (emailPreferences.reportFormats.isEmpty()) {
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
                for (String fileType : emailPreferences.reportFormats) {
                    FileExport fileExport = getFileExport(fileType);
                    bp = createBodyPart(fileExport);
                    mp.addBodyPart(bp);
                }
                message.setContent(mp);
                return message;
            }
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
    private static String getFromAddress(EmailPreferences emailPreferences) {
        // We try to guess the from address.
        final String from;
        // If the user name for the smtp server is an e-mail address, we just use that.
        if (emailPreferences.user.indexOf("@") > 0) {
            from = emailPreferences.user;
        }
        // Otherwise we use the user@server.  We try to strip any "smtp" part of the server domain.
        else {
            String server = emailPreferences.server.replaceAll("smtp[^\\.]*\\.", "");
            from = emailPreferences.user + "@" + server;
        }
        Log.v(TAG, "Sending mail from " + from);
        return from;
    }

    private FileExport getFileExport(String fileType) throws FileNotFoundException, UnsupportedEncodingException {
        Log.v(TAG, "getFileExport: fileType = " + fileType);
        if ("csv".equals(fileType)) return new CSVExport(this, null);
        if ("html".equals(fileType)) return new HTMLExport(this, true, null);
        if ("excel".equals(fileType)) return new ExcelExport(this, null);
        if ("kml".equals(fileType)) return new KMLExport(this, null, NetMonColumns.SOCKET_CONNECTION_TEST);
        if ("db".equals(fileType)) return new DBExport(this, null);
        return null;
    }

    private static BodyPart createBodyPart(FileExport fileExport) throws MessagingException {
        Log.v(TAG, "createBodyPart: fileExport = " + fileExport);
        BodyPart bp = new MimeBodyPart();
        File file = fileExport.export();
        DataSource dataSource = new FileDataSource(file);
        bp.setDataHandler(new DataHandler(dataSource));
        bp.setFileName(file.getName());
        Log.v(TAG, "created body part for " + fileExport);
        return bp;
    }

    private String getMessageBody(EmailPreferences emailPreferences) {
        String reportSummary = SummaryExport.getSummary(this);
        String dateRange = SummaryExport.getDataCollectionDateRange(this);
        String messageBody = getString(R.string.export_message_text, dateRange);
        if (!emailPreferences.reportFormats.isEmpty()) messageBody += getString(R.string.export_message_text_file_attached);
        messageBody += reportSummary;
        return messageBody;
    }

    private void scheduleEmailReports() {
        Log.v(TAG, "scheduleEmailReports");
        PendingIntent pendingIntent = getPendingIntent(this);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        int emailInterval = NetMonPreferences.getInstance(this).getEmailReportInterval();
        Log.v(TAG, "email interval = " + emailInterval);
        if (emailInterval > 0) alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + emailInterval, pendingIntent);
    }

    static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context.getApplicationContext(), EmailReportsService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context.getApplicationContext(), PENDING_INTENT_REQUEST_CODE, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pendingIntent;
    }

}
