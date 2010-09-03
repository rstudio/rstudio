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
package com.google.gwt.sample.expenses.client.ui.employee;

import com.google.gwt.app.place.ProxyRenderer;
import com.google.gwt.sample.expenses.client.request.EmployeeProxy;

/**
 * Renders {@link EmployeeProxy}s for display to the user. Requires the
 * displayName property to have been fetched.
 */
public class EmployeeRenderer extends ProxyRenderer<EmployeeProxy> {
  private static EmployeeRenderer INSTANCE;

  public static EmployeeRenderer instance() {
    if (INSTANCE == null) {
      INSTANCE = new EmployeeRenderer();
    }

    return INSTANCE;
  }

  protected EmployeeRenderer() {
    super(new String[] { "displayName"} );
  }

  public String render(EmployeeProxy object) {
    if (object == null) {
      return "";
    }
    return object.getDisplayName() + " (" + object.getId() + ")";
  }
}
