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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the HttpThrowableReporter.
 */
public class HttpThrowableReporterTest extends GWTTestCase {
  private static final class Payload extends JavaScriptObject {
    protected Payload() {
    }

    public native String getMessage() /*-{
      return this.message;
    }-*/;

    public native JsArrayString getStackTrace() /*-{
      return this.stackTrace;
    }-*/;

    public native String getStrongName() /*-{
      return this.strongName;
    }-*/;
  }

  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  public void testPayload() {
    String payload;
    Throwable e;

    try {
      throw new RuntimeException("foo");
    } catch (Throwable t) {
      e = t;
      payload = HttpThrowableReporter.buildPayload(t);
    }

    assertNotNull(payload);
    Payload p = JsonUtils.unsafeEval(payload);

    assertEquals("foo", p.getMessage());
    assertEquals(GWT.getPermutationStrongName(), p.getStrongName());

    JsArrayString stack = p.getStackTrace();
    assertNotNull(stack);
    assertEquals(e.getStackTrace().length, stack.length());

    for (int i = 0, j = e.getStackTrace().length; i < j; i++) {
      assertEquals(e.getStackTrace()[i].getMethodName(), stack.get(i));
    }
  }
}
