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
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "runtime-agent is healthy", null));
  }

  @PostMapping("/apps/{slug}/install")
  public ResponseEntity<RuntimeCommandResponse> install(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    RuntimeComposeService.ProcessResult result = runtimeComposeService.install(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已安装", result.output()));
  }

  @PostMapping("/apps/{slug}/start")
  public ResponseEntity<RuntimeCommandResponse> start(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    RuntimeComposeService.ProcessResult result = runtimeComposeService.start(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已启动", result.output()));
  }

  @PostMapping("/apps/{slug}/stop")
  public ResponseEntity<RuntimeCommandResponse> stop(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    RuntimeComposeService.ProcessResult result = runtimeComposeService.stop(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已停止", result.output()));
  }

  @DeleteMapping("/apps/{slug}")
  public ResponseEntity<RuntimeCommandResponse> uninstall(
      @PathVariable String slug, @Valid @RequestBody RuntimeCommandRequest request) {
    RuntimeComposeService.ProcessResult result = runtimeComposeService.uninstall(slug, request);
    return ResponseEntity.ok(new RuntimeCommandResponse("ok", "应用已卸载", result.output()));
  }
}
