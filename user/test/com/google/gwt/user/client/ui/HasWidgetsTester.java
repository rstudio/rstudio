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

import java.util.Iterator;
import java.util.Set;

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
  static class DefaultWidgetAdder implements WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      container.add(child);
    }
  }

  private static class TestWidget extends Widget {
    TestWidget() {
      setElement(DOM.createDiv());
    }

    @Override
    protected void onLoad() {
      // During onLoad, isAttached must be true, and the element be a descendant
      // of the body element.
      Assert.assertTrue(isAttached());
      Assert.assertTrue(DOM.isOrHasChild(RootPanel.getBodyElement(),
          getElement()));
    }

    @Override
    protected void onUnload() {
      // During onUnload, everything must *still* be attached.
      Assert.assertTrue(isAttached());
      Assert.assertTrue(DOM.isOrHasChild(RootPanel.getBodyElement(),
          getElement()));
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
    testAll(container, adder, false);
  }

  /**
   * Runs all tests for {@link HasWidgets}. It is recommended that tests call
   * this method or {@link #testAll(HasWidgets, WidgetAdder)} so that future
   * tests are automatically included.
   * 
   * @param container the container widget to test
   * @param adder the method of adding children
   * @param supportsMultipleWidgets true if container supports multiple children
   */
  static void testAll(HasWidgets container, WidgetAdder adder,
      boolean supportsMultipleWidgets) {
    testAttachDetachOrder(container, adder);
    testRemovalOfNonExistantChild(container);
    testDoAttachChildrenWithError(container, adder, supportsMultipleWidgets);
    testDoDetachChildrenWithError(container, adder, supportsMultipleWidgets);
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
    resetContainer(container);

    // Make sure the container's attached.
    Assert.assertTrue(container instanceof Widget);
    RootPanel.get().add((Widget) container);

    // Adding and removing the test widget will cause it to test onLoad and
    // onUnload order.
    TestWidget widget = new TestWidget();
    adder.addChild(container, widget);
    Assert.assertTrue(widget.isAttached());
    Assert.assertTrue(DOM.isOrHasChild(RootPanel.getBodyElement(),
        widget.getElement()));
    container.remove(widget);

    // After removal, the widget should be detached.
    Assert.assertFalse(widget.isAttached());
    Assert.assertFalse(DOM.isOrHasChild(RootPanel.getBodyElement(),
        widget.getElement()));
  }

  /**
   * Ensures that the physical and logical state of children are consistent even
   * if one of the children throws an error in onLoad.
   * 
   * @param container the container
   * @param adder the method of adding children
   * @param supportMultipleWidgets true if container supports multiple widgets
   */
  static void testDoAttachChildrenWithError(HasWidgets container,
      WidgetAdder adder, boolean supportMultipleWidgets) {
    resetContainer(container);

    // Create a widget that will throw an exception onLoad.
    BadWidget badWidget = new BadWidget();
    badWidget.setFailOnLoad(true);

    // Add some children to a panel.
    if (supportMultipleWidgets) {
      adder.addChild(container, new Label("test0"));
      adder.addChild(container, new Label("test1"));
      adder.addChild(container, badWidget);
      adder.addChild(container, new Label("test2"));
      adder.addChild(container, new Label("test3"));
    } else {
      adder.addChild(container, badWidget);
    }
    Assert.assertFalse(badWidget.isAttached());

    // Attach the widget.
    try {
      RootPanel.get().add((Widget) container);
    } catch (AttachDetachException e) {
      // Expected.
      Set<Throwable> causes = e.getCauses();
      Assert.assertEquals(1, causes.size());
      Throwable[] throwables = causes.toArray(new Throwable[1]);
      // Composites that use internal panels for layout (eg. TabPanel) will
      // throws the AttachDetachException from the inner panel instead of an
      // IllegalArgumentException from the bad widget
      Assert.assertTrue(throwables[0] instanceof IllegalArgumentException
          || throwables[0] instanceof AttachDetachException);
    }
    Iterator<Widget> children = container.iterator();
    while (children.hasNext()) {
      Widget w = children.next();
      Assert.assertTrue(w.isAttached());
      assertContainerIsOrHasChild(container, w);
    }
    Assert.assertEquals(RootPanel.get(), ((Widget) container).getParent());

    // Detach the panel.
    RootPanel.get().remove((Widget) container);
    Assert.assertFalse(badWidget.isAttached());
  }

  /**
   * Ensures that the physical and logical state of children are consistent even
   * if one of the children throws an error in onUnload.
   * 
   * @param container the container
   * @param adder the method of adding children
   * @param supportMultipleWidgets true if container supports multiple widgets
   */
  static void testDoDetachChildrenWithError(HasWidgets container,
      WidgetAdder adder, boolean supportMultipleWidgets) {
    resetContainer(container);

    // Create a widget that will throw an exception onUnload.
    BadWidget badWidget = new BadWidget();
    badWidget.setFailOnUnload(true);

    // Add some children to a panel.
    if (supportMultipleWidgets) {
      adder.addChild(container, new Label("test0"));
      adder.addChild(container, new Label("test1"));
      adder.addChild(container, badWidget);
      adder.addChild(container, new Label("test2"));
      adder.addChild(container, new Label("test3"));
    } else {
      adder.addChild(container, badWidget);
    }
    Assert.assertFalse(badWidget.isAttached());

    // Attach the widget.
    RootPanel.get().add((Widget) container);
    Assert.assertTrue(badWidget.isAttached());

    try {
      RootPanel.get().remove((Widget) container);
    } catch (AttachDetachException e) {
      // Expected.
      Set<Throwable> causes = e.getCauses();
      Assert.assertEquals(1, causes.size());
      Throwable[] throwables = causes.toArray(new Throwable[1]);
      // Composites that use internal panels for layout (eg. TabPanel) will
      // throws the AttachDetachException from the inner panel instead of an
      // IllegalArgumentException from the bad widget
      Assert.assertTrue(throwables[0] instanceof IllegalArgumentException
          || throwables[0] instanceof AttachDetachException);
    }
    Iterator<Widget> children = container.iterator();
    while (children.hasNext()) {
      Widget w = children.next();
      Assert.assertFalse(w.isAttached());
      assertContainerIsOrHasChild(container, w);
    }
    Assert.assertNull(((Widget) container).getParent());
  }

  /**
   * Tests to ensure that {@link HasWidgets#remove(Widget)} is resilient to
   * being called with a widget that is not present as a child in the container.
   * 
   * @param container
   */
  static void testRemovalOfNonExistantChild(HasWidgets container) {
    resetContainer(container);
    TestWidget widget = new TestWidget();
    container.remove(widget);
  }

  /**
   * Assert that the container is a parent of the child. Some Panels are not the
   * direct parent of their children, so we walk up the chain looking for a
   * parent.
   * 
   * @param container
   * @param child
   */
  private static void assertContainerIsOrHasChild(HasWidgets container,
      Widget child) {
    boolean containerIsOrHasChild = false;
    Widget parent = child.getParent();
    while (parent != null && !containerIsOrHasChild) {
      if (parent == container) {
        containerIsOrHasChild = true;
      }
      parent = parent.getParent();
    }
    Assert.assertTrue(containerIsOrHasChild);
  }

  /**
   * Reset the container between tests.
   * 
   * @param container the container
   */
  private static void resetContainer(HasWidgets container) {
    container.clear();
    if (((Widget) container).isAttached()) {
      RootPanel.get().remove((Widget) container);
    }
  }
}
