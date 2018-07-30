package org.keycloak.documentation.test.utils;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.keycloak.documentation.test.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkUtils {

    private static final Logger logger = Logger.getLogger(LinkUtils.class);

    private HttpUtils http = new HttpUtils();
    private Config config;
    private File verifiedLinksCacheFile;
    private boolean verbose;
    private Map<String, Long> verifiedLinks;

    public LinkUtils(Config config, boolean verbose) {
        this.config = config;
        this.verifiedLinksCacheFile = config.getVerifiedLinksCache();
        this.verbose = verbose;
        this.verifiedLinks = loadCheckedLinksCache();
    }

    public void close() {
        saveCheckedLinksCache();
    }

    public Set<String> findInvalidInternalAnchors(String body) {
        Set<String> invalidInternalAnchors = new HashSet<>();
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

    public List<InvalidLink> findInvalidLinks(String body, List<String> ignoredLinks, List<String> ignoredLinkRedirects) throws IOException {
        List<InvalidLink> invalidLinks = new LinkedList<>();
        Pattern p = Pattern.compile("<a href=\"([^ \"]*)[^>]*\">");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String link = m.group(1);

            if (verifyLink(link, ignoredLinks, invalidLinks)) {
                if (link.startsWith("http")) {
                    String anchor = link.contains("#") ? link.split("#")[1] : null;
                    String error = null;

                    HttpUtils.Response response = http.load(link, anchor != null, false);

                    if (response.getRedirectLocation() != null) {
                        if (!validRedirect(response.getRedirectLocation(), ignoredLinkRedirects)) {
                            error = "invalid redirect to " + response.getRedirectLocation();
                        }
                    } else if (response.isSuccess() && anchor != null) {
                        if (!(response.getContent().contains("id=\"" + anchor + "\"") || response.getContent().toString().contains("name=\"" + anchor + "\""))) {
                            error = "invalid anchor " + anchor;
                        }
                    } else {
                        error = response.getError();
                    }

                    if (error == null) {
                        verifiedLinks.put(link, System.currentTimeMillis());

                        if (verbose) {
                            System.out.println("[OK]  " + link);
                        }
                    } else {
                        invalidLinks.add(new InvalidLink(link, error));

                        if (verbose) {
                            System.out.println("[BAD] " + link);
                        }
                    }
                } else if (link.startsWith("file")) {
                    File f = new File(new URL(link).getFile());
                    if (!f.isFile()) {
                        invalidLinks.add(new InvalidLink(link, "local guide not found"));
                    } else {
                        String anchor = link.contains("#") ? link.split("#")[1] : null;
                        if (anchor != null) {
                            String w = FileUtils.readFileToString(f, "utf-8");
                            if (!(w.contains("id=\"" + anchor + "\"") || w.contains("name=\"" + anchor + "\""))) {
                                invalidLinks.add(new InvalidLink(link, "invalid anchor " + anchor));
                            }
                        }
                    }
                }
            }
        }

        return invalidLinks;
    }

    public Set<String> findInvalidImages(String body, File guideDir, String guideUrl) {
        Set<String> missingImages = new HashSet<>();
        Pattern p = Pattern.compile("<img src=\"([^ \"]*)[^>]*\"");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String image = m.group(1);
            if (config.isLoadFromFiles()) {
                File f = new File(guideDir, image);
                if (!f.isFile()) {
                    missingImages.add(image);
                }
            } else {
                if (image.startsWith("./")) {
                    image = guideUrl + image;
                }

                if (!verifiedLinks.containsKey(image)) {
                    boolean valid = http.isValid(image);
                    if (valid) {
                        verifiedLinks.put(image, System.currentTimeMillis());

                        if (verbose) {
                            System.out.println("[OK]  " + image);
                        }
                    } else {
                        missingImages.add(image);

                        if (verbose) {
                            System.out.println("[BAD]  " + image);
                        }
                    }
                }
            }
        }
        return missingImages;
    }

    private boolean verifyLink(String link, List<String> ignoredLinks, List<InvalidLink> invalidLinks) {
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

        for (InvalidLink l : invalidLinks) {
            if (l.getLink().equals(link)) {
                return false;
            }
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
                    if (checked + Constants.LINK_CHECK_EXPIRATION >= System.currentTimeMillis()) {
                        m.put((String) e.getKey(), checked);
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
