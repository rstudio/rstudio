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
package com.google.gwt.jsonp.server;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that returns a given value, following the JSONP protocol.
 * Expected url parameters:
 *
 * <ul>
 *   <li>action: one of the following values:
 *     <ul>
 *       <li>TIMEOUT: don't respond anything to simulate a timeout
 *       <li>SUCCESS: return a JSON value
 *       <li>FAILURE: return an error
 *     </ul>
 *   <li>value: the JSON value to return if action == "SUCCESS"
 *   <li>error: the error message to return if action == "FAILURE"
 * </ul>
 */
public class EchoServlet extends HttpServlet {

  private enum Action {
    SUCCESS,
    FAILURE,
    TIMEOUT
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse res)
      throws IOException {
    res.setContentType("application/javascript");
    switch (Action.valueOf(req.getParameter("action"))) {
      case SUCCESS: {
        handleDelay(req);
        String callback = req.getParameter("callback");
        String value = req.getParameter("value");
        if (value == null) {
          value = "";
        }
        res.getWriter().println(callback + "(" + value + ");");
        res.getWriter().flush();
        break;
      }

      case FAILURE: {
        handleDelay(req);
        String failureCallback = req.getParameter("failureCallback");
        String error = req.getParameter("error");
        if (failureCallback != null) {
          res.getWriter().println(failureCallback + "('" + error + "');");
          res.getWriter().flush();
        } else {
          // If no failure callback is defined, send the error through the
          // success callback.
          String callback = req.getParameter("callback");
          res.getWriter().println(callback + "('" + error + "');");
          res.getWriter().flush();
        }
        break;
      }

      case TIMEOUT:
        // Don't respond anything so that a timeout happens.
    }
  }

  /**
   * Handle a delay query parameter if present.
   * 
   * @param req
   */
  private void handleDelay(HttpServletRequest req) {
    String delayVal = req.getParameter("delay");
    if (delayVal != null) {
      int delayMs = Integer.parseInt(delayVal);
      try {
        Thread.sleep(delayMs);
      } catch (InterruptedException e) {
        log("Interrupted sleep, continuing");
      }
    }
  }
}
