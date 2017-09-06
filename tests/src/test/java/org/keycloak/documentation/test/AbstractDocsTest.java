package org.keycloak.documentation.test;

import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.keycloak.documentation.test.utils.DocsTestUtils;
import org.keycloak.documentation.test.utils.LinkTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public abstract class AbstractDocsTest {

    private static File docsRootDir = findDocsRoot();

    private static List<String> ignoredLinkRedirects = loadConfig("/ignored-link-redirects");
    private static List<String> ignoredVariables = loadConfig("/ignored-variables");
    private static List<String> ignoredLinks = loadConfig("/ignored-links");

    private DocsTestUtils utils = new DocsTestUtils();
    private LinkTestUtils linkUtils = new LinkTestUtils(new File(docsRootDir, ".verified-links"));

    private File guideDir;
    private String body;

    @Before
    public void before() throws IOException {
        guideDir = new File(docsRootDir, getGuide() + "/target/generated-docs");

        File htmlFile = new File(guideDir, "index.html");
        if (!htmlFile.isFile()) {
            htmlFile = new File(guideDir, "master.html");
        }

        body = utils.readBody(htmlFile);
    }

    @After
    public void after() {
        linkUtils.close();
    }

    public abstract String getGuide();

    @Test
    public void checkVariables() {
        List<String> missingVariables = utils.findMissingVariables(body, ignoredVariables);
        checkFailures("Variables not found", missingVariables);
    }

    @Test
    public void checkIncludes() {
        List<String> missingIncludes = utils.findMissingIncludes(body);
        checkFailures("Includes not found", missingIncludes);
    }

    @Test
    public void checkImages() {
        List<String> failures = utils.findMissingImages(body, guideDir);
        checkFailures("Images not found", failures);
    }

    @Test
    public void checkInternalAnchors() {
        List<String> invalidInternalAnchors = linkUtils.findInvalidInternalAnchors(body);
        checkFailures("Internal anchors not found", invalidInternalAnchors);
    }

    @Test
    public void checkExternalLinks() {
        List<LinkTestUtils.InvalidLink> invalidLinks = linkUtils.findInvalidLinks(body, ignoredLinks, ignoredLinkRedirects);
        if (!invalidLinks.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid links:");
            for (LinkTestUtils.InvalidLink l : invalidLinks) {
                sb.append("\n");
                sb.append(" * " + l.getLink() + " (" + l.getError() + ")");
                Assert.fail(sb.toString());
            }
        }
    }

    private static File findDocsRoot() {
        File f = new File("").getAbsoluteFile();
        if (f.getName().equals("tests")) {
            f = f.getParentFile();
        }
        return f;
    }

    private void checkFailures(String message, List<String> failures) {
        if (!failures.isEmpty()) {
            Assert.fail(message + ":\n * " + String.join("\n * ", failures));
        }
    }

    private static List<String> loadConfig(String resource) {
        try {
            return IOUtils.readLines(AbstractDocsTest.class.getResourceAsStream(resource), "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
