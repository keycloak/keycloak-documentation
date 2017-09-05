package org.keycloak.documentation.test;

import org.apache.log4j.Logger;

public class UpgradingTest extends AbstractDocsTest {

    private static final Logger logger = Logger.getLogger(UpgradingTest.class);

    @Override
    public String getGuide() {
        return "upgrading";
    }

    public Logger getLogger() {
        return logger;
    }

}
