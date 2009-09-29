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
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;

/**
 * Tests for {@link HorizontalSplitPanel}.
 */
public class HorizontalSplitPanelTest extends
    SplitPanelTestBase<HorizontalSplitPanel> {

  public void testDebugId() {
    HorizontalSplitPanel hSplit = new HorizontalSplitPanel();
    hSplit.ensureDebugId("hsplit");
    Label left = new Label("left");
    hSplit.setLeftWidget(left);
    Label right = new Label("right");
    hSplit.setRightWidget(right);
    UIObjectTest.assertDebugId("hsplit", hSplit.getElement());
    UIObjectTest.assertDebugId("hsplit-left", DOM.getParent(left.getElement()));
    UIObjectTest.assertDebugId("hsplit-right",
        DOM.getParent(right.getElement()));
  }

  @Override
  protected HorizontalSplitPanel createPanel() {
    return new HorizontalSplitPanel();
  }

  @Override
  protected Widget getEndOfLineWidget(HorizontalSplitPanel split) {
    return split.getEndOfLineWidget();
  }

  @Override
  protected Widget getStartOfLineWidget(HorizontalSplitPanel split) {
    return split.getStartOfLineWidget();
  }

  @Override
  protected void setEndOfLineWidget(HorizontalSplitPanel split, Widget w) {
    split.setEndOfLineWidget(w);
  }

  @Override
  protected void setStartOfLineWidget(HorizontalSplitPanel split, Widget w) {
    split.setStartOfLineWidget(w);
  }
}
