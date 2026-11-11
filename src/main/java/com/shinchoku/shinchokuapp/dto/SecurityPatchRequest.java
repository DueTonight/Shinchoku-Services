package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/**
 * Typed PATCH payload. Typed presence checks such as hasAssigneeId() distinguish omission from
 * explicit null.
 */
public class SecurityPatchRequest extends PatchRequest {

  private Boolean requireMfa;

  private Integer sessionTimeoutMinutes;

  private List<String> allowedEmailDomains;

  public Boolean getRequireMfa() {
    return requireMfa;
  }

  @JsonSetter("requireMfa")
  public void setRequireMfa(Boolean requireMfa) {
    this.requireMfa = requireMfa;
    supplied("requireMfa");
  }

  public Integer getSessionTimeoutMinutes() {
    return sessionTimeoutMinutes;
  }

  @JsonSetter("sessionTimeoutMinutes")
  public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
    this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    supplied("sessionTimeoutMinutes");
  }

  public List<String> getAllowedEmailDomains() {
    return allowedEmailDomains;
  }

  @JsonSetter("allowedEmailDomains")
  public void setAllowedEmailDomains(List<String> allowedEmailDomains) {
    this.allowedEmailDomains = allowedEmailDomains;
    supplied("allowedEmailDomains");
  }

  @JsonIgnore
  public boolean hasRequireMfa() {
    return has("requireMfa");
  }

  @JsonIgnore
  public boolean hasSessionTimeoutMinutes() {
    return has("sessionTimeoutMinutes");
  }

  @JsonIgnore
  public boolean hasAllowedEmailDomains() {
    return has("allowedEmailDomains");
  }
}
