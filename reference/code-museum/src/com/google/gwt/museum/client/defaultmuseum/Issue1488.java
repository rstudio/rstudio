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
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * RichTextArea - setEnabled does not work.
 * 
 */
public class Issue1488 extends AbstractIssue {
  RichTextArea t = new RichTextArea();

  @Override
  public Widget createIssue() {
    t.setFocus(true);
    return t;
  }

  @Override
  public String getInstructions() {
    return "Should not be able to type in the text area";
  }

  @Override
  public String getSummary() {
    return "RichTextArea - setEnabled does not work";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
