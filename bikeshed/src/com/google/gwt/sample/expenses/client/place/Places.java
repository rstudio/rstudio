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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.sample.expenses.shared.ExpensesEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The places of the ExpensesScaffold app.
 */
public class Places {
  public static final EntityListPlace EMPLOYEE_LIST = new EntityListPlace();
  public static final EntityListPlace REPORT_LIST = new EntityListPlace();

  /**
   * @return the set of places to see lists of entities, and their localized
   *         names
   */
  public static Map<EntityListPlace, String> getListPlacesAndNames() {
    // TODO: i18n, get the Strings from a Messages interface. Really, names don't belong
    // in this class at all.
    Map<EntityListPlace, String> navPlaces = new LinkedHashMap<EntityListPlace, String>();
    navPlaces.put(EMPLOYEE_LIST, "Employees");
    navPlaces.put(REPORT_LIST, "Reports");
    return navPlaces;
  }

  public EntityDetailsPlace getDetailsPlaceFor(ExpensesEntity<?> e) {
    return new EntityDetailsPlace(e);
  }

  public EditEntityPlace getEditPlaceFor(ExpensesEntity<?> e) {
    return new EditEntityPlace(e);
  }
}
