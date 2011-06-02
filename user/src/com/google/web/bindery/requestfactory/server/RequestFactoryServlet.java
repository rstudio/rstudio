/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.requestfactory.server;

import com.google.gwt.user.server.rpc.RPCServletUtils;
import com.google.web.bindery.requestfactory.shared.RequestFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles GWT RequestFactory JSON requests.
 */
@SuppressWarnings("serial")
public class RequestFactoryServlet extends HttpServlet {

  private static final boolean DUMP_PAYLOAD = Boolean.getBoolean("gwt.rpc.dumpPayload");
  private static final String JSON_CHARSET = "UTF-8";
  private static final String JSON_CONTENT_TYPE = "application/json";
  private static final Logger log = Logger
      .getLogger(RequestFactoryServlet.class.getCanonicalName());

  /**
   * These ThreadLocals are used to allow service objects to obtain access to
   * the HTTP transaction.
   */
  private static final ThreadLocal<ServletContext> perThreadContext =
      new ThreadLocal<ServletContext>();
  private static final ThreadLocal<HttpServletRequest> perThreadRequest =
      new ThreadLocal<HttpServletRequest>();
  private static final ThreadLocal<HttpServletResponse> perThreadResponse =
      new ThreadLocal<HttpServletResponse>();

  /**
   * Returns the thread-local {@link HttpServletRequest}.
   * 
   * @return an {@link HttpServletRequest} instance
   */
  public static HttpServletRequest getThreadLocalRequest() {
    return perThreadRequest.get();
  }

  /**
   * Returns the thread-local {@link HttpServletResponse}.
   * 
   * @return an {@link HttpServletResponse} instance
   */
  public static HttpServletResponse getThreadLocalResponse() {
    return perThreadResponse.get();
  }

  /**
   * Returns the thread-local {@link ServletContext}
   * 
   * @return the {@link ServletContext} associated with this servlet
   */
  public static ServletContext getThreadLocalServletContext() {
    return perThreadContext.get();
  }

  private final SimpleRequestProcessor processor;

  /**
   * Constructs a new {@link RequestFactoryServlet} with a
   * {@code DefaultExceptionHandler}.
   */
  public RequestFactoryServlet() {
    this(new DefaultExceptionHandler());
  }

  /**
   * Use this constructor in subclasses to provide a custom
   * {@link ExceptionHandler}.
   * 
   * @param exceptionHandler an {@link ExceptionHandler} instance
   * @param serviceDecorators an array of ServiceLayerDecorators that change how
   *          the RequestFactory request processor interact with the domain
   *          objects
   */
  public RequestFactoryServlet(ExceptionHandler exceptionHandler,
      ServiceLayerDecorator... serviceDecorators) {
    processor = new SimpleRequestProcessor(ServiceLayer.create(serviceDecorators));
    processor.setExceptionHandler(exceptionHandler);
  }

  /**
   * Processes a POST to the server.
   * 
   * @param request an {@link HttpServletRequest} instance
   * @param response an {@link HttpServletResponse} instance
   * @throws IOException if an internal I/O error occurs
   * @throws ServletException if an error occurs in the servlet
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    perThreadContext.set(getServletContext());
    perThreadRequest.set(request);
    perThreadResponse.set(response);

    // No new code should be placed outside of this try block.
    try {
      ensureConfig();
      String jsonRequestString =
          RPCServletUtils.readContent(request, JSON_CONTENT_TYPE, JSON_CHARSET);
      if (DUMP_PAYLOAD) {
        System.out.println(">>> " + jsonRequestString);
      }

      try {
        String payload = processor.process(jsonRequestString);
        if (DUMP_PAYLOAD) {
          System.out.println("<<< " + payload);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(RequestFactory.JSON_CONTENT_TYPE_UTF8);
        // The Writer must be obtained after setting the content type
        PrintWriter writer = response.getWriter();
        writer.print(payload);
        writer.flush();
      } catch (RuntimeException e) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        log.log(Level.SEVERE, "Unexpected error", e);
      }
    } finally {
      perThreadContext.set(null);
      perThreadRequest.set(null);
      perThreadResponse.set(null);
    }
  }

  private void ensureConfig() {
    String symbolMapsDirectory = getServletConfig().getInitParameter("symbolMapsDirectory");
    if (symbolMapsDirectory != null) {
      Logging.setSymbolMapsDirectory(symbolMapsDirectory);
    }
  }
}
