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

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet component of the
 * {@link com.google.gwt.http.client.RequestBuilderTest RequestBuilderTest}.
 */
public class RequestBuilderTestServlet extends HttpServlet {

  private static String getPathInfoBase() {
    return "/com.google.gwt.http.RequestBuilderTest.JUnit/testRequestBuilder/";
  }

  @Override
  protected void doDelete(HttpServletRequest request,
      HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String method = request.getMethod();
    String pathInfo = request.getPathInfo();
    if (pathInfo.equals(getPathInfoBase() + "setRequestHeader")) {
      String value = request.getHeader("Foo");
      response.getWriter().print("Hello");
      if (value.equals("Bar1")) {
        response.setStatus(HttpServletResponse.SC_OK);
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
    } else if (pathInfo.equals(getPathInfoBase() + "sendRequest_GET")) {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write("<html><body>hello</body></html>");
      response.setContentType("text/html");
    } else if (pathInfo.equals(getPathInfoBase() + "setTimeout/timeout")) {
      // cause a timeout on the client
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      response.setStatus(HttpServletResponse.SC_OK);
    } else if (pathInfo.equals(getPathInfoBase() + "setTimeout/noTimeout")) {
      // wait but not long enough to timeout
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      response.getWriter().print("setTimeout/noTimeout");
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Override
  protected void doHead(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    String parameter = request.getParameter("method");
    if ("test request".equals(parameter)) {
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    BufferedReader reader = request.getReader();
    String content = reader.readLine();
    if (content.equals("<html><body>Put Me</body></html>")) {
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
