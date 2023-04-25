package com.zerdicorp.acl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ACLLogBuilder {

    private static final String template =
            "Version: {version}\n" +
                    "Author:  {authorShort} <{author}>\n" +
                    "Date:    {datetime}\n\n" +
                    "    {message}\n";

    public static String getCurrentDatetime() {
        return new SimpleDateFormat("EEE MMM d HH:mm:ss y Z").format(new Date());
    }

    public static String getAuthor() throws IOException {
        final Runtime rt = Runtime.getRuntime();
        final String[] commands = {"/bin/sh", "-c", "git config user.email"};
        final Process proc = rt.exec(commands);
        final BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        return stdInput.readLine();
    }

    public static String build(String version, String message) throws IOException {
        final String author = getAuthor();
        final String authorShort = author.split("@")[0];
        final String datetime = getCurrentDatetime();
        final String preparedMessage = String.join("\n    ", message.split("\n"));

        return template
                .replace("{version}", version)
                .replace("{authorShort}", authorShort)
                .replace("{author}", author)
                .replace("{datetime}", datetime)
                .replace("{message}", preparedMessage);
    }
}
