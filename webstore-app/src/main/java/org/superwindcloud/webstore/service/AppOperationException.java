package org.superwindcloud.webstore.service;

public class AppOperationException extends RuntimeException {

  public AppOperationException(String message) {
    super(message);
  }

  public AppOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
