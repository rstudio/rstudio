package com.google.gwt.http.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResponseTestServlet extends HttpServlet {

  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.addHeader("header1", "value1");
    response.addHeader("header2", "value2");
    response.addHeader("header3", "value3");
    
    /* 
     * If the ends with noResponseText then do not send it.  This is used to
     * test some Safari specific anomalies.
     */
    if (!request.getPathInfo().endsWith("noResponseText")) {
      response.getWriter().print("Hello World!");
    }

    response.setStatus(HttpServletResponse.SC_OK);
  }
}
