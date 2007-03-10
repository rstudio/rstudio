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
package com.google.gwt.http.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: document me.
 */
public class ResponseTestServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.addHeader("header1", "value1");
    response.addHeader("header2", "value2");
    response.addHeader("header3", "value3");

    /*
     * If the ends with noResponseText then do not send it. This is used to test
     * some Safari specific anomalies.
     */
    if (!request.getPathInfo().endsWith("noResponseText")) {
      response.getWriter().print("Hello World!");
    }

    response.setStatus(HttpServletResponse.SC_OK);
  }
}
