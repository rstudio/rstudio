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
package com.google.gwt.sample.expenses.gwt.client.place;

/**
 * A convenient base implementation of {@link ScaffoldPlaceProcessor}. Just
 * override the methods for the types of places that you actually care about.
 * <p>
 * <strong>NB</strong>It is a bad idea to use this class if your code needs to
 * be extended when new subclasses of {@link ScaffoldPlace} are added. If that's
 * the case, implement {@link ScaffoldPlaceProcessor} yourself, so that the compiler
 * will let you know to update your code.
 */
public class BaseScaffoldPlaceProcessor implements ScaffoldPlaceProcessor {

  public void process(EmployeeScaffoldPlace employeePlace) {
  }

  public void process(ListScaffoldPlace listPlace) {
  }

  public void process(ReportScaffoldPlace reportPlace) {
  }
}
