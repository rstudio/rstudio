/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.client.LocaleInfo;

/**
 * Tests {@link HTML}.
 * Note: tests only the direction and alignment logic.
 */
public class HTMLTest extends LabelTest {

  private final String EN_HTML = "<b style=\"color: red\">" + EN_TEXT + "</b>";
  private final String IW_HTML = "<b style=\"color: red\">" + IW_TEXT + "</b>";
  private HTML label;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  // setDirection is deprecated; this only assures backwards compatibility.
  public void testSetDirection() {
    for (int i = 0; i < 2; i++) {
      String id = i == 0 ? "div label: " : "span label: ";
      label = HTML.wrap(i == 0 ? createAttachedDivElement() :
          createAttachedSpanElement());
      label.setDirection(Direction.RTL);
      assertLabelDirection(id + "label's direction is incorrect after " +
          "setDirection", Direction.RTL);

      label.setText(EN_TEXT, Direction.LTR);
      assertLabelDirection(id + "label's direction is incorrect after " +
          "setText with a specific direction", Direction.LTR);

      label.setText(EN_TEXT);
      assertLabelDirection(id + "label's direction wasn't reverted to the " +
          "direction set by last setDirection when calling setText with no " +
          "direction argument and without a directionEstimator", Direction.RTL);
      if (i == 1) {
        // For span element, we also specifically assert that the direction of
        // the topmost element matches the last setDirection.
        assertEquals(id + "element's direction does not match the direction " +
            "set by last setDirection when calling setText with no direction " +
            "argument and without a directionEstimator", Direction.RTL,
            BidiUtils.getDirectionOnElement(label.getElement()));
      }
    }
  }

  public void testSetDirectionEstimatorAndSetHtml() {
    testSetDirectionEstimatorAndSetTextOrHtml(true);
  }

  public void testSetDirectionEstimatorAndSetText() {
    testSetDirectionEstimatorAndSetTextOrHtml(false);
  }

  /**
   * Asserts that both the {@link Label#getContentDirection} and the physical
   * dir attribute match the expected direction.
   *
   * @param message Assertion message
   * @param expected Expected direction
   */
  private void assertLabelDirection(String message, Direction expected) {
    assertTrue("attribute mismatch: " + message,
        expected == getLabelDirection() ||
        /* For inline elements, empty dir attribute is acceptable if LTR is
         * expected and the locale is not RTL. */
        isSpanWrapped() && getLabelDirection() == Direction.DEFAULT &&
        expected == Direction.LTR && !LocaleInfo.getCurrentLocale().isRTL());

    assertEquals("contentDir mismatch: " + message, expected,
        label.getContentDirection());
  }

  private Direction getLabelDirection() {
    Element element = isSpanWrapped() ?
        label.getElement().getFirstChildElement() : label.getElement();

    return BidiUtils.getDirectionOnElement(element);
  }

  // This will not work generally. It assumes that the label's content isn't
  // consist of a span tag.
  private boolean isSpanWrapped() {
    Element inner = label.getElement().getFirstChildElement();
    return inner != null && inner.getTagName().equalsIgnoreCase("span");
  }

  private void setLabelTextOrHtml(String content, boolean isHtml) {
    if (isHtml) {
      label.setHTML(content);
    } else {
      label.setText(content);
    }
  }

  private void setLabelTextOrHtml(String content, Direction dir, boolean isHtml) {
    if (isHtml) {
      label.setHTML(content, dir);
    } else {
      label.setText(content, dir);
    }
  }

  private void testSetDirectionEstimatorAndSetTextOrHtml(boolean isHtml) {
    String enContent = isHtml ? EN_HTML : EN_TEXT;
    String iwContent = isHtml ? IW_HTML : IW_TEXT;
    for (int i = 0; i < 2; i++) {
      String id = i == 0 ? "div label: " : "span label: ";
      label = HTML.wrap(i == 0 ? createAttachedDivElement() :
          createAttachedSpanElement());

      setLabelTextOrHtml(enContent, isHtml);
      assertLabelDirection(id + "label's direction is not DEFAULT upon " +
          "standard initialization", Direction.DEFAULT);

      setLabelTextOrHtml(iwContent, Direction.RTL, isHtml);
      assertLabelDirection(id + "label's direction is not RTL after it was" +
          " explicitly set to RTL", Direction.RTL);

      setLabelTextOrHtml(enContent, isHtml);
      assertLabelDirection(id + "label's direction was not specified, and no" +
          " estimator specified, thus should return to initial value (DEFAULT)",
          Direction.DEFAULT);

      label.setDirectionEstimator(true);
      assertLabelDirection(id + "label's direction wasn't instantly updated" +
          " to LTR on switching direction estimation on", Direction.LTR);

      setLabelTextOrHtml(iwContent, isHtml);
      assertLabelDirection(id + "label's direction wasn't estimated as RTL",
          Direction.RTL);

      setLabelTextOrHtml(iwContent, Direction.LTR, isHtml);
      assertLabelDirection(id + "label's direction is not LTR after it was" +
          " explicitly set to LTR (direction estimation is on)", Direction.LTR);

      setLabelTextOrHtml(iwContent, Direction.DEFAULT, isHtml);
      assertLabelDirection(id + "label's direction is not DEFAULT after it" +
          " was explicitly set to DEFAULT (direction estimation is on)",
          Direction.DEFAULT);

      // TODO(jlabanca): Need a cross-browser way to test innerHTML.
      // assertEquals(id + "retreived html is incorrect", iwContent,
      //     label.getHTML().toLowerCase());
      assertEquals(id + "retreived text is incorrect", IW_TEXT,
          label.getText().toLowerCase());
    }
  }
}
