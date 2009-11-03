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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.user.client.ui.Label;

/**
 * A label that has an enum, to test UiBinder enum parsing.
 */
public class EnumeratedLabel extends Label {
  
  /**
   * An enum representing the suffix type.
   */
  public enum Suffix { ending, suffix, tail}

  private Suffix suffix = Suffix.ending;
  private String value = "";
  
  @Override
  public void setText(String text) {
    this.value = text;
    update();
  }
  
  public void setSuffix(Suffix suffix) {
    this.suffix = suffix;
    update();
  }

  private void update() {
    super.setText(value + ": " + suffix);
  }
}
