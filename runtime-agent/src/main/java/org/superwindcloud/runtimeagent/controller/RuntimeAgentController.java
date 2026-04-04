package org.superwindcloud.runtimeagent.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.superwindcloud.runtimeagent.dto.RuntimeCommandRequest;
import org.superwindcloud.runtimeagent.dto.RuntimeCommandResponse;
import org.superwindcloud.runtimeagent.service.RuntimeComposeService;

@RestController
@RequestMapping("/api")
public class RuntimeAgentController {

  private final RuntimeComposeService runtimeComposeService;

  public RuntimeAgentController(RuntimeComposeService runtimeComposeService) {
    this.runtimeComposeService = runtimeComposeService;
  }

  @GetMapping("/health")
  public ResponseEntity<RuntimeCommandResponse> health() {
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "runtime-agent is healthy"));
  }

  @PostMapping("/apps/{slug}/install")
  public ResponseEntity<RuntimeCommandResponse> install(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    runtimeComposeService.install(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已安装"));
  }

  @PostMapping("/apps/{slug}/start")
  public ResponseEntity<RuntimeCommandResponse> start(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    runtimeComposeService.start(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已启动"));
  }

  @PostMapping("/apps/{slug}/stop")
  public ResponseEntity<RuntimeCommandResponse> stop(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    runtimeComposeService.stop(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已停止"));
  }

  @DeleteMapping("/apps/{slug}")
  public ResponseEntity<RuntimeCommandResponse> uninstall(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    runtimeComposeService.uninstall(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已卸载"));
  }
}
