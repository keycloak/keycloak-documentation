package org.keycloak.documentation.test.utils;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public class HttpUtils {

    private CloseableHttpClient client;

    public HttpUtils() {
        try {
            client = createClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    client.close();
                } catch (IOException e) {
                }
            }
        });
    }

    public Response load(String url) {
        return exec(new HttpGet(url));
    }

    public Response isValid(String url) {
        return exec(new HttpHead(url));
    }

    private Response exec(HttpUriRequestBase method) {
        Response response = new Response();

        HttpClientResponseHandler<String> responseHandler = new HttpClientResponseHandler<String>() {
            @Override
            public String handleResponse(ClassicHttpResponse r) throws IOException {
                int status = r.getCode();

                if (status == HttpStatus.SC_SUCCESS) {
                    response.setSuccess(true);

                    HttpEntity entity = r.getEntity();
                    try {
                        String c = entity != null ? EntityUtils.toString(entity) : "";
                        response.setContent(c);
                    } catch (ParseException e) {
                        throw new ClientProtocolException(e);
                    }
                } else if (status / 100 == 3) {
                    String location = r.getFirstHeader("Location").getValue();
                    response.setRedirectLocation(location);
                    response.setSuccess(false);
                } else {
                    response.setError("invalid status code " + status);
                    response.setSuccess(false);
                }
                return "";
            }
        };

        try {
            client.execute(method, responseHandler);
        } catch (Exception e) {
            e.printStackTrace();
            response.setError("exception " + e.getMessage());
            response.setSuccess(false);
        }

        return response;
    }

    private static CloseableHttpClient createClient() throws Exception {
        return HttpClientBuilder.create()
            .setRetryStrategy(new DefaultHttpRequestRetryStrategy(
                Constants.HTTP_RETRY,
                TimeValue.ofSeconds(1L)
            ))
            .disableCookieManagement()
            .disableRedirectHandling()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(new NoopSSLConnectionSocketFactory())
                    .build()
            )
            .build();
    }

    private static class NoopSSLConnectionSocketFactory extends SSLConnectionSocketFactory {
        private static SSLContext sslContext;

        static {
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(
                    null,
                    new X509TrustManager[] {
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                    },
                    null
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public NoopSSLConnectionSocketFactory() {
            super(sslContext, new NoopHostnameVerifier());
        }

        @Override
        protected void verifySession(String hostname, SSLSession sslSession) throws SSLException {
            // no-op
        }
    }

    public static class Response {

        private boolean success;
        private String content;
        private String redirectLocation;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        private void setSuccess(boolean success) {
            this.success = success;
        }

        public String getContent() {
            return content;
        }

        private void setContent(String content) {
            this.content = content;
        }

        public String getRedirectLocation() {
            return redirectLocation;
        }

        private void setRedirectLocation(String redirectLocation) {
            this.redirectLocation = redirectLocation;
        }

        public String getError() {
            return error;
        }

        private void setError(String error) {
            this.error = error;
        }
    }

}
