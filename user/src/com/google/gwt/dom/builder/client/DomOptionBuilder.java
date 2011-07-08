/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.builder.client;

import com.google.gwt.dom.builder.shared.OptionBuilder;
import com.google.gwt.dom.client.OptionElement;

/**
 * DOM-based implementation of {@link OptionBuilder}.
 */
public class DomOptionBuilder extends
    DomElementBuilderBase<OptionBuilder, OptionElement> implements OptionBuilder {

  DomOptionBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public OptionBuilder defaultSelected() {
    assertCanAddAttribute().setDefaultSelected(true);
    return this;
  }

  @Override
  public OptionBuilder disabled() {
    assertCanAddAttribute().setDisabled(true);
    return this;
  }

  @Override
  public OptionBuilder label(String label) {
    assertCanAddAttribute().setLabel(label);
    return this;
  }

  @Override
  public OptionBuilder selected() {
    assertCanAddAttribute().setSelected(true);
    return this;
  }

  @Override
  public OptionBuilder value(String value) {
    assertCanAddAttribute().setValue(value);
    return this;
  }
}
