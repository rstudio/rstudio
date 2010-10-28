/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.module.server;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that returns files in the WEB-INF/deploy directory.
 */
public class DeployServlet extends HttpServlet {

  @Override
  public void init() throws ServletException {
    getServletContext().log("DeployServlet initialized");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    String path = req.getQueryString();
    ServletContext context = getServletContext();
    context.log("DeployServlet get: " + path);
    InputStream istr = null;
    ServletOutputStream ostr = null;
    try {
      istr = context.getResourceAsStream("/WEB-INF/deploy/" + path);
      if (istr == null) {
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      res.setStatus(HttpServletResponse.SC_OK);
      res.setContentType("text/plain");
      ostr = res.getOutputStream();
      byte[] buf = new byte[4096];
      int n;
      while ((n = istr.read(buf)) >= 0) {
        ostr.write(buf, 0, n);
      }
    } finally {
      if (istr != null) {
        istr.close();
      }
      if (ostr != null) {
        ostr.flush();
      }
    }
  }
}
