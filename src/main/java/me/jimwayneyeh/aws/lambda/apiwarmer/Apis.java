package me.jimwayneyeh.aws.lambda.apiwarmer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.model.GetResourcesRequest;
import com.amazonaws.services.apigateway.model.GetResourcesResult;
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.GetStagesRequest;
import com.amazonaws.services.apigateway.model.GetStagesResult;
import com.amazonaws.services.apigateway.model.Resource;
import com.amazonaws.services.apigateway.model.RestApi;
import com.amazonaws.services.apigateway.model.Stage;

public class Apis {
  private static final Logger log = LoggerFactory.getLogger(Apis.class);
  
  private AmazonApiGateway apiGateway;
  private Regions region;
  private HashSet<String> selectedStages = new HashSet<String>();
  
  private TreeSet<URI> uris = null;
  
  public Apis (Regions region) {
    this.apiGateway = AmazonApiGatewayClient.builder()
        .withRegion(region)
        .build();
    
    this.region = region;
  }
  
  public Apis setSelectedStages (String stages) {
    if (StringUtils.isNotBlank(stages)) {
      setSelectedStages(stages.split(","));
    }
    return this;
  }
  
  public Apis setSelectedStages (String[] stages) {
    if (stages == null) {
      return this;
    }
    
    for (String stage : stages) {
      if (StringUtils.isNotBlank(stage)) {
        selectedStages.add(stage.trim());
      }
    }
    
    return this;
  }
  
  public TreeSet<URI> getUris () {
    uris = new TreeSet<URI>();
    
    GetRestApisRequest request = new GetRestApisRequest();
    GetRestApisResult apis = apiGateway.getRestApis(request);
    
    for (RestApi api : apis.getItems()) {
      log.trace("ID of API '{}' at {}: {}", api.getName(), region.getName(), api.getId());
      findUris(api.getId());
    }
    
    try {
      return uris;
    } finally {
      uris = null;
    }
  }
  
  private void findUris (String apiId) {
    
    log.debug("Find API stages for API '{}'...", apiId);
    GetStagesRequest stageRequest = new GetStagesRequest();
    stageRequest.setRestApiId(apiId);
    GetStagesResult stages = apiGateway.getStages(stageRequest);
    
    for (Stage stage : stages.getItem()) {
      log.trace("\tstage: {}", stage.getStageName());
      
      if (selectedStages.size() == 0) {
        findUris(apiId, stage);
      } else {
        if (selectedStages.contains(stage.getStageName())) {
          findUris(apiId, stage);
        }
      }
    }
  }
  
  private void findUris (String apiId, Stage stage) {
    
    log.debug("Find API resources for API '{}'...", apiId);
    GetResourcesRequest resourcesRequest = new GetResourcesRequest();
    resourcesRequest.setRestApiId(apiId);
    GetResourcesResult resources = apiGateway.getResources(resourcesRequest);
    
    for (Resource apiResource : resources.getItems()) {
      log.trace("\tResource: {}", apiResource.getPath());
      findUri(apiId, stage, apiResource);
    }
  }
  
  private void findUri (String apiId, Stage stage, Resource apiResource) {
    try {
      URI uri = new URIBuilder()
          .setScheme("https")
          .setHost(
              String.format("%s.execute-api.%s.amazonaws.com", apiId, region.getName()))
          .setPath(stage.getStageName())
          .setPath(apiResource.getPath())
          .build();
      log.trace("URI: {}", uri);
      uris.add(uri);
    } catch (URISyntaxException e) {
      log.error("Error occurred when building URI for API '{}' at '{}'.",
          apiId, region.getName(), e);
    }
  }
}
