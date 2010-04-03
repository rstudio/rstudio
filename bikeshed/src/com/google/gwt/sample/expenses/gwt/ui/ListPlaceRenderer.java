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
package com.google.gwt.sample.expenses.gwt.ui;

import com.google.gwt.sample.expenses.gwt.place.ExpensesListPlace;
import com.google.gwt.sample.expenses.gwt.request.ExpensesKey;
import com.google.gwt.user.client.ui.Renderer;

/**
 * Renders {@link ExpensesListPlace}s for display to users.
 */
public class ListPlaceRenderer implements Renderer<ExpensesListPlace> {

  private final Renderer<ExpensesKey<?>> entityRenderer;

  /**
   * @param entityRenderer
   */
  public ListPlaceRenderer(Renderer<ExpensesKey<?>> entityRenderer) {
    this.entityRenderer = entityRenderer;
  }

  public String render(ExpensesListPlace object) {
    return entityRenderer.render(object.getKey());
  }
}
