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
package com.google.gwt.sample.expenses.gwt.ui.employee;

import com.google.gwt.sample.expenses.gwt.request.EmployeeKey;
import com.google.gwt.valuestore.shared.Values;

/**
 * Renders report details to HTML.
 */
public class EmployeeDetailsBuilder {

  public void append(StringBuilder list, Values<EmployeeKey> values) {
    String user = values.get(EmployeeKey.get().getUserName());
    list.append("<div>");
    list.append("<label>").append("User Name: ").append("</label>");
    list.append("<span>").append(user).append("</span>");
    list.append("</div>");

    list.append("<div>");
    String display = values.get(EmployeeKey.get().getDisplayName());
    list.append("<label>").append("Display Name: ").append("</label>");
    list.append("<span>").append(display).append("</span>");
    list.append("</div>");
  }
}
