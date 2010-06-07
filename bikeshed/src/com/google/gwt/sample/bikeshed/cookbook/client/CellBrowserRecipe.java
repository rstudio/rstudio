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

import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;

/**
 * {@link CellBrowser} Recipe.
 */
public class CellBrowserRecipe extends Recipe {

  public CellBrowserRecipe() {
    super("Cell Browser");
  }

  @Override
  protected Widget createWidget() {
    FlowPanel p = new FlowPanel();

    final Label label = new Label();
    final MultiSelectionModel<String> selectionModel = new MultiSelectionModel<String>();
    selectionModel.addSelectionChangeHandler(new SelectionModel.SelectionChangeHandler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        label.setText("Selected " + selectionModel.getSelectedSet().toString());
      }
    });

    CellBrowser browser = new CellBrowser(new MyTreeViewModel(
        selectionModel), "...");
    browser.setAnimationEnabled(true);
    browser.setHeight("200px");

    p.add(browser);
    p.add(new HTML("<hr>"));
    p.add(label);

    return p;
  }
}
