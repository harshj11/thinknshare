package com.app.thinknshare.user.exception;

public class RefreshException extends RuntimeException {
  public final String code;
  public RefreshException(String code, String msg) { super(msg); this.code = code; }

  public String getCode() { return code; }
}
