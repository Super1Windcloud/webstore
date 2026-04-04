package org.superwindcloud.runtimeagent.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.superwindcloud.runtimeagent.exception.RuntimeOperationException;

@RestControllerAdvice
public class RuntimeAgentExceptionHandler {

  @ExceptionHandler(RuntimeOperationException.class)
  public ResponseEntity<Map<String, String>> handleRuntime(RuntimeOperationException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("status", "error", "message", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("status", "error", "message", "请求参数无效"));
  }
}
