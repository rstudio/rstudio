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
package com.google.gwt.sample.dynatablerf.client.widgets;

import com.google.gwt.app.client.IntegerBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.sample.dynatablerf.shared.AddressProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Allows an AddressProxy to be edited.
 */
public class AddressEditor extends Composite implements Editor<AddressProxy> {
  interface Binder extends UiBinder<Widget, AddressEditor> {
  }

  @UiField
  TextBox street;
  @UiField
  TextBox city;
  @UiField
  TextBox state;
  @UiField
  IntegerBox zip;

  // TODO: Allow UiBinder to configure this
  final TakesValueEditor<String> streetEditor;
  final TakesValueEditor<String> cityEditor;
  final TakesValueEditor<String> stateEditor;
  final TakesValueEditor<Integer> zipEditor;

  public AddressEditor() {
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));

    // TODO: Let UiBinder wire this up
    streetEditor = TakesValueEditor.of(street);
    cityEditor = TakesValueEditor.of(city);
    stateEditor = TakesValueEditor.of(state);
    zipEditor = TakesValueEditor.of(zip);
  }
}
