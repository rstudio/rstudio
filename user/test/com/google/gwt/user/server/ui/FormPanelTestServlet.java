package com.google.gwt.user.server.ui;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FormPanelTestServlet extends HttpServlet {

  protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {

    String query = req.getQueryString();
    if (query != null) {
      // Echo the query string.
      rsp.getWriter().write(query);
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException {

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
