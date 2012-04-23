/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.server;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.shared.Localizable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that returns a localized string based on the inferred locale.
 */
public class TestServlet extends GwtServletBase {

  /**
   * Test interface for server-side localization.
   */
  public interface Test extends Localizable {
    String result();
  }

  /**
   * Implementation for the default locale.
   */
  public static class Test_ implements Test {
    @Override
    public String result() {
      return "default";
    }
  }

  /**
   * Implementation for the de locale.
   */
  public static class Test_de implements Test {
    @Override
    public String result() {
      return "de";
    }
  }

  /**
   * Implementation for the en locale.
   */
  public static class Test_en implements Test {
    @Override
    public String result() {
      return "en";
    }
  }

  /**
   * Implementation for the en_US locale.
   */
  public static class Test_en_US implements Test {
    @Override
    public String result() {
      return "en_US";
    }
  }

  /**
   * Implementation for the fr locale.
   */
  public static class Test_fr implements Test {
    @Override
    public String result() {
      return "fr";
    }
  }

  @Override
  public void init() throws ServletException {
    localeCookie = "LOCALE";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    resp.setContentType("text/plain");
    Test t = GWT.create(Test.class);
    PrintWriter writer = resp.getWriter();
    writer.print(t.result());
    writer.close();
  }

  // POST is only used to set the locale cookie for the next GET
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    BufferedReader reader = req.getReader();
    String locale = reader.readLine();
    reader.close();
    resp.setHeader("Set-Cookie", "LOCALE=" + locale);
    resp.setContentType("text/plain");
    PrintWriter writer = resp.getWriter();
    writer.close();
  }
}
