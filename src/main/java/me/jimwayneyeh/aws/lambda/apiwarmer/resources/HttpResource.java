package me.jimwayneyeh.aws.lambda.apiwarmer.resources;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResource {
  private static final Logger LOGGER = 
      LoggerFactory.getLogger(HttpResource.class);
  private static CloseableHttpClient httpClient = null;
  
  public static CloseableHttpClient getSingleton () {
    if (httpClient == null) {
      initiateHttpClient();
    }
    return httpClient;
  }
  
  public static synchronized void initiateHttpClient () {
    if (httpClient == null) {
      LOGGER.info("Initiate a singleton of HTTP client.");
      
      // Create the configuration for Apache HTTP client.
      // We don't actually care about the response, so it is set to timeout
      // quickly.
      RequestConfig defaultRequestConfig = RequestConfig.custom()
          .setSocketTimeout(5000)
          .setConnectTimeout(5000)
          .setConnectionRequestTimeout(5000)
          .build();
      
      httpClient = HttpClients.custom()
          .setDefaultRequestConfig(defaultRequestConfig)
          .build();
    }
  }
  
  private HttpResource () {}
}