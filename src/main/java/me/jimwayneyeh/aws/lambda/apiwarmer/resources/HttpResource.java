package me.jimwayneyeh.aws.lambda.apiwarmer.resources;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpResource {
  private static CloseableHttpClient httpClient = null;
  
  public static CloseableHttpClient getSingleton () {
    if (httpClient == null) {
      initiateHttpClient();
    }
    return httpClient;
  }
  
  public static synchronized void initiateHttpClient () {
    if (httpClient == null) {
      // Create the configuration for Apache HTTP client.
      RequestConfig defaultRequestConfig = RequestConfig.custom()
          .setSocketTimeout(10000)
          .setConnectTimeout(10000)
          .setConnectionRequestTimeout(10000)
          .build();
      
      httpClient = HttpClients.custom()
          .setDefaultRequestConfig(defaultRequestConfig)
          .build();
    }
  }
  
  private HttpResource () {}
}