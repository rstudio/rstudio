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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.junit.client.GWTTestCase;

import com.google.gwt.user.client.ui.HasHorizontalAlignment.AutoHorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

/**
 * Tests {@link Label}.
 * Note: tests only the alignment logic. direction logic is tested at
 * {@link HTMLTest}, and other stuff remains currently untested.
 */
public class LabelTest extends GWTTestCase {

  static final String html1 = "<b>hello</b><i>world</i>:)";
  static final String html2 = "<b>goodbye</b><i>world</i>:(";

  protected final String EN_TEXT = "abc";
  protected final String IW_TEXT = "\u05e0\u05e1\u05e2";
  private Label label;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testSetAutoHorizontalAlignmentNoDirectionEstimator() {
    Element elem = createAttachedDivElement();
    // Initialize the div with a specific direction, to verify it remembers its
    // original direction on setText with no direction argument.
    BidiUtils.setDirectionOnElement(elem, Direction.LTR);
    label = Label.wrap(elem);

    label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    assertAlign("horizontal alignment was set to left by " +
        "setHorizontalAlignment, but is not",
        HasHorizontalAlignment.ALIGN_LEFT);

    label.setAutoHorizontalAlignment(null);
    assertEquals("text-align is not empty after " +
        "setAutoHorizontalAlignment(null)", "",
        label.getElement().getStyle().getTextAlign());

    label.setAutoHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    assertAlign("horizontal alignment was set to right by " +
        "setAutoHorizontalAlignment, but is not",
        HasHorizontalAlignment.ALIGN_RIGHT);

    label.setText(IW_TEXT, Direction.RTL);
    label.setAutoHorizontalAlignment(
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);
    assertAlign("automatic horizontal alignment was set to ALIGN_CONTENT_END," +
        " content was declared RTL", HasHorizontalAlignment.ALIGN_LEFT,
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);

    label.setText(EN_TEXT);
    assertAlign("automatic horizontal alignment was set to ALIGN_CONTENT_END," +
        " content direction was reset to the original LTR after calling " +
        "setText with no direction argument",
        HasHorizontalAlignment.ALIGN_RIGHT,
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);

    label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_JUSTIFY);
    assertAlign("horizontal alignment was set to justify by " +
        "setHorizontalAlignment, but is not",
        HasHorizontalAlignment.ALIGN_JUSTIFY);
  }

  public void testSetAutoHorizontalAlignmentWithDirectionEstimator() {
    Element elem = createAttachedDivElement();
    // Initialize the div with a specific direction, to verify it remembers its
    // original direction on turning direction estimator off.
    BidiUtils.setDirectionOnElement(elem, Direction.LTR);
    label = Label.wrap(elem);

    label.setAutoHorizontalAlignment(
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);
    label.setDirectionEstimator(true);
    label.setText(IW_TEXT);
    assertAlign("automatic horizontal alignment was set to ALIGN_CONTENT_END," +
        " and content is supposedly estimated as RTL",
        HasHorizontalAlignment.ALIGN_LEFT,
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);

    label.setAutoHorizontalAlignment(
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);
    assertAlign("automatic horizontal alignment was set (again) to " +
        "ALIGN_CONTENT_END, and content is estimated as RTL",
        HasHorizontalAlignment.ALIGN_LEFT,
        HasAutoHorizontalAlignment.ALIGN_CONTENT_END);

    label.setAutoHorizontalAlignment(
        HasAutoHorizontalAlignment.ALIGN_CONTENT_START);
    assertAlign("automatic horizontal alignment was set to " +
        "ALIGN_CONTENT_START, content is estimated as RTL",
        HasHorizontalAlignment.ALIGN_RIGHT,
        HasAutoHorizontalAlignment.ALIGN_CONTENT_START);

    label.setDirectionEstimator(false);
    assertAlign("horizontal alignment was supposed to be reset to the " +
        "original ALIGN_LEFT after turning off direction estimator, and " +
        "automatic horizontal alignment was to ALIGN_CONTENT_START",
        HasHorizontalAlignment.ALIGN_LEFT,
        HasAutoHorizontalAlignment.ALIGN_CONTENT_START);
  }

  /**
   * Create a div and attach it to the {@link RootPanel}.
   *
   * @return the new div
   */
  protected Element createAttachedDivElement() {
    DivElement elem = Document.get().createDivElement();
    RootPanel.getBodyElement().appendChild(elem);
    return elem;
  }

  /**
   * Create a span and attach it to the {@link RootPanel}.
   *
   * @return the new span
   */
  protected Element createAttachedSpanElement() {
    SpanElement elem = Document.get().createSpanElement();
    RootPanel.getBodyElement().appendChild(elem);
    return elem;
  }

  private void assertAlign(String msg, HorizontalAlignmentConstant expected) {
    assertAlign(msg, expected, expected);
  }

  /**
   * Asserts that everything is fine with the alignment.
   *
   * @param msg assertion message
   * @param expected expected horizontal alignment
   * @param expectedAuto expected auto horizontal alignment
   */
  private void assertAlign(String msg, HorizontalAlignmentConstant expected,
      AutoHorizontalAlignmentConstant expectedAuto) {
    assertEquals(msg + " (text-align property value is incorrect)",
        expected.getTextAlignString(),
        label.getElement().getStyle().getTextAlign());
    assertEquals(msg + " (getHorizontalAlignment return value is incorrect)",
        expected, label.getHorizontalAlignment());
    assertEquals(msg + " (getAutoHorizontalAlignment return value is incorrect)",
        expectedAuto, label.getAutoHorizontalAlignment());
  }
}
