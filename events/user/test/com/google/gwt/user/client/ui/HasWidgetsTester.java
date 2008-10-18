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

import com.google.gwt.user.client.DOM;

import junit.framework.Assert;

/**
 * All test cases for widgets that implement HasWidgets should derive from this
 * test case, and make sure to run all of its test templates.
 */
public abstract class HasWidgetsTester {

  /**
   * Used in test templates to allow the child class to specify how a widget
   * will be added to its container. This is necessary because
   * {@link HasWidgets#add(Widget)} is allowed to throw
   * {@link UnsupportedOperationException}.
   */
  interface WidgetAdder {

    /**
     * Adds the specified child to a container.
     */
    void addChild(HasWidgets container, Widget child);
  }

  /**
   * Default implementation used by containers for which
   * {@link HasWidgets#add(Widget)} will not throw an exception.
   */
  private static class DefaultWidgetAdder implements WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      container.add(child);
    }
  }

  private static class TestWidget extends Widget {
    TestWidget() {
      setElement(DOM.createDiv());
    }

    protected void onLoad() {
      // During onLoad, isAttached must be true, and the element be a descendant
      // of the body element.
      Assert.assertTrue(isAttached());
      Assert.assertTrue(DOM.isOrHasChild(RootPanel.getBodyElement(), getElement()));
    }

    protected void onUnload() {
      // During onUnload, everything must *still* be attached.
      Assert.assertTrue(isAttached());
      Assert.assertTrue(DOM.isOrHasChild(RootPanel.getBodyElement(), getElement()));
    }
  }

  /**
   * Runs all tests for {@link HasWidgets}. It is recommended that tests call
   * this method or {@link #testAll(HasWidgets, WidgetAdder} so that future
   * tests are automatically included.
   * 
   * @param container
   */
  static void testAll(HasWidgets container) {
    testAll(container, new DefaultWidgetAdder());
  }

  /**
   * Runs all tests for {@link HasWidgets}. It is recommended that tests call
   * this method or {@link #testAll(HasWidgets, WidgetAdder)} so that future
   * tests are automatically included.
   * 
   * @param container
   * @param adder
   */
  static void testAll(HasWidgets container, WidgetAdder adder) {
    testAttachDetachOrder(container, adder);
    testRemovalOfNonExistantChild(container);
  }

  /**
   * Tests attach and detach order, assuming that the container's
   * {@link HasWidgets#add(Widget)} method does not throw
   * {@link UnsupportedOperationException}.
   * 
   * @param test
   * @param container
   * @see #testAttachDetachOrder(TestCase, HasWidgets,
   *      com.google.gwt.user.client.ui.HasWidgetsTester.WidgetAdder)
   */
  static void testAttachDetachOrder(HasWidgets container) {
    testAttachDetachOrder(container, new DefaultWidgetAdder());
  }

  /**
   * Ensures that children are attached and detached in the proper order. This
   * must result in the child's onLoad() method being called just *after* its
   * element is attached to the DOM, and its onUnload method being called just
   * *before* its element is detached from the DOM.
   */
  static void testAttachDetachOrder(HasWidgets container, WidgetAdder adder) {
    // Make sure the container's attached.
    Assert.assertTrue(container instanceof Widget);
    RootPanel.get().add((Widget) container);

    // Adding and removing the test widget will cause it to test onLoad and
    // onUnload order.
    TestWidget widget = new TestWidget();
    adder.addChild(container, widget);
    Assert.assertTrue(widget.isAttached());
    Assert.assertTrue(DOM.isOrHasChild(RootPanel.getBodyElement(), widget.getElement()));
    container.remove(widget);

    // After removal, the widget should be detached.
    Assert.assertFalse(widget.isAttached());
    Assert.assertFalse(DOM.isOrHasChild(RootPanel.getBodyElement(), widget.getElement()));
  }

  /**
   * Tests to ensure that {@link HasWidgets#remove(Widget)} is resilient to
   * being called with a widget that is not present as a child in the container.
   * 
   * @param container
   */
  static void testRemovalOfNonExistantChild(HasWidgets container) {
    TestWidget widget = new TestWidget();
    container.remove(widget);
  }
}
