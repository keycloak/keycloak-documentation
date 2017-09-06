package org.keycloak.documentation.test.utils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkTestUtils {

    private static final long LINK_CHECK_EXPIRATION = TimeUnit.DAYS.toMillis(1);
    private static final Logger logger = Logger.getLogger(LinkTestUtils.class);

    private File verifiedLinksCacheFile;
    private Map<String, Long> verifiedLinks;

    public LinkTestUtils(File verifiedLinksCacheFile) {
        this.verifiedLinksCacheFile = verifiedLinksCacheFile;
        this.verifiedLinks = loadCheckedLinksCache();
    }

    public void close() {
        saveCheckedLinksCache();
    }

    public List<String> findInvalidInternalAnchors(String body) {
        List<String> invalidInternalAnchors = new LinkedList<>();
        Pattern p = Pattern.compile("<a href=\"([^ \"]*)[^>]*\">");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String link = m.group(1);

            if (link.startsWith("#")) {
                if (!body.contains("id=\"" + link.substring(1) + "\"")) {
                    invalidInternalAnchors.add(link.substring(1));
                }
            }
        }
        return invalidInternalAnchors;
    }

    public List<InvalidLink> findInvalidLinks(String body, List<String> ignoredLinks, List<String> ignoredLinkRedirects) {
        List<InvalidLink> invalidLinks = new LinkedList<>();
        Pattern p = Pattern.compile("<a href=\"([^ \"]*)[^>]*\">");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String link = m.group(1);

            if (verifyLink(link, ignoredLinks)) {
                if (link.startsWith("http")) {
                    String anchor = link.contains("#") ? link.split("#")[1] : null;

                    String error = null;
                    HttpURLConnection.setFollowRedirects(false);
                    HttpURLConnection connection = null;
                    try {
                        connection = (HttpURLConnection) new URL(link).openConnection();
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(5000);
                        int status = connection.getResponseCode();
                        if (status != 200) {
                            if (status == 302) {
                                String location = URLDecoder.decode(connection.getHeaderField("Location"), "utf-8");
                                if (!validRedirect(location, ignoredLinkRedirects)) {
                                    error = "invalid redirect to " + location;
                                }
                            } else {
                                error = "invalid status code " + status;
                            }
                        } else {
                            if (anchor != null) {
                                StringWriter w = new StringWriter();
                                IOUtils.copy(connection.getInputStream(), w, "utf-8");
                                if (!(w.toString().contains("id=\"" + anchor + "\"") || w.toString().contains("name=\"" + anchor + "\""))) {
                                    error = "invalid anchor " + anchor;
                                }
                            }
                        }
                    } catch (Exception e) {
                        error = "exception " + e.getMessage();
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                    if (error == null) {
                        verifiedLinks.put(link, System.currentTimeMillis());

                        logger.debug("Checked link: " + link);
                    } else {
                        logger.debug("Bad link: " + link + " (" + error + ")");
                        invalidLinks.add(new InvalidLink(link, error));
                    }
                }
            }
        }

        return invalidLinks;
    }


    private boolean verifyLink(String link, List<String> ignoredLinks) {
        for (String ignored : ignoredLinks) {
            if (ignored.endsWith("*") && link.startsWith(ignored.substring(0, ignored.length() - 1))) {
                return false;
            } else if (ignored.equals(link)) {
                return false;
            }
        }

        if (verifiedLinks.containsKey(link)) {
            return false;
        }

        return true;
    }

    private boolean validRedirect(String location, List<String> ignoredLinkRedirects) {
        for (String valid : ignoredLinkRedirects) {
            if (valid.endsWith("*") && location.startsWith(valid.substring(0, valid.length() - 1))) {
                return true;
            } else if (valid.equals(location)) {
                return true;
            }
        }
        return false;
    }


    public static class InvalidLink {

        private String link;
        private String error;

        public InvalidLink(String link, String error) {
            this.link = link;
            this.error = error;
        }

        public String getLink() {
            return link;
        }

        public String getError() {
            return error;
        }
    }

    private Map<String, Long> loadCheckedLinksCache() {
        Map<String, Long> m = new HashMap<>();
        try {
            if (verifiedLinksCacheFile.isFile()) {
                Properties p = new Properties();
                p.load(new FileInputStream(verifiedLinksCacheFile));
                for(Map.Entry<Object, Object> e : p.entrySet()) {
                    long checked = Long.valueOf((String) e.getValue());
                    if (checked < System.currentTimeMillis() + LINK_CHECK_EXPIRATION) {
                        m.put((String) e.getKey(), System.currentTimeMillis());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    private void saveCheckedLinksCache() {
        try {
            Properties p = new Properties();
            for (Map.Entry<String, Long> e : verifiedLinks.entrySet()) {
                p.put(e.getKey(), Long.toString(e.getValue()));
            }
            FileOutputStream os = new FileOutputStream(verifiedLinksCacheFile);
            p.store(os, null);
            os.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
