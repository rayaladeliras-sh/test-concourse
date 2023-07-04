package com.stubhub.identity.token.service.exception;

import java.util.function.Supplier;

public class ExceptionSupplier<T extends Throwable> implements Supplier<T> {
  private T t;

  public ExceptionSupplier(T t) {
    this.t = t;
  }

  @Override
  public T get() {
    return t;
  }
}
