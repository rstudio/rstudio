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
package com.google.web.bindery.requestfactory.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlets.GzipFilter;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

/**
 * A utility class for hosting an instance of {@link RequestFactoryServlet}.
 */
public class RequestFactoryTestServer {
  public static void main(String[] args) {
    Server server = new Server();

    ServerConnector connector = new ServerConnector(server);
    connector.setPort(9999);
    server.addConnector(connector);

    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    handler.setContextPath("/");
    handler.addServlet(RequestFactoryServlet.class, "/gwtRequest");
    handler.addFilter(GzipFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

    SessionHandler sessionHandler = new SessionHandler();
    sessionHandler.getSessionManager().setMaxInactiveInterval(5);
    handler.setSessionHandler(sessionHandler);
    server.setHandler(handler);

    try {
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
