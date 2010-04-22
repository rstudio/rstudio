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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

/**
 * Abstract base class for all recipes.
 */
public abstract class Recipe {
  private final String title;
  private Widget widget;

  public Recipe(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public final Widget getWidget() {
    if (widget == null) {
      widget = createWidget();

      // Minor style hack: we need to give a little margin to all recipes,
      // so we just do it here instead of on all the individual recipes.
      widget.getElement().getStyle().setMargin(0.5, Unit.EM);
    }

    return widget;
  }

  protected abstract Widget createWidget();
}
