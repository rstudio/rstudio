/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.http.client.RequestBuilderTest;

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

  @Override
  protected void doDelete(HttpServletRequest request,
      HttpServletResponse response) {
    try {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().print(RequestBuilderTest.SERVLET_DELETE_RESPONSE);
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String pathInfo = request.getPathInfo();
    if (pathInfo.equals("/setRequestHeader")) {
      String value = request.getHeader("Foo");
      if (value.equals("Bar1")) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(RequestBuilderTest.SERVLET_GET_RESPONSE);
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
    } else if (pathInfo.equals("/send_GET")) {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(RequestBuilderTest.SERVLET_GET_RESPONSE);
    } else if (pathInfo.equals("/sendRequest_GET")) {
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write(RequestBuilderTest.SERVLET_GET_RESPONSE);
    } else if (pathInfo.equals("/setTimeout/timeout")) {
      // cause a timeout on the client
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
      }
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().print(RequestBuilderTest.SERVLET_GET_RESPONSE);
    } else if (pathInfo.equals("/setTimeout/noTimeout")) {
      // wait but not long enough to timeout
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().print(RequestBuilderTest.SERVLET_GET_RESPONSE);
    } else if (pathInfo.equals("/user/pass")) {
      String auth = request.getHeader("Authorization");
      if (auth == null) {
        response.setHeader("WWW-Authenticate", "BASIC");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      } else {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(RequestBuilderTest.SERVLET_GET_RESPONSE);
      }
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
    try {
      String parameter = request.getParameter("method");
      if ("test request".equals(parameter)) {
        /*
         * On Safari 2.0.4, if the HTTP response does not include any response
         * text the status message will be undefined. So, we make sure that the
         * post returns some data. See
         * http://bugs.webkit.org/show_bug.cgi?id=3810.
         */
        response.getWriter().print(RequestBuilderTest.SERVLET_POST_RESPONSE);
        response.setStatus(HttpServletResponse.SC_OK);
      } else if (request.getPathInfo().equals("/simplePost")) {
        response.getWriter().print(RequestBuilderTest.SERVLET_POST_RESPONSE);
        response.setStatus(HttpServletResponse.SC_OK);
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    BufferedReader reader = request.getReader();
    String content = reader.readLine();
    if (content != null && content.equals("<html><body>Put Me</body></html>")) {
      response.getWriter().print(RequestBuilderTest.SERVLET_PUT_RESPONSE);
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
