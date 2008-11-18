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

package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Clicking on StackPanel fails to open stack.
 */
public class Issue2331 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    StackPanel p = new StackPanel();
    p.add(new Label("Content A"), "Header A");
    p.add(new Label("Content B"), "Header B");
    p.add(new Label("Content C"), "Header C");
    return p;
  }

  @Override
  public String getInstructions() {
    return "Click on B";
  }

  @Override
  public String getSummary() {
    return "Stack Panel does not response to switching stacks";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
