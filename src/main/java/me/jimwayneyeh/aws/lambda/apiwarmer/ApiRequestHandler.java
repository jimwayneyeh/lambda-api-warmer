package me.jimwayneyeh.aws.lambda.apiwarmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class ApiRequestHandler implements RequestStreamHandler {

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    // TODO Auto-generated method stub
    
  }
}
