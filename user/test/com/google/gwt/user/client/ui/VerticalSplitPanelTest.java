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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.DOM;

/**
 * Tests for {@link VerticalSplitPanel}.
 */
public class VerticalSplitPanelTest extends
    SplitPanelTestBase<VerticalSplitPanel> {

  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testDebugId() {
    VerticalSplitPanel vSplit = new VerticalSplitPanel();
    vSplit.ensureDebugId("vsplit");
    Label top = new Label("top");
    vSplit.setTopWidget(top);
    Label bottom = new Label("bottom");
    vSplit.setBottomWidget(bottom);
    UIObjectTest.assertDebugId("vsplit", vSplit.getElement());
    UIObjectTest.assertDebugId("vsplit-top", DOM.getParent(top.getElement()));
    UIObjectTest.assertDebugId("vsplit-bottom",
        DOM.getParent(bottom.getElement()));
  }

  @Override
  protected VerticalSplitPanel createPanel() {
    return new VerticalSplitPanel();
  }

  @Override
  protected Widget getEndOfLineWidget(VerticalSplitPanel split) {
    return split.getBottomWidget();
  }

  @Override
  protected Widget getStartOfLineWidget(VerticalSplitPanel split) {
    return split.getTopWidget();
  }

  @Override
  protected void setEndOfLineWidget(VerticalSplitPanel split, Widget w) {
    split.setBottomWidget(w);
  }

  @Override
  protected void setStartOfLineWidget(VerticalSplitPanel split, Widget w) {
    split.setTopWidget(w);
  }
}
