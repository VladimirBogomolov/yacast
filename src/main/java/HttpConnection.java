import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.ByteStreams;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.cookie.RFC6265CookieSpecProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class HttpConnection implements AutoCloseable {
    private final static int MAX_REDIRECTS = 20;
    private final static int MAX_ATTEMPTS = 10;
    private final Logger logger = LogManager.getLogger(getClass());
    private final CloseableHttpAsyncClient client;

    public HttpConnection() {
        client = createHttpClient();
        client.start();
    }

    public String doPost(String url, Map<String, String> params) {

        List<BasicNameValuePair> formParams = params.entrySet()
                                                    .stream()
                                                    .map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
                                                    .collect(Collectors.toList());
        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        request.setEntity(formEntity);
        return doRequest(request);
    }

    public String doPost(String url, JsonNode params, Map<String, String> headers) {
        StringEntity entity = new StringEntity(params.toString(), ContentType.APPLICATION_JSON);
        HttpPost request = new HttpPost(url);
        headers.forEach(request::setHeader);
        request.setEntity(entity);
        return doRequest(request);
    }

    public String doGet(String url) {
        return doRequest(new HttpGet(url));
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    private CloseableHttpAsyncClient createHttpClient() {
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpClientContext context;

        CookieHandler.setDefault(new CookieManager());

        Lookup<CookieSpecProvider> cookieSpecReg = RegistryBuilder.<CookieSpecProvider>create()
            .register(CookieSpecs.STANDARD, new RFC6265CookieSpecProvider())
            .build();
        final List<Header> headers = Arrays.asList(
            new BasicHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"),
            new BasicHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                                    + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"),
            new BasicHeader(HttpHeaders.CONNECTION, "keep-alive"),
            new BasicHeader(HttpHeaders.CACHE_CONTROL, "max-age=0")
        );

        context = HttpClientContext.create();
        context.setCookieSpecRegistry(cookieSpecReg);
        context.setCookieStore(cookieStore);
        RequestConfig requestConfig =
            RequestConfig.custom()
                         .setMaxRedirects(MAX_REDIRECTS)
                         .setCookieSpec(CookieSpecs.STANDARD)
                         .build();

        HttpAsyncClientBuilder builder = HttpAsyncClientBuilder
            .create()
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setDefaultRequestConfig(requestConfig)
            .setDefaultHeaders(headers)
            .setDefaultCookieStore(cookieStore);
        return builder.build();
    }

    private String doRequest(HttpUriRequest request) {
        URL url;
        try {
            url = request.getURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Future<HttpResponse> future = client.execute(request, null);
        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            try {
                HttpResponse httpResponse = future.get(60, TimeUnit.SECONDS);
                if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    logger.warn("Got status code " + httpResponse.getStatusLine().getStatusCode() + " request " + url);
                }
                try (InputStream content = httpResponse.getEntity().getContent()) {
                    return new String(ByteStreams.toByteArray(content));
                }
            } catch (TimeoutException ignored) {
                logger.warn("Time out on request " + url);
            } catch (InterruptedException | ExecutionException | IOException e) {
                throw new RuntimeException(url.toString(), e);
            }
            attempts++;
        }
        throw new RuntimeException("Whoops, it seems site is not accessible " + url);
    }
}