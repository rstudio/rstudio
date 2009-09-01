/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.sample.client;

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

/**
 * A widget tied to a non-primitive type, used to show that
 * such properties can be set via templates.
 */
public class FooLabel extends Composite {
  @UiConstructor
  public FooLabel() {
    Label l = new Label();
    initWidget(l);
  }

  /**
   * @return the text
   */
  public String getText() {
    return getLabel().getText();
  }

  public void setPojo(ArbitraryPojo pojo) {
    getLabel().setText("This widget has non primitive properties: "
        + pojo.toString());
  }

  private Label getLabel() {
    return (Label) getWidget();
  }
}
