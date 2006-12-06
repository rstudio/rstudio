package com.google.gwt.http.server;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestBuilderTestServlet extends HttpServlet {

  private static String getPathInfoBase() {
    return "/com.google.gwt.http.RequestBuilderTest/testRequestBuilder/";
  }
  
  protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_OK);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
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
  
  protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_OK);
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    BufferedReader reader = request.getReader();
    String content = reader.readLine();
    response.getWriter().print("POST");
    if (content.equals("method=test+request")) {
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    BufferedReader reader = request.getReader();
    String content = reader.readLine();
    if (content.equals("<html><body>Put Me</body></html>")) {
      response.setStatus(HttpServletResponse.SC_OK);
    } else {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  private int count;
}
