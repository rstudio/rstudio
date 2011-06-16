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

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.servlet.GzipFilter;

/**
 * A utility class for hosting an instance of {@link RequestFactoryServlet}.
 */
public class RequestFactoryTestServer {
  public static void main(String[] args) {
    Server server = new Server();

    SocketConnector connector = new SocketConnector();
    connector.setPort(9999);
    server.addConnector(connector);

    Context ctx = new Context();
    ctx.addServlet(RequestFactoryServlet.class, "/gwtRequest");
    ctx.addFilter(GzipFilter.class, "*", Handler.DEFAULT);
    SessionHandler sessionHandler = new SessionHandler();
    sessionHandler.getSessionManager().setMaxInactiveInterval(5);
    ctx.setSessionHandler(sessionHandler);
    server.addHandler(ctx);

    try {
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }
}
