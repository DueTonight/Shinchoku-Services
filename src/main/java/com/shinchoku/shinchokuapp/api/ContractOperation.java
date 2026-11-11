package com.shinchoku.shinchokuapp.api;

import java.lang.annotation.*;

/** Links a typed controller method to its existing OpenAPI validation rules. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContractOperation {

  String value();
}
