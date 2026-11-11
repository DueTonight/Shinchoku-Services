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
public class ProfilePatchRequest extends PatchRequest {

  private String displayName;

  private String avatarUrl;

  private String timezone;

  private Theme theme;

  public String getDisplayName() {
    return displayName;
  }

  @JsonSetter("displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
    supplied("displayName");
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  @JsonSetter("avatarUrl")
  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
    supplied("avatarUrl");
  }

  public String getTimezone() {
    return timezone;
  }

  @JsonSetter("timezone")
  public void setTimezone(String timezone) {
    this.timezone = timezone;
    supplied("timezone");
  }

  public Theme getTheme() {
    return theme;
  }

  @JsonSetter("theme")
  public void setTheme(Theme theme) {
    this.theme = theme;
    supplied("theme");
  }

  @JsonIgnore
  public boolean hasDisplayName() {
    return has("displayName");
  }

  @JsonIgnore
  public boolean hasAvatarUrl() {
    return has("avatarUrl");
  }

  @JsonIgnore
  public boolean hasTimezone() {
    return has("timezone");
  }

  @JsonIgnore
  public boolean hasTheme() {
    return has("theme");
  }
}
