package org.keycloak.documentation.test;

import org.apache.log4j.Logger;

public class AuthorizationServicesTest extends AbstractDocsTest {

    private static final Logger logger = Logger.getLogger(AuthorizationServicesTest.class);

    @Override
    public String getGuide() {
        return "authorization_services";
    }

    public Logger getLogger() {
        return logger;
    }

}
