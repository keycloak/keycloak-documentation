package org.keycloak.documentation.test;

import org.apache.log4j.Logger;

public class ServerInstallationTest extends AbstractDocsTest {

    private static final Logger logger = Logger.getLogger(ServerInstallationTest.class);

    @Override
    public String getGuide() {
        return "server_installation";
    }

    public Logger getLogger() {
        return logger;
    }

}
