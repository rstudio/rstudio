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

import static com.google.gwt.sample.dynatablerf.client.StringConstants.CITY;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.STATE;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.STREET;
import static com.google.gwt.sample.dynatablerf.client.StringConstants.ZIP;

import com.google.gwt.app.client.IntegerBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.IntegerEditor;
import com.google.gwt.editor.client.StringEditor;
import com.google.gwt.sample.dynatablerf.shared.AddressProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows an AddressProxy to be edited.
 */
public class AddressEditor extends Composite implements Editor<AddressProxy> {
  interface Binder extends UiBinder<Widget, AddressEditor> {
  }

  private static final String[] ALL_PROPERTIES = {STREET, CITY, STATE, ZIP};

  @UiField
  TextBox street;
  @UiField
  TextBox city;
  @UiField
  TextBox state;
  @UiField
  IntegerBox zip;

  final StringEditor streetEditor;
  final StringEditor cityEditor;
  final StringEditor stateEditor;
  final IntegerEditor zipEditor;

  public AddressEditor() {
    initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));

    // TODO: Let UiBinder wire this up
    streetEditor = StringEditor.of(street);
    cityEditor = StringEditor.of(city);
    stateEditor = StringEditor.of(state);
    zipEditor = IntegerEditor.of(zip);
  }

  public Editor<?> getEditorForPath(String path) {
    if (STREET.equals(path)) {
      return streetEditor;
    } else if (CITY.equals(path)) {
      return cityEditor;
    } else if (STATE.equals(path)) {
      return stateEditor;
    } else if (ZIP.equals(path)) {
      return zipEditor;
    }
    return null;
  }

  public Set<String> getPaths() {
    return new HashSet<String>(Arrays.asList(ALL_PROPERTIES));
  }
}
