package com.stubhub.identity.token.service.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;

public class ExtendedErrorException extends ResponseStatusException {

  private static final long serialVersionUID = 8539939407704576713L;

  private String error;

  private Map<String, Object> data;

  public ExtendedErrorException(
      @NonNull HttpStatus status,
      @Nullable String error,
      @Nullable String reason,
      @Nullable Throwable cause,
      Map<String, Object> data) {
    super(status, reason, cause);
    this.error = error;
    this.data = data;
  }

  public static Builder create(HttpStatus status) {
    Assert.notNull(status, "status can't be null");
    return new Builder().status(status);
  }

  public static class Builder {
    private HttpStatus status;
    private String error;
    private String reason;
    private Throwable cause;
    private Map<String, Object> data;

    public ExtendedErrorException build() {
      return new ExtendedErrorException(status, error, reason, cause, data);
    }

    public Builder status(HttpStatus status) {
      this.status = status;
      return this;
    }

    public Builder error(String error) {
      this.error = error;
      return this;
    }

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder cause(Throwable cause) {
      this.cause = cause;
      return this;
    }

    public Builder putData(String key, Object value) {
      if (value == null) {
        return this;
      }
      if (data == null) {
        data = new HashMap<>();
      }
      data.put(key, value);
      return this;
    }

    public Builder data(Map<String, Object> data) {
      this.data = data;
      return this;
    }
  }

  public String getError() {
    return error;
  }

  public Map<String, Object> getData() {
    return data;
  }
}
