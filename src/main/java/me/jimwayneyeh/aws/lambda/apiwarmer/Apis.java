package me.jimwayneyeh.aws.lambda.apiwarmer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.apigateway.APIGatewayClient;
import software.amazon.awssdk.services.apigateway.model.GetResourcesRequest;
import software.amazon.awssdk.services.apigateway.model.GetResourcesResponse;
import software.amazon.awssdk.services.apigateway.model.GetRestApisRequest;
import software.amazon.awssdk.services.apigateway.model.GetRestApisResponse;
import software.amazon.awssdk.services.apigateway.model.GetStagesRequest;
import software.amazon.awssdk.services.apigateway.model.GetStagesResponse;
import software.amazon.awssdk.services.apigateway.model.Resource;
import software.amazon.awssdk.services.apigateway.model.RestApi;
import software.amazon.awssdk.services.apigateway.model.Stage;

public class Apis {
  private static final Logger LOGGER = LoggerFactory.getLogger(Apis.class);
  
  private APIGatewayClient apiGateway;
  private Region region;
  
  private HashSet<String> selectedStages = new HashSet<String>();
  
  private TreeSet<URI> uris = null;
  
  public Apis (Region region) {
    this.region = region;
    this.apiGateway = APIGatewayClient.builder().region(region).build();
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
    
    GetRestApisResponse apis =
        apiGateway.getRestApis(GetRestApisRequest.builder().build());
    
    for (RestApi api : apis.items()) {
      LOGGER.trace("ID of API '{}' at {}: {}", api.name(), region, api.id());
      findUris(api.id());
    }
    
    try {
      return uris;
    } finally {
      uris = null;
    }
  }
  
  private void findUris (String apiId) {
    
    LOGGER.debug("Find API stages for API '{}'...", apiId);
    GetStagesResponse stages = apiGateway.getStages(
        GetStagesRequest.builder().restApiId(apiId).build());
    
    for (Stage stage : stages.item()) {
      LOGGER.trace("\tstage: {}", stage.stageName());
      
      if (selectedStages.size() == 0) {
        findUris(apiId, stage);
      } else {
        if (selectedStages.contains(stage.stageName())) {
          findUris(apiId, stage);
        }
      }
    }
  }
  
  private void findUris (String apiId, Stage stage) {
    
    LOGGER.debug("Find API resources for API '{}'...", apiId);
    GetResourcesResponse resources = apiGateway.getResources(
        GetResourcesRequest.builder().restApiId(apiId).build());
    
    for (Resource apiResource : resources.items()) {
      LOGGER.trace("\tResource: {}", apiResource.path());
      findUri(apiId, stage, apiResource);
    }
  }
  
  private void findUri (String apiId, Stage stage, Resource apiResource) {
    try {
      URI uri = new URIBuilder()
          .setScheme("https")
          .setHost(
              String.format("%s.execute-api.%s.amazonaws.com", apiId, region.value()))
          .setPath(stage.stageName())
          .setPath(apiResource.path())
          .build();
      LOGGER.trace("URI: {}", uri);
      uris.add(uri);
    } catch (URISyntaxException e) {
      LOGGER.error("Error occurred when building URI for API '{}' at '{}'.",
          apiId, region, e);
    }
  }
}