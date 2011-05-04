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
package com.google.gwt.uibinder.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests {@link UiBinderUtil}.
 */
public class UiBinderUtilTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderSuite";
  }

  public void testAttachToDomAndGetChildUnattached() {
    go();
  }

  public void testAttachToDomAndGetChildUnderUnattached() {
    DivElement div = Document.get().createDivElement();
    try {
      go(div);
    } finally {
      detach(div);
    }
  }

  public void testAttachToDomAndGetChildUnderHidden() {
    DivElement div = Document.get().createDivElement();
    try {
      RootPanel.getBodyElement().appendChild(div);
      div.getStyle().setVisibility(Visibility.HIDDEN);
      go(div);
    } finally {
      detach(div);
    }
  }

  public void testAttachToDomAndGetChildUnderDisplayNone() {
    DivElement div = Document.get().createDivElement();
    try {
      RootPanel.getBodyElement().appendChild(div);
      div.getStyle().setDisplay(Display.NONE);
      go(div);
    } finally {
      detach(div);
    }
  }

  public void testAttachToDomAndGetChildUnderAttachedThenUnattached() {
    DivElement div = Document.get().createDivElement();
    detach(div);
    try {
      RootPanel.getBodyElement().appendChild(div);
      go(div);
    } finally {
      detach(div);
    }
  }

  /**
   * Make sure this test's clean up method actually works.
   */
  public void testDetach() {
    DivElement div = Document.get().createDivElement();
    RootPanel.getBodyElement().appendChild(div);
    detach(div);
    assertNull(div.getParentElement());
  }

  private void assertStartsWith(String string, String prefix) {
    assertTrue('"' + string + "\" should start with \"" + prefix + "\"",
        string.startsWith(prefix));
  }

  private void findAndAssertTextBeforeFirstChild(Element div, String id,
      String firstText) {
    UiBinderUtil.TempAttachment t = UiBinderUtil.attachToDom(div);
    Element child = Document.get().getElementById(id);
    t.detach();
    assertStartsWith(child.getInnerHTML(), firstText + "<");
  }

  private void detach(Element div) {
    if (div != null) {
      Element parent = div.getParentElement();
      if (parent != null) {
        parent.removeChild(div);
      }
    }
  }

  private void go() {
    go(null);
  }

  private void go(Element underHere) {
    Element div = null;
    try {
      String ableId = DOM.createUniqueId();
      String bakerId = DOM.createUniqueId();
      String charlieId = DOM.createUniqueId();
      String deltaId = DOM.createUniqueId();

      String ableText = "able" + Random.nextInt();
      String bakerText = "baker" + Random.nextInt();
      String charlieText = "charlie" + Random.nextInt();
      String deltaText = "delta" + Random.nextInt();

      StringBuilder b = new StringBuilder();
      b.append("<div>");
      b.append("<span id='").append(ableId).append("'>").append(ableText);
      b.append("<span id='").append(bakerId).append("'>").append(bakerText);
      b.append("<span id='").append(charlieId).append("'>").append(charlieText);
      b.append("<span id='").append(deltaId).append("'>").append(deltaText);
      b.append("</span>").append("</span>").append("</span>").append("</span>");
      b.append("</div>");

      div = UiBinderUtil.fromHtml(b.toString());
      if (underHere != null) {
        underHere.insertFirst(div);
      }
      findAndAssertTextBeforeFirstChild(div, ableId, ableText);
      findAndAssertTextBeforeFirstChild(div, bakerId, bakerText);
      findAndAssertTextBeforeFirstChild(div, charlieId, charlieText);
      UiBinderUtil.TempAttachment t = UiBinderUtil.attachToDom(div);
      Element e = Document.get().getElementById(deltaId);
      t.detach();
      assertEquals(deltaText, e.getInnerText());
    } finally {
      // tearDown isn't reliable enough, e.g. doesn't fire when exceptions
      // happen
      detach(div);
    }
  }
}
