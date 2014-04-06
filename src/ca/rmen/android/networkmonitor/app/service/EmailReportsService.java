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
import java.util.Set;

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

import android.app.IntentService;
import android.content.Intent;

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
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.EmailSecurity;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

public class EmailReportsService extends IntentService {

    private static final String TAG = Constants.TAG + EmailReportsService.class.getSimpleName();
    private static final String ENCODING = "UTF-8";

    public EmailReportsService() {
        super(TAG);
        Log.v(TAG, "Constructor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent, intent = " + intent);
        Set<String> fileTypes = NetMonPreferences.getInstance(this).getEmailReportFormats();
        String reportSummary = SummaryExport.getSummary(this);
        String dateRange = SummaryExport.getDataCollectionDateRange(this);
        String messageBody = getString(R.string.export_message_text, dateRange);
        if (!fileTypes.isEmpty()) messageBody += getString(R.string.export_message_text_file_attached);
        messageBody += reportSummary;

        String serverName = NetMonPreferences.getInstance(this).getEmailServer();
        int port = NetMonPreferences.getInstance(this).getEmailPort();
        final String user = NetMonPreferences.getInstance(this).getEmailUser();
        final String password = NetMonPreferences.getInstance(this).getEmailPassword();
        EmailSecurity security = NetMonPreferences.getInstance(this).getEmailSecurity();
        String recipients = NetMonPreferences.getInstance(this).getEmailRecipients();

        // Set up properties for mail sending.
        Properties props = new Properties();
        props.put("mail.smtp.host", serverName);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        if (security == EmailSecurity.TLS) props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", String.valueOf(true));
        props.put("mail.transport.protocol", "smtp");
        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        // Create the mail
        MimeMessage message = new MimeMessage(mailSession);
        String subject = getString(R.string.export_subject_send_log);

        // Set the subject, from, and to fields.
        try {
            message.setSubject(MimeUtility.encodeText(subject, ENCODING, "Q"));
            message.setFrom(new InternetAddress(user));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            if (fileTypes.isEmpty()) {
                // No file attachment, just send a plain text mail with the summary.
                message.setHeader("Content-Transfer-Encoding", "quoted-printable");
                message.setText(messageBody, ENCODING);
            } else {
                // We have file attachments, so we need to do a multi-part mail.
                // Add the plain text version of the mail
                Multipart mp = new MimeMultipart("alternative");
                BodyPart bp = new MimeBodyPart();
                bp.setContent(messageBody, "text/plain;charset=" + ENCODING);
                bp.setHeader("Content-Transfer-Encoding", "quoted-printable");
                mp.addBodyPart(bp);
                // Now add the file attachments.
                for (String fileType : fileTypes) {
                    FileExport fileExport = getFileExport(fileType);
                    bp = createBodyPart(fileExport);
                    mp.addBodyPart(bp);
                }
                message.setContent(mp);
            }
        } catch (MessagingException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
            return;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
            return;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not send mail " + e.getMessage(), e);
            return;
        }

        // Send the mail
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

    private FileExport getFileExport(String fileType) throws FileNotFoundException, UnsupportedEncodingException {
        Log.v(TAG, "getFileExport: fileType = " + fileType);
        if ("csv".equals(fileType)) return new CSVExport(this, null);
        if ("html".equals(fileType)) return new HTMLExport(this, true, null);
        if ("excel".equals(fileType)) return new ExcelExport(this, null);
        if ("kml".equals(fileType)) return new KMLExport(this, null, NetMonColumns.SOCKET_CONNECTION_TEST);
        if ("db".equals(fileType)) return new DBExport(this, null);
        return null;
    }

    private BodyPart createBodyPart(FileExport fileExport) throws MessagingException {
        Log.v(TAG, "createBodyPart: fileExport = " + fileExport);
        BodyPart bp = new MimeBodyPart();
        File file = fileExport.export();
        DataSource dataSource = new FileDataSource(file);
        bp.setDataHandler(new DataHandler(dataSource));
        bp.setFileName(file.getName());
        Log.v(TAG, "created body part for " + fileExport);
        return bp;
    }

}
