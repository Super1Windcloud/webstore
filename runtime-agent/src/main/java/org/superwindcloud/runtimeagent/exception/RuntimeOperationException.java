package org.superwindcloud.runtimeagent.exception;

public class RuntimeOperationException extends RuntimeException {

  public RuntimeOperationException(String message) {
    super(message);
  }

  public RuntimeOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
