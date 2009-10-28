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

package com.google.gwt.museum.client.common;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Helper class to get/set default values for visual tests.
 */
public class ControlInputPanel extends Composite {
  /**
   * Input
   * 
   * @param <V>
   * @param <W>
   */
  public abstract static class Input<V, W extends Widget> {
    protected String name;
    protected V value;
    protected W widget;

    protected Input(String name) {
      this.name = name;
      this.widget = createInputWidget();
    }

    /**
     * Gets the name of the input
     * 
     * @return the name of the input
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the value of this input.
     */
    public abstract V getValue();

    @SuppressWarnings("unchecked")
    protected W createInputWidget() {
      return (W) new TextBox();
    }

    protected void setValue(V value) {
      ((HasText) widget).setText(value.toString());
    }
  }

  /**
   * Set/get integer value.
   */

  public static class IntegerInput extends Input<Integer, TextBox> {
    public IntegerInput(String name, int defaultValue, ControlInputPanel p) {
      this(name, defaultValue, defaultValue, p);
    }

    public IntegerInput(String name, int defaultHostedValue,
        int defaultWebValue, ControlInputPanel p) {
      super(name);

      this.value = GWT.isScript() ? defaultWebValue : defaultHostedValue;
      p.add(this);
      widget.setText(value.toString());
    }

    @Override
    public Integer getValue() {
      return Integer.valueOf(widget.getText());
    }
  }

  final FlexTable layout = new FlexTable();

  private int numInputs;

  public ControlInputPanel() {
    layout.setWidth("100%");
    initWidget(layout);
  }

  private void add(Input<?, ?> input) {
    layout.setText(0, numInputs, input.getName());
    layout.setWidget(1, numInputs, input.widget);
  }
}
