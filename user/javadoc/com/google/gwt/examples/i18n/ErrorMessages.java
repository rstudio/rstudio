package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Messages;

public interface ErrorMessages extends Messages {
  String permissionDenied(int errorCode, String username);
}
