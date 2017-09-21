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
import com.amazonaws.services.apigateway.model.GetRestApisRequest;
import com.amazonaws.services.apigateway.model.GetRestApisResult;
import com.amazonaws.services.apigateway.model.GetStagesRequest;
import com.amazonaws.services.apigateway.model.GetStagesResult;
import com.amazonaws.services.apigateway.model.RestApi;
import com.amazonaws.services.apigateway.model.Stage;

public class Apis {
  private static final Logger log = LoggerFactory.getLogger(Apis.class);
  
  private AmazonApiGateway apiGateway;
  private Regions region;
  private HashSet<String> selectedStages = new HashSet<String>();
  
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
    TreeSet<URI> uris = new TreeSet<URI>();
    
    GetRestApisRequest request = new GetRestApisRequest();
    GetRestApisResult apis = apiGateway.getRestApis(request);
    
    for (RestApi api : apis.getItems()) {
      log.trace("ID of API '{}' at {}: {}", api.getName(), region.getName(), api.getId());
      
      uris.addAll(getUrisByApi(api.getId()));
    }
    
    return uris;
  }
  
  private TreeSet<URI> getUrisByApi (String apiId) {
    TreeSet<URI> uris = new TreeSet<URI>();
    
    GetStagesRequest stageRequest = new GetStagesRequest();
    stageRequest.setRestApiId(apiId);
    GetStagesResult stages = apiGateway.getStages(stageRequest);
    
    for (Stage stage : stages.getItem()) {
      log.trace("\tstage: {}", stage.getStageName());
      
      URI uri = null;
      if (selectedStages.size() == 0) {
        uri = getUri(apiId, stage);
      } else {
        if (selectedStages.contains(stage.getStageName())) {
          uri = getUri(apiId, stage);
        }
      }
      
      if (uri != null) {
        uris.add(uri);
      }
    }
    
    return uris;
  }
  
  private URI getUri (String apiId, Stage stage) {
    try {
      return new URIBuilder()
          .setScheme("https")
          .setHost(
              String.format("%s.execute-api.%s.amazonaws.com", apiId, region.getName()))
          .setPath(stage.getStageName())
          .build();
    } catch (URISyntaxException e) {
      log.error("Error occurred when building URI for API '{}' at '{}'.",
          apiId, region.getName(), e);
      return null;
    }
  }
}
