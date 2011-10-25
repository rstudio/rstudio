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
package com.google.gwt.junit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWTBridge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Defangs {@link GWT#create(Class)} to allow unit tests to mock out Widgets and
 * other UIObjects.
 */
public class GWTMockUtilities {
  
  private static GWTDummyBridge dummyBridge;

  /**
   * Replace the normal GWT.create() behavior with a method that returns null
   * instead of throwing a runtime exception. This is to allow JUnit tests to
   * mock classes that make GWT.create() calls in their static initializers.
   * This is not for use with GWTTestCase, and is not for use testing widgets
   * themselves. Rather, it is to allow pure java unit tests of classes that
   * need to manipulate widgets.
   * 
   * <p>
   * <b>NOTE:</b> Be sure to call {@link #restore} in your tearDown
   * method, to avoid confusing downstream tests.
   * 
   * <p>
   * Sample use:
   * 
   * <pre>&#64;Override
   * public void setUp() throws Exception {
   *   super.setUp();
   *   GWTMockUtilities.disarm();
   * }
   *
   * &#64;Override
   * public void tearDown() {
   *   GWTMockUtilities.restore();
   * }
   * 
   * public void testSomething() {
   *   MyStatusWidget mock = EasyMock.createMock(MyStatusWidget.class);
   *   EasyMock.expect(mock.setText("expected text"));
   *   EasyMock.replay(mock);
   *   
   *   StatusController controller = new StatusController(mock);
   *   controller.setStatus("expected text");
   *   
   *   EasyMock.verify(mock);
   * }
   * </pre>
   */
  public static void disarm() {
    dummyBridge = new GWTDummyBridge();
    setGwtBridge(dummyBridge);
  }

  public static void restore() {
    setGwtBridge(null);
    dummyBridge = null;
  }
  
  /**
   * After {@see #disarm()} replaces the normal GWT.create(), this method
   * enables Messages to be faked out. The fake instance creation is delegated
   * to {@see FakeMessagesMaker}.  
   */
  public static void returnMockMessages() {
    if (dummyBridge != null) {
      dummyBridge.enableMockMessages();
    }
  }

  /**
   * Install the given instance of {@link GWTBridge}, allowing it to override
   * the behavior of calls to {@link GWT#create(Class)}.
   */
  private static void setGwtBridge(GWTBridge bridge) {
    Class<GWT> gwtClass = GWT.class;
    Class<?>[] paramTypes = new Class[] {GWTBridge.class};
    Method setBridgeMethod = null;
    try {
      setBridgeMethod = gwtClass.getDeclaredMethod("setBridge", paramTypes);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    setBridgeMethod.setAccessible(true);
    try {
      setBridgeMethod.invoke(gwtClass, new Object[] {bridge});
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
