/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.server.rpc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: document me.
 */
public class RemoteServiceServletTestServiceImpl extends
    RemoteServiceServletTestServiceImplBase {
  /**
   * Explicitly return a 404 for the "404" URL.
   */
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (req.getPathInfo() != null && req.getPathInfo().endsWith("404")) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND,
          "Intentionally not found URL");
      return;
    }
    super.service(req, resp);
  }

}
