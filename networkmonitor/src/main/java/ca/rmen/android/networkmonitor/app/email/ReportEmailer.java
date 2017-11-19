/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Set;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.CSVExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.DBExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.ExcelExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.FileExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.GnuplotExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.HTMLExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.SummaryExport;
import ca.rmen.android.networkmonitor.app.dbops.backend.export.kml.KMLExport;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailConfig;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailSecurity;
import ca.rmen.android.networkmonitor.app.service.NetMonNotification;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

/**
 * Sends a mail to a recipient or recipients, including (or not) some file attachments with exports of the log.
 */
public class ReportEmailer {

    private static final String TAG = Constants.TAG + ReportEmailer.class.getSimpleName();
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
        Set<File> attachments = StreamSupport.stream(emailConfig.reportFormats)
                .map(this::createAttachment)
                .collect(Collectors.toSet());
        final String protocol;
        if(emailConfig.security == EmailSecurity.TLS)
            protocol = "TLS";
        else if(emailConfig.security == EmailSecurity.SSL)
            protocol = "SSL";
        else
            protocol = null;
        String from = getFromAddress(emailConfig);
        String[] recipients = TextUtils.split(emailConfig.recipients.trim(), "[,; ]+");
        String subject = mContext.getString(R.string.export_subject_send_log);
        String body = getMessageBody(emailConfig);

        try {
            Emailer.sendEmail(protocol, emailConfig.server, emailConfig.port,
                    emailConfig.user, emailConfig.password,
                    from, recipients,
                    subject,
                    body, attachments);
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
            String server = emailConfig.server.replaceAll("smtp[^.]*\\.", "");
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
                fileExport = new HTMLExport(mContext);
                break;
            case "excel":
                fileExport = new ExcelExport(mContext);
                break;
            case "kml":
                fileExport = new KMLExport(mContext, NetMonColumns.SOCKET_CONNECTION_TEST);
                break;
            case "gnuplot":
                fileExport = new GnuplotExport(mContext);
                break;
            case "db":
            default:
                fileExport = new DBExport(mContext);
                break;
        }
        fileExport.execute(null);
        return fileExport.getFile();
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
