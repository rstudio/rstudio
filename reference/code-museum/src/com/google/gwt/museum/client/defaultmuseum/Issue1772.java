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
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalSplitPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * If a {@link VerticalSplitPanel} contains a {@link Frame}, then the splitter
 * will stop responding when the user drags it over the {@link Frame}. If the
 * user releases the mouse over the frame, the splitter will continue to move
 * when it emerges froo the {@link Frame}.
 */
public class Issue1772 extends AbstractIssue {
  @Override
  public Widget createIssue() {
    // Create a heavy grid for top of the split panel
    Grid topWidget = new Grid(10, 10);
    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        topWidget.setHTML(i, j, i + ":" + j);
      }
    }

    // Create a frame for the bottom of the split panel
    Frame bottomWidget = new Frame("http://www.google.com");
    bottomWidget.setSize("100%", "100%");

    // Create the split panel
    VerticalSplitPanel splitPanel = new VerticalSplitPanel();
    splitPanel.getElement().getStyle().setProperty("marginTop", "100px");
    splitPanel.getElement().getStyle().setProperty("marginLeft", "200px");
    splitPanel.getElement().getStyle().setProperty("border", "3px solid black");
    splitPanel.setPixelSize(500, 500);
    splitPanel.setTopWidget(topWidget);
    splitPanel.setBottomWidget(bottomWidget);
    return splitPanel;
  }

  @Override
  public String getInstructions() {
    return "Drag the splitter into the bottom half of the split panel very "
        + "quickly and release the mouse when it is no longer over the "
        + "splitter.  The splitter should not continue to move after the mouse "
        + "button is released.";
  }

  @Override
  public String getSummary() {
    return "SplitPanel continues to drag after mouse is released";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }
}
