package com.shinchoku.shinchokuapp.dto;

import com.fasterxml.jackson.annotation.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

/** Validated API input. */
public record CycleCreateRequest(
    @NotNull Integer number, @NotNull LocalDate startsOn, @NotNull LocalDate endsOn) {}
