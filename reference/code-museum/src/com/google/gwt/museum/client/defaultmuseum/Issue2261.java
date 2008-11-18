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
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Open disclosure panel causes flicker.
 */
public class Issue2261 extends AbstractIssue {

  @Override
  public Widget createIssue() {
    DisclosurePanel disclosurePanel = new DisclosurePanel("Disclosure Panel 1");
    disclosurePanel.setAnimationEnabled(true);
    Label content = new Label("Some content<br/><br/><br/>");
    content.setHeight("200px");
    content.getElement().getStyle().setProperty("background", "blue");
    disclosurePanel.setContent(content);
    return disclosurePanel;
  }

  @Override
  public String getInstructions() {
    return "Open disclosure panel and you should not see a flicker";
  }

  @Override
  public String getSummary() {
    return "DisclosurePanel flicker";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
