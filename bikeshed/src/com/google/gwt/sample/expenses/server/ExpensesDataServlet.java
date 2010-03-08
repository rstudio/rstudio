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
package com.google.gwt.sample.expenses.server;

import com.google.gwt.sample.expenses.domain.Employee;
import com.google.gwt.sample.expenses.shared.MethodName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 */
public class ExpensesDataServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);

    MethodName methodName = getMethodName(request.getParameter("methodName"));
    PrintWriter writer = response.getWriter();
    switch (methodName) {
      case FIND_ALL_EMPLOYEES:
        JSONArray jsonArray = new JSONArray();
        for (Employee e : Employee.findAllEmployees()) {
          try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("USER_NAME", e.getUserName());
            jsonObject.put("DISPLAY_NAME", e.getDisplayName());
            jsonArray.put(jsonObject);
          } catch (JSONException ex) {
            System.err.println("Unable to create a JSON object " + ex);
          }
        }
        writer.print(jsonArray.toString());
        break;
      case FIND_EMPLOYEE:
        // TODO
        break;
    }
    writer.flush();
  }

  /**
   * @param request
   * @return
   */
  private MethodName getMethodName(String methodString) {
    for (MethodName method : MethodName.values()) {
      if (method.name().equals(methodString)) {
        return method;
      }
    }
    throw new IllegalArgumentException("unknown methodName: " + methodString);
  }

}
