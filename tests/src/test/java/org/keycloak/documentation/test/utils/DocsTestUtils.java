package org.keycloak.documentation.test.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocsTestUtils {

    public String readBody(File htmlFile) throws IOException {
        String s = FileUtils.readFileToString(htmlFile, "utf-8");

        Pattern p = Pattern.compile("<body.*?>(.*?)</body>.*?",Pattern.DOTALL);
        Matcher m = p.matcher(s);

        m.find();
        return m.group(1);
    }

    public List<String> findMissingVariables(String body, List<String> ignoredVariables) {
        List<String> missingVariables = new LinkedList<>();
        Pattern p = Pattern.compile("[^$/]\\{([^ }]*)}");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String key = m.group(1);
            if (!key.isEmpty() && !ignoredVariables.contains(key)) {
                missingVariables.add(key);
            }
        }
        return missingVariables;
    }

    public List<String> findMissingIncludes(String body) {
        List<String> missingIncludes = new LinkedList<>();
        Pattern p = Pattern.compile("Unresolved directive.*");
        Matcher m = p.matcher(body);
        if (m.find()) {
            missingIncludes.add(m.group());
        }
        return missingIncludes;
    }

    public List<String> findMissingImages(String body, File guideDir) {
        List<String> missingImages = new LinkedList<>();
        Pattern p = Pattern.compile("<img src=\"([^ \"]*)[^>]*\">");
        Matcher m = p.matcher(body);
        while (m.find()) {
            String image = m.group(1);
            File f = new File(guideDir, image);
            if (!f.isFile()) {
                missingImages.add(image);
            }
        }
        return missingImages;
    }

}
