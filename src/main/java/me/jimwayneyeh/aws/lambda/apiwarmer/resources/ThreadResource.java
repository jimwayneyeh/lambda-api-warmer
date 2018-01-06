package me.jimwayneyeh.aws.lambda.apiwarmer.resources;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadResource {
  private static final Logger LOGGER = 
      LoggerFactory.getLogger(ThreadResource.class);
  
  private static ScheduledExecutorService pool = null;
  
  static {
    initiateThreadPool();
  }
  
  public static synchronized void initiateThreadPool () {
    int numberInPool = 5;
    String poolStr = System.getenv("threadpool");
    if (StringUtils.isNotBlank(poolStr) && StringUtils.isNumeric(poolStr)) {
      numberInPool = Integer.parseInt(poolStr);
    }
    
    LOGGER.info("Initiate execution pool with {} threads.", numberInPool);
    pool = Executors.newScheduledThreadPool(numberInPool);
  }
  
  public static ScheduledExecutorService getPool () {
    return pool;
  }
}