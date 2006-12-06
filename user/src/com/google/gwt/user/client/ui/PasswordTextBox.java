/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;

/**
 * A text box that visually masks its input to prevent eavesdropping.
 * 
 * <p>
 * <img class='gallery' src='PasswordTextBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-PasswordTextBox { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TextBoxExample}
 * </p>
 */
public class PasswordTextBox extends TextBoxBase {

  /**
   * Creates an empty password text box.
   */
  public PasswordTextBox() {
    super(DOM.createInputPassword());
    setStyleName("gwt-PasswordTextBox");
  }
}
