package org.superwindcloud.webstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegistrationForm {

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 32, message = "Username must be 3-32 characters")
  private String username;

  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 72, message = "Password must be 6-72 characters")
  private String password;
}
