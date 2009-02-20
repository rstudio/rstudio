/*
 * Copyright 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.client;

import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * This is a utility class which can report Throwables to the server via a
 * JSON-formatted payload.
 */
public final class HttpThrowableReporter {

  private static class MyHandler implements UncaughtExceptionHandler {
    private final String url;

    public MyHandler(String url) {
      this.url = url;
    }

    public void onUncaughtException(Throwable e) {
      report(url, e);
    }
  }

  /**
   * Installs an {@link UncaughtExceptionHandler} that will automatically invoke
   * {@link #report(String, Throwable)}.
   * 
   * @param url A URL that is suitable for use with {@link XMLHttpRequest}
   */
  public static void installUncaughtExceptionHandler(String url) {
    GWT.setUncaughtExceptionHandler(new MyHandler(url));
  }

  /**
   * Report a Throwable to the server. This method will sent an HTTP
   * <code>POST</code> request with a JSON-formatted payload. The payload will
   * consist of a single JSON object with the following keys:
   * <dl>
   * <dt>strongName</dt>
   * <dd>The result of calling {@link GWT#getPermutationStrongName()}</dd>
   * <dt>message</dt>
   * <dd>The result of calling {@link Throwable#getMessage()}</dd>
   * <dt>stackTrace</dt>
   * <dd>A list of the methods names in the Throwable's stack trace, derived
   * from {@link StackTraceElement#getMethodName()}.</dd>
   * </dl>
   * 
   * The response from the server is ignored.
   * 
   * @param url A URL that is suitable for use with {@link XMLHttpRequest}
   * @param t The Throwable to report
   * @return <code>true</code> if the request was successfully initiated
   * @see com.google.gwt.core.linker.SymbolMapsLinker
   */
  public static boolean report(String url, Throwable t) {
    try {
      XMLHttpRequest xhr = XMLHttpRequest.create();
      xhr.open("POST", url);
      xhr.send(buildPayload(t));
      return true;
    } catch (Throwable t2) {
      return false;
    }
  }

  /**
   * Visible for testing.
   */
  static String buildPayload(Throwable t) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"strongName\" : ");
    sb.append(JsonUtils.escapeValue(GWT.getPermutationStrongName()));
    sb.append(",\"message\" : ");
    sb.append(JsonUtils.escapeValue(t.getMessage()));

    sb.append(",\"stackTrace\" : [");
    boolean needsComma = false;
    for (StackTraceElement e : t.getStackTrace()) {
      if (needsComma) {
        sb.append(",");
      } else {
        needsComma = true;
      }

      sb.append(JsonUtils.escapeValue(e.getMethodName()));
    }
    sb.append("]}");

    return sb.toString();
  }

  private HttpThrowableReporter() {
  }
}
