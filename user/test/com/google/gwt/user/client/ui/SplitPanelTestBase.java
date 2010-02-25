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
 * Tests both {@link HorizontalSplitPanel} and {@link VerticalSplitPanel}.
 * 
 * @param <T> the panel type
 */
public abstract class SplitPanelTestBase<T extends SplitPanel> extends
    PanelTestBase<T> {

  private static Widget createMockWidget() {
    final Label label = new Label();
    label.setText("Testing 1, 2, 3");
    DOM.setStyleAttribute(label.getElement(), "fontSize", "72pt");
    return label;
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  @Override
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(createPanel(),
        new HasWidgetsTester.DefaultWidgetAdder(), false);
  }

  /**
   * Tests creation, widget assignment, null assignment.
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testSplitPanelCreate() {
    final T panel = createPanel();
    final Widget widgetA = createMockWidget();
    final Widget widgetB = createMockWidget();

    // Intentionally add before setting widgets.
    RootPanel.get().add(panel);

    panel.setHeight("100px");
    panel.setWidth("100px");

    // Ensure position can be set before widgets are added.
    panel.setSplitPosition("20px");

    setEndOfLineWidget(panel, widgetB);
    setStartOfLineWidget(panel, widgetA);

    assertTrue(getEndOfLineWidget(panel) == widgetB);
    assertTrue(getStartOfLineWidget(panel) == widgetA);

    setStartOfLineWidget(panel, null);
    setEndOfLineWidget(panel, null);

    assertTrue(getStartOfLineWidget(panel) == null);
    assertTrue(getEndOfLineWidget(panel) == null);

    setStartOfLineWidget(panel, widgetB);
    setEndOfLineWidget(panel, widgetA);

    assertTrue(getStartOfLineWidget(panel) == widgetB);
    assertTrue(getEndOfLineWidget(panel) == widgetA);

    // Ensure we ended up at the right size.
    assertEquals(100, panel.getOffsetWidth());
    assertEquals(100, panel.getOffsetHeight());
  }

  /**
   * Get the widget at the end of the line.
   * 
   * @param split the {@link SplitPanel}
   * @return the widget
   */
  protected abstract Widget getEndOfLineWidget(T split);

  /**
   * Get the widget at the start of the line.
   * 
   * @param split the {@link SplitPanel}
   * @return the widget
   */
  protected abstract Widget getStartOfLineWidget(T split);

  /**
   * Set the widget at the end of the line.
   * 
   * @param split the {@link SplitPanel}
   * @param w the widget to set
   */
  protected abstract void setEndOfLineWidget(T split, Widget w);

  /**
   * Set the widget at the start of the line.
   * 
   * @param split the {@link SplitPanel}
   * @param w the widget to set
   */
  protected abstract void setStartOfLineWidget(T split, Widget w);
}
