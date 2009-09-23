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

/**
 * Base tests for classes that extend {@link Panel}
 * 
 * TODO: add circular containment test.
 * 
 * @param <T> the panel type
 */
public abstract class PanelTestBase<T extends Panel> extends WidgetTestBase {

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(createPanel(),
        new HasWidgetsTester.DefaultWidgetAdder(), supportsMultipleWidgets());
  }

  public void testOnAttach() {
    // Used to call onDetach when not attached.
    Widget someWidget = new TextBox();
    Panel panel1 = createPanel(); // new and unattached
    Panel panel2 = createPanel(); // new and unattached
    panel1.add(someWidget);
    panel2.add(someWidget);

    // Make sure that the RootPanel does not throw an exception.
    RootPanel.get().setParent(null);
    RootPanel.get().setParent(null);
  }

  public void testRemoveWithError() {
    // Create a widget that will throw an exception onUnload.
    BadWidget badWidget = new BadWidget();
    badWidget.setFailOnUnload(true);

    // Add the widget to a panel.
    Panel panel = createPanel();
    panel.add(badWidget);
    assertFalse(badWidget.isAttached());

    // Attach the widget.
    RootPanel.get().add(panel);
    assertTrue(badWidget.isAttached());

    // Remove the widget from the panel.
    try {
      panel.remove(badWidget);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertFalse(badWidget.isAttached());
    assertNull(badWidget.getParent());
    assertNull(badWidget.getElement().getParentElement());

    // Detach the panel to ensure that it doesn't throw an exception.
    RootPanel.get().remove(panel);
  }

  protected abstract T createPanel();

  /**
   * Check if the panel in test supports multiple (unbounded) widgets.
   * 
   * @return true if multiple widgets are supported
   */
  protected boolean supportsMultipleWidgets() {
    return true;
  }
}
