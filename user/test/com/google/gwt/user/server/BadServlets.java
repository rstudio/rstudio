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
package com.google.gwt.user.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Holds servlets which are bad in various ways, used by BadServletsTest, to
 * make sure that JUnitShell keeps going after a bad servlet is specified.
 */
public class BadServlets {

  /**
   * Test an abstract servlet.
   */
  public static abstract class Abstract implements Servlet {
  }

  /**
   * Test a servlet that throws an exception in its constructor.
   */
  public static class CtorException extends HttpServlet {
    public CtorException() {
      super();
      throw new RuntimeException("ctor failed");
    }
  }

  /**
   * Test a servlet that is just an interface.
   */
  public interface Interface extends Servlet {
  }

  /**
   * Test a servlet with no default constructor.
   */
  public static class NoDefaultCtor extends HttpServlet {
    
    public NoDefaultCtor(int bogus) {
      super();
    }
  }

  /**
   * Test a servlet that is not an HttpServlet.
   */
  public static class NotHttpServlet implements Servlet {

    public void destroy() {
    }

    public ServletConfig getServletConfig() {
      return null;
    }

    public String getServletInfo() {
      return null;
    }

    public void init(ServletConfig config) throws ServletException {
    }

    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException {
    }
  }

  /**
   * Ok servlet, to make sure the test setup is correct.
   */
  public static class Ok extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/plain");
      PrintWriter writer = resp.getWriter();
      writer.print("ok");
      writer.flush();
    }
  }

  /**
   * Test a servlet that throws an exception in its static initializer.
   */
  public static class StaticException extends HttpServlet {
    static {
      /**
       * "if (true)" required to avoid compiler error: "initializer must be
       * able to complete normally."
       */
      if (true) {
        throw new ArithmeticException("/ by zero");
      }
    }
  }
}
