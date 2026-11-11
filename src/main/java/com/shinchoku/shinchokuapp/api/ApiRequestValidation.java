package com.shinchoku.shinchokuapp.api;

import com.shinchoku.shinchokuapp.security.Caller;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.databind.ObjectMapper;

/** Schema maps are confined to validation metadata; controllers and services receive typed DTOs. */
@ControllerAdvice
@SuppressWarnings("unchecked")
public class ApiRequestValidation extends RequestBodyAdviceAdapter implements HandlerInterceptor {

  private final Contract contract;

  private final ObjectMapper objectMapper;

  private final Map<String, Map<String, Object>> operations = new HashMap<>();

  public ApiRequestValidation(Contract contract, ObjectMapper objectMapper) {
    this.contract = contract;
    this.objectMapper = objectMapper;
    ((Map<String, Map<String, Map<String, Object>>>) contract.document().get("paths"))
        .values()
        .forEach(
            methods ->
                methods
                    .values()
                    .forEach(
                        operation ->
                            operations.put(operation.get("operationId").toString(), operation)));
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(handler instanceof HandlerMethod method)) {
      return true;
    }
    ContractOperation annotation = method.getMethodAnnotation(ContractOperation.class);
    if (annotation == null) {
      return true;
    }
    Map<String, Object> operation = operations.get(annotation.value());
    Caller caller = Caller.current();
    caller.requireScope(Set.of("GET", "HEAD").contains(request.getMethod()) ? "read" : "write");
    if (operation.containsKey("security")) {
      caller.requireSession();
    }
    Map<String, String> pathValues =
        (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    Set<String> allowedQueryNames = new HashSet<>();
    for (Map<String, Object> parameter :
        (List<Map<String, Object>>) operation.getOrDefault("parameters", List.of())) {
      String name = parameter.get("name").toString();
      Map<String, Object> schema = (Map<String, Object>) parameter.get("schema");
      if (parameter.get("in").equals("path")) {
        contract.validateSchema(schema, pathValues.get(name), name);
        continue;
      }
      allowedQueryNames.add(name);
      Object value = schema.get("default");
      String[] supplied = request.getParameterValues(name);
      if (supplied != null) {
        try {
          value =
              switch (Objects.toString(schema.get("type"), "")) {
                case "array" -> {
                  List<String> values = new ArrayList<>();
                  for (String item : supplied) {
                    if (!item.isEmpty()) {
                      values.addAll(Arrays.asList(item.split(",", -1)));
                    }
                  }
                  yield values;
                }
                case "integer" -> Integer.valueOf(supplied[0]);
                case "boolean" -> {
                  if (!Set.of("true", "false").contains(supplied[0])) {
                    throw new IllegalArgumentException();
                  }
                  yield Boolean.valueOf(supplied[0]);
                }
                default -> supplied[0];
              };
        } catch (Exception exception) {
          throw new ApiException(400, "INVALID_PARAMETER", "Invalid query parameter: " + name);
        }
      }
      if (value == null && Boolean.TRUE.equals(parameter.get("required"))) {
        throw new ApiException(400, "MISSING_PARAMETER", "Missing query parameter: " + name);
      }
      if (value != null) {
        contract.validateSchema(schema, value, name);
      }
    }
    for (String name : request.getParameterMap().keySet()) {
      if (!allowedQueryNames.contains(name)) {
        throw new ApiException(400, "UNKNOWN_PARAMETER", "Unknown query parameter: " + name);
      }
    }
    if (!operation.containsKey("requestBody") && request.getContentLengthLong() > 0) {
      Object body = readJson(request.getInputStream().readAllBytes());
      if (!(body instanceof Map<?, ?> fields) || !fields.isEmpty()) {
        throw new ApiException(
            400, "UNEXPECTED_BODY", "This endpoint does not accept a request body");
      }
    }
    return true;
  }

  @Override
  public boolean supports(
      MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
    return parameter.getMethodAnnotation(ContractOperation.class) != null;
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage message,
      MethodParameter parameter,
      Type type,
      Class<? extends HttpMessageConverter<?>> converter)
      throws IOException {
    byte[] bytes = message.getBody().readAllBytes();
    Map<String, Object> operation =
        operations.get(parameter.getMethodAnnotation(ContractOperation.class).value());
    Map<String, Object> body = (Map<String, Object>) operation.get("requestBody");
    Map<String, Object> media =
        (Map<String, Object>) ((Map<?, ?>) body.get("content")).get("application/json");
    contract.validateSchema((Map<String, Object>) media.get("schema"), readJson(bytes), "body");
    return new HttpInputMessage() {

      public HttpHeaders getHeaders() {
        return message.getHeaders();
      }

      public InputStream getBody() {
        return new ByteArrayInputStream(bytes);
      }
    };
  }

  private Object readJson(byte[] bytes) {
    try {
      return objectMapper.readValue(bytes, Object.class);
    } catch (Exception exception) {
      throw new ApiException(400, "BAD_REQUEST", "Malformed request");
    }
  }
}
