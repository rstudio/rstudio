/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.server.testing;

import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.server.MessageCatalogFactory.Context;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Mock {@link Context} for testing.  Throws exceptions on any warning or error
 * message, and writes any output to the supplied {@link ByteArrayOutputStream}.
 */
public class MockMessageCatalogContext implements Context {
  private final ByteArrayOutputStream baos;

  private GwtLocaleFactory factory = new GwtLocaleFactoryImpl();

  public MockMessageCatalogContext(ByteArrayOutputStream baos) {
    this.baos = baos;
  }

  public OutputStream createBinaryFile(String catalogName) {
    return baos;
  }

  public PrintWriter createTextFile(String catalogName, String charSet) {
    try {
      return new PrintWriter(new BufferedWriter(
          new OutputStreamWriter(baos, "UTF-8")), false);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("io error", e);
    }
  }

  public void error(String msg) {
    throw new RuntimeException("warning: " + msg);
  }

  public void error(String msg, Throwable cause) {
    throw new RuntimeException("warning: " + msg, cause);
  }

  public GwtLocaleFactory getLocaleFactory() {
    return factory;
  }

  public void warning(String msg) {
    throw new RuntimeException("warning: " + msg);
  }

  public void warning(String msg, Throwable cause) {
    throw new RuntimeException("warning: " + msg, cause);
  }
}