package com.shinchoku.shinchokuapp.api;

import com.shinchoku.shinchokuapp.entity.enums.WireValue;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.*;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.*;

/** Registers contract validation and the lowercase enum values used by query parameters. */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

  private final ApiRequestValidation validation;

  public WebConfiguration(ApiRequestValidation validation) {
    this.validation = validation;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(validation);
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(
        new ConverterFactory<String, Enum>() {

          public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return value -> {
              for (T candidate : targetType.getEnumConstants()) {
                if (candidate instanceof WireValue wire && wire.value().equals(value)) {
                  return candidate;
                }
              }
              throw new IllegalArgumentException("Unsupported enum value");
            };
          }
        });
  }
}
