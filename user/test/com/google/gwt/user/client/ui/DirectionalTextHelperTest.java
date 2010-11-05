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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.shared.AnyRtlDirectionEstimator;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link DirectionalTextHelper}.
 */
public class DirectionalTextHelperTest extends GWTTestCase {

  private final String EN_TEXT = "abc";
  private final String IW_TEXT = "\u05e0\u05e1\u05e2";
  private final String EN_HTML = "<b style=\"color: red\">" + EN_TEXT + "</b>";
  private final String IW_HTML = "<b style=\"color: red\">" + IW_TEXT + "</b>";
  private Element element;
  private DirectionalTextHelper directionalTextHelper;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  // setDirection is deprecated; this only assures backwards compatibility.
  public void testSetDirection() {
    element = Document.get().createSpanElement();
    directionalTextHelper = new DirectionalTextHelper(element,
        /* is inline? */ true);

    directionalTextHelper.setDirection(Direction.RTL);
    assertDirection("element's direction is incorrect after setDirection",
        Direction.RTL);

    directionalTextHelper.setTextOrHtml(EN_TEXT, Direction.LTR, false);
    assertDirection("target's direction is incorrect after setText with a" +
        "specific direction", Direction.LTR);

    directionalTextHelper.setTextOrHtml(EN_TEXT, false);
    assertDirection("target's direction wasn't reverted to the direction set" +
        "by last setDirection when calling setText with no direction argument" +
        "and without a directionEstimator", Direction.RTL);

    // We also specifically assert that the direction of the topmost element
    // matches the last setDirection. (this is needed only for inline elements).
    assertEquals("element's direction does not match the direction set by " +
        "last setDirection when calling setText with no direction argument " +
        "and without a directionEstimator", Direction.RTL,
        BidiUtils.getDirectionOnElement(element));
  }

  public void testSetDirectionEstimator() {
    element = Document.get().createSpanElement();
    BidiUtils.setDirectionOnElement(element, Direction.LTR);
    directionalTextHelper = new DirectionalTextHelper(element,
        /* is inline? */ true);
    directionalTextHelper.setDirectionEstimator(true);
    
    // If the element is span-wrapped, a redundant refresh occurred.
    assertFalse("setDirectionEstimator(true) refreshed appearance before text" +
        "had been received", isSpanWrapped());
    
    directionalTextHelper.setDirectionEstimator(false);
    directionalTextHelper.setTextOrHtml(IW_TEXT, false);
    assertDirection("Original element's direction (LTR) was modified with no" +
        "apparent reason", Direction.LTR);

    directionalTextHelper.setDirectionEstimator(true);
    assertDirection("Direction was not refreshed on " +
        "setDirectionEstimator(true) after receiving text with no explicit " +
        "direction", Direction.RTL);
    
    directionalTextHelper.setTextOrHtml(IW_TEXT, Direction.LTR, false);
    directionalTextHelper.setDirectionEstimator(
        AnyRtlDirectionEstimator.get());
    assertDirection("Direction was refreshed on setDirectionEstimator after " +
        "receiving text with explicit direction", Direction.LTR);
    
    directionalTextHelper.setTextOrHtml(IW_TEXT, false);
    directionalTextHelper.setDirectionEstimator(false);
    assertDirection("Direction was not reset to the initial element direction" +
        " on turning off direction estimation when last call to setTextOrHtml" +
        " did not declare explicit direction.", Direction.LTR);
  }

  public void testSetDirectionEstimatorAndSetHtml() {
    testSetTextOrHtml(true);
  }

  public void testSetDirectionEstimatorAndSetText() {
    testSetTextOrHtml(false);
  }

  /**
   * Asserts that both the {@link HasDirectionalText#getTextDirection} and the
   * physical dir attribute match the expected direction.
   *
   * @param message Assertion message
   * @param expected Expected direction
   */
  private void assertDirection(String message, Direction expected) {
    assertTrue("dir attribute mismatch: " + message,
        expected == getElementDirection() ||
        /* For inline elements, empty dir attribute is acceptable if LTR is
         * expected and the locale is not RTL. */
        isSpanWrapped() && getElementDirection() == Direction.DEFAULT &&
        (expected == Direction.RTL) == LocaleInfo.getCurrentLocale().isRTL());

    assertEquals("textDir mismatch: " + message, expected,
        directionalTextHelper.getTextDirection());
  }

  private Direction getElementDirection() {
    Element elem = isSpanWrapped() ? element.getFirstChildElement() : element;
    return BidiUtils.getDirectionOnElement(elem);
  }

  // This will not work generally. It assumes that the widget's content isn't
  // consist of a span tag.
  private boolean isSpanWrapped() {
    Element inner = element.getFirstChildElement();
    return inner != null && inner.getTagName().equalsIgnoreCase("span");
  }

  private void testSetTextOrHtml(boolean isHtml) {
    String enContent = isHtml ? EN_HTML : EN_TEXT;
    String iwContent = isHtml ? IW_HTML : IW_TEXT;
    for (int i = 0; i < 2; i++) {
      boolean isDiv = i == 0;
      String id = isDiv ? "div widget: " : "span widget: ";
      element = isDiv ? Document.get().createDivElement() :
          Document.get().createSpanElement();
      directionalTextHelper = new DirectionalTextHelper(element,
          /* is inline? */ !isDiv);

      directionalTextHelper.setTextOrHtml(enContent, isHtml);
      assertDirection(id + "widget's direction is not DEFAULT upon " +
          "standard initialization", Direction.DEFAULT);

      directionalTextHelper.setTextOrHtml(iwContent, Direction.RTL, isHtml);
      assertDirection(id + "widget's direction is not RTL after it was" +
          " explicitly set to RTL", Direction.RTL);

      directionalTextHelper.setTextOrHtml(enContent, isHtml);
      assertDirection(id + "widget's direction was not specified, and no" +
          " estimator specified, thus should return to initial value (DEFAULT)",
          Direction.DEFAULT);

      // Toggling on direction estimation from now on.    
      directionalTextHelper.setDirectionEstimator(true);
      
      assertDirection(id + "widget's direction wasn't instantly updated" +
          " to LTR on switching direction estimation on", Direction.LTR);

      directionalTextHelper.setTextOrHtml(iwContent, isHtml);
      assertDirection(id + "widget's direction wasn't estimated as RTL",
          Direction.RTL);

      directionalTextHelper.setTextOrHtml(iwContent, Direction.LTR, isHtml);
      assertDirection(id + "widget's direction is not LTR after it was" +
          " explicitly set to LTR (direction estimation is on)", Direction.LTR);

      directionalTextHelper.setTextOrHtml(iwContent, Direction.DEFAULT, isHtml);
      assertDirection(id + "widget's direction is not DEFAULT after it" +
          " was explicitly set to DEFAULT (direction estimation is on)",
          Direction.DEFAULT);

      // TODO(jlabanca): Need a cross-browser way to test innerHTML.
      // assertEquals(id + "retreived html is incorrect", iwContent,
      //     directionalTextHelper.getTextOrHtml(true).toLowerCase());
      assertEquals(id + "retreived text is incorrect", IW_TEXT,
          directionalTextHelper.getTextOrHtml(false).toLowerCase());
    }
  }
}
