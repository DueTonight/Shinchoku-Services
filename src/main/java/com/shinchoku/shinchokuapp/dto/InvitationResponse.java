package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Public API output; persistence entities and credential hashes are never serialized. */
public record InvitationResponse(
    UUID id,
    String email,
    WorkspaceRole role,
    Instant expiresAt,
    Instant acceptedAt,
    Instant revokedAt) {}
