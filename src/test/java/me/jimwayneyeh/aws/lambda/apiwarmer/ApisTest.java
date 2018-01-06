package me.jimwayneyeh.aws.lambda.apiwarmer;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.regions.Region;

public class ApisTest {
  @Test
  public void test () {
    new Apis(Region.US_WEST_2).getUris();
  }
}
