package org.superwindcloud.runtimeagent.exception;

public class RuntimeOperationException extends RuntimeException {

  private final String output;

  public RuntimeOperationException(String message) {
    this(message, null, null);
  }

  public RuntimeOperationException(String message, String output) {
    this(message, output, null);
  }

  public RuntimeOperationException(String message, Throwable cause) {
    this(message, null, cause);
  }

  public RuntimeOperationException(String message, String output, Throwable cause) {
    super(message, cause);
    this.output = output;
  }

  public String getOutput() {
    return output;
  }
}
