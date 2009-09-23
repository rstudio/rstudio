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

package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Test for {@link DecoratorPanel}.
 */
public class DecoratorPanelTest extends SimplePanelTestBase<DecoratorPanel> {

  /**
   * Assert that an element has the specified class name.
   * 
   * @param elem the DOM {@link Element}
   * @param styleName the style name the element should have
   */
  private static void assertStyleName(Element elem, String styleName) {
    assertEquals(styleName, DOM.getElementProperty(elem, "className"));
  }

  /**
   * Test addition and removal of widgets.
   */
  public void testAddRemoveWidget() {
    DecoratorPanel panel = new DecoratorPanel();
    Label contents = new Label("test");
    panel.setWidget(contents);
    assertEquals(contents, panel.getWidget());
  }

  /**
   * Test the ability to define custom rows.
   */
  public void testCustomRows() {
    String[] rowStyles = {"rowA", "rowB", "rowC", "rowD"};
    DecoratorPanel panel = new DecoratorPanel(rowStyles, 2);

    // Check the styles of each row
    for (int i = 0; i < rowStyles.length; i++) {
      String rowStyle = rowStyles[i];
      assertStyleName(DOM.getParent(DOM.getParent(panel.getCellElement(i, 0))),
          rowStyle);
      assertStyleName(DOM.getParent(panel.getCellElement(i, 0)), rowStyle
          + "Left");
      assertStyleName(DOM.getParent(panel.getCellElement(i, 1)), rowStyle
          + "Center");
      assertStyleName(DOM.getParent(panel.getCellElement(i, 2)), rowStyle
          + "Right");
    }

    // Check the container element
    assertTrue(panel.getCellElement(2, 1) == panel.getContainerElement());
  }

  /**
   * Test the default styles.
   */
  public void testDefaultStyles() {
    String[] rowStyles = {"top", "middle", "bottom"};
    DecoratorPanel panel = new DecoratorPanel();

    // Check the styles of each row
    for (int i = 0; i < rowStyles.length; i++) {
      String rowStyle = rowStyles[i];
      assertStyleName(DOM.getParent(DOM.getParent(panel.getCellElement(i, 0))),
          rowStyle);
      assertStyleName(DOM.getParent(panel.getCellElement(i, 0)), rowStyle
          + "Left");
      assertStyleName(DOM.getParent(panel.getCellElement(i, 1)), rowStyle
          + "Center");
      assertStyleName(DOM.getParent(panel.getCellElement(i, 2)), rowStyle
          + "Right");
    }

    // Check the container element
    assertTrue(panel.getCellElement(1, 1) == panel.getContainerElement());
  }

  @Override
  protected DecoratorPanel createPanel() {
    return new DecoratorPanel();
  }
}
