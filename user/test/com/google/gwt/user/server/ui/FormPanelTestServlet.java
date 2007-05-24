/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.server.ui;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for use with {@link FormPanelTest}.
 */
public class FormPanelTestServlet extends HttpServlet {

  protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {

    rsp.setContentType("text/html");

    String query = req.getQueryString();
    if (query != null) {
      // Echo the query string.
      rsp.getWriter().write(query);
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {

    rsp.setContentType("text/html");

    String query = req.getQueryString();
    if (query != null) {
      if (query.equals("sendHappyHtml")) {
        rsp.getWriter().write("<html><body><div id=':)'></div></body></html>");
        return;
      }
    }

    // Read the request content.
    BufferedReader reader = req.getReader();
    char[] buf = new char[req.getContentLength()];
    reader.read(buf, 0, req.getContentLength());

    // Echo the request content.
    rsp.getWriter().write(buf);
  }
}
