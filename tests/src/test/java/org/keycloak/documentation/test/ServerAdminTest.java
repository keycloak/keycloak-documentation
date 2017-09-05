package org.keycloak.documentation.test;

import org.apache.log4j.Logger;

public class ServerAdminTest extends AbstractDocsTest {

    private static final Logger logger = Logger.getLogger(ServerAdminTest.class);

    @Override
    public String getGuide() {
        return "server_admin";
    }

    public Logger getLogger() {
        return logger;
    }

}
