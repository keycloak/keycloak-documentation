package org.keycloak.documentation.test;

import org.apache.log4j.Logger;

public class SecuringAppsTest extends AbstractDocsTest {

    private static final Logger logger = Logger.getLogger(SecuringAppsTest.class);

    @Override
    public String getGuide() {
        return "securing_apps";
    }

    public Logger getLogger() {
        return logger;
    }

}
