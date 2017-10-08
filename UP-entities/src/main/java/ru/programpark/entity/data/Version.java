package ru.programpark.entity.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class Version {
    private static String formalVersion = "1.1";
    private static String gitHash = null;
    private static String gitDate = null;
    private static String buildDate = null;

    public static String getFormalVersion() {
        return formalVersion;
    }

    private static void loadGitData() {
        String path = "/ru/programpark/entity/git.properties";
        Properties props = new Properties();
        try {
            InputStream in = Version.class.getResourceAsStream(path);
            if (in != null) props.load(in);
            gitHash = props.get("git.commit.id.describe").toString()
                           .replaceAll("-dirty", "*");
            gitDate = props.get("git.commit.time").toString();
        } catch (Exception e) {}
    }

    public static String getGitHash() {
        if (gitHash == null) loadGitData();
        return gitHash;
    }

    public static String getGitDate() {
        if (gitDate == null) loadGitData();
        return gitDate;
    }

    public static String getBuildDate() {
        if (buildDate == null) {
            Date date = new Date();
            DateFormat datef = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            try {
                URL resource = Version.class.getResource("Version.class");
                File file = null;
                if (resource != null) {
                    switch (resource.getProtocol()) {
                    case "file":
                        file = new File(resource.toURI());
                        break;
                    case "jar":
                        String path = resource.getPath();
                        file = new File(path.substring(5, path.indexOf("!")));
                        break;
                    }
                }
                if (file != null) date = new Date(file.lastModified());
            } catch (Exception e) {}
            buildDate = datef.format(date);
        }
        return buildDate;
    }

    public static String string() {
        loadGitData();
        return "UniversalPlanner" + " v. " + formalVersion +
            " (" + getBuildDate()
            + ((gitHash == null) ? "" : (" " + gitHash)) + ")";
    }
}
