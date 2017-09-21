package me.jimwayneyeh.aws.lambda.apiwarmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import me.jimwayneyeh.aws.lambda.apiwarmer.resources.HttpResource;
import me.jimwayneyeh.aws.lambda.apiwarmer.resources.ThreadResource;

public class ApiRequestHandler implements RequestStreamHandler {
  private static final Logger log = 
      LoggerFactory.getLogger(ApiRequestHandler.class);
  
  public ApiRequestHandler () {
    HttpResource.initiateHttpClient();
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    log.info("API heating is launched.");
    
    Set<Regions> regions = getRegions();
    log.info("{} regions are going to be warmed.", regions.size());
    
    for (Regions region : regions) {
      log.info("Warming region '{}'...", region.getName());
      TreeSet<URI> uris = new Apis(region)
          .setSelectedStages(System.getenv("stages"))
          .getUris();
      
      log.debug("{} APIs are found in region '{}'.", 
          uris.size(), region.getName());
      warmUpApis(uris);
    }
    
    log.info("The environment is warm now.");
  }
  
  private Set<Regions> getRegions () {
    TreeSet<Regions> regions = new TreeSet<Regions>();
    String regionsStr = System.getenv("regions");
    
    if (StringUtils.isNotBlank(regionsStr)) {
      for (String regionStr : regionsStr.split(",")) {
        regions.add(Regions.fromName(regionStr));
      }
    }
    
    return regions;
  }
  
  private void warmUpApis (TreeSet<URI> uris) {
    TreeMap<URI, ScheduledFuture<?>> futures = 
        new TreeMap<URI, ScheduledFuture<?>>();
    
    for (URI uri : uris) {
      ScheduledFuture<?> future = ThreadResource.getPool()
          .schedule(new ApiRequest(uri), 0, TimeUnit.MICROSECONDS);
      futures.put(uri, future);
    }
    
    // Wait for execution.
    int count = 1;
    for (Entry<URI, ScheduledFuture<?>> entry : futures.entrySet()) {
      URI uri = entry.getKey();
      ScheduledFuture<?> future = entry.getValue();
      log.trace("Waming up {}/{} '{}'...", count++, uris.size(), entry.getKey());
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        log.warn("Execution error with URI '{}'.", uri, e);
      }
    }
  }
  
  private static class ApiRequest implements Runnable {
    private static final Logger log = 
        LoggerFactory.getLogger(ApiRequest.class);
    private URI uri;
    
    public ApiRequest (URI uri) {
      this.uri = uri;
    }
    
    @Override
    public void run() {
      HttpGet request = new HttpGet(uri);
      try (CloseableHttpResponse response = HttpResource.getSingleton().execute(request)) {
        log.trace("Status code '{}' is got for URI '{}'.", 
            response.getStatusLine().getStatusCode(), uri);
      } catch (Throwable t) {
        log.trace("Error when calling API '{}': {}", uri, t.getMessage());
      } finally {
        request.releaseConnection();
      }
    }
  }
}