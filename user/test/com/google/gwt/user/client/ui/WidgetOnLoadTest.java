/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Tests the semantics and ordering of onAttach/onDetach/onLoad/onUnload.
 */
public class WidgetOnLoadTest extends GWTTestCase {

  static class TestPanel extends FlowPanel {
    int onAttachOrder;
    int onLoadOrder;
    int onDetachOrder;
    int onUnloadOrder;
    boolean domAttachedOnLoad;
    boolean domAttachedOnUnload;

    @Override
    protected void onAttach() {
      onAttachOrder = ++orderIndex;
      super.onAttach();
    }

    @Override
    protected void onDetach() {
      onDetachOrder = ++orderIndex;
      super.onDetach();
    }

    @Override
    protected void onLoad() {
      onLoadOrder = ++orderIndex;
      domAttachedOnLoad = isElementAttached(getElement());
      super.onLoad();
    }

    @Override
    protected void onUnload() {
      onUnloadOrder = ++orderIndex;
      domAttachedOnUnload = isElementAttached(getElement());
      super.onUnload();
    }
  }

  static class TestWidget extends Label {
    int onAttachOrder;
    int onLoadOrder;
    int onDetachOrder;
    int onUnloadOrder;
    boolean domAttachedOnLoad;
    boolean domAttachedOnUnload;

    @Override
    protected void onAttach() {
      onAttachOrder = ++orderIndex;
      super.onAttach();
    }

    @Override
    protected void onDetach() {
      onDetachOrder = ++orderIndex;
      super.onDetach();
    }

    @Override
    protected void onLoad() {
      domAttachedOnLoad = isElementAttached(getElement());
      onLoadOrder = ++orderIndex;
      super.onLoad();
    }

    @Override
    protected void onUnload() {
      onUnloadOrder = ++orderIndex;
      domAttachedOnUnload = isElementAttached(getElement());
      super.onUnload();
    }
  }

  static int orderIndex;

  static boolean isElementAttached(Element elem) {
    return DOM.isOrHasChild(RootPanel.getBodyElement(), elem);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testOnLoadAndUnloadOrder() {
    class TestAttachHandler implements AttachEvent.Handler {
      int delegateAttachOrder;
      int delegateDetachOrder;

      public void onAttach(AttachEvent event) {
        delegateAttachOrder = ++orderIndex;
      }
      public void onDetach(AttachEvent event) {
        delegateDetachOrder = ++orderIndex;
      }
    }

    TestPanel tp = new TestPanel();
    TestWidget tw = new TestWidget();
    TestAttachHandler ta = new TestAttachHandler();
    tw.addAttachHandler(ta);

    tp.add(tw);
    RootPanel.get().add(tp);
    RootPanel.get().remove(tp);

    // Trivial tests. Ensure that each panel/widget's onAttach/onDetach are
    // called before their associated onLoad/onUnload.
    assertTrue(tp.onAttachOrder < tp.onLoadOrder);
    assertTrue(tp.onDetachOrder < tp.onUnloadOrder);
    assertTrue(tw.onAttachOrder < tw.onLoadOrder);
    assertTrue(tw.onLoadOrder < ta.delegateAttachOrder);
    assertTrue(tw.onDetachOrder < tw.onUnloadOrder);
    assertTrue(tw.onUnloadOrder < ta.delegateDetachOrder);

    // Also trivial. Ensure that the panel's onAttach/onDetach is called before
    // its child's onAttach/onDetach.
    assertTrue(tp.onAttachOrder < tw.onAttachOrder);

    // Ensure that the panel's onLoad is only called after its widgets are
    // attached/loaded.
    assertTrue(tp.onLoadOrder > tw.onLoadOrder);

    // Ensure that the panel's onUnload is called before its widgets are
    // detached/unloaded.
    assertTrue(tp.onUnloadOrder < tw.onUnloadOrder);

    // Make sure each widget/panel's elements are actually still attached to the
    // DOM during onLoad/onUnload.
    assertTrue(tp.domAttachedOnLoad);
    assertTrue(tp.domAttachedOnUnload);
    assertTrue(tw.domAttachedOnLoad);
    assertTrue(tw.domAttachedOnUnload);
  }
}
