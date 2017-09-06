package org.keycloak.documentation.test;

import org.apache.log4j.Logger;

public class GettingStartedTest extends AbstractDocsTest {

    private static final Logger logger = Logger.getLogger(GettingStartedTest.class);

    @Override
    public String getGuide() {
        return "getting_started";
    }

    public Logger getLogger() {
        return logger;
    }

}
