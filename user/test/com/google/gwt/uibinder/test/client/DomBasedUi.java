/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

/**
 * Sample use of a {@code UiBinder} with no dependency on
 * com.google.gwt.user.
 */
public class DomBasedUi {
  /**
   * Resources for this template.
   */
  public interface Resources extends ClientBundle {
    @Source("DomBasedUi.css")
    Style style();
  }

  /**
   * CSS for this template.
   */
  public interface Style extends CssResource {
    String bodyColor();
    String bodyFont();
  }

  interface Binder extends UiBinder<Element, DomBasedUi> {
  }

  private static final Resources res = GWT.create(Resources.class);
  private static final Binder binder = GWT.create(Binder.class);

  @UiField SpanElement nameSpan;
  @UiField Element tmElement;
  @UiField Element root;
  @UiField TableColElement narrowColumn;
  @UiField TableRowElement tr;
  @UiField TableCellElement th1;
  @UiField TableCellElement th2;
  @UiField TableSectionElement tbody;
  @UiField TableCellElement th4;
  
  public DomBasedUi(String yourNameHere) {
    res.style().ensureInjected();
    binder.createAndBindUi(this);
    nameSpan.setInnerText(yourNameHere);
  }
}
