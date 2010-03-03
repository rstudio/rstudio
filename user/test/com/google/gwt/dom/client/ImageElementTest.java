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
package com.google.gwt.dom.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link ImageElement}.
 */
public class ImageElementTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * IE6 has a special implementation of {@link ImageElement#setSrc(String)}
   * that prevents the browser from loading uncached images multiple times. In
   * order to test this properly, the browser should be set to clear its cache
   * on exit.
   */
  public void testSetSrc() {
    // The parent element will actually load the image.
    ImageElement parent = Document.get().createImageElement();
    parent.setSrc("largeImage0.jpg");
    assertTrue(parent.getSrc().endsWith("largeImage0.jpg"));

    // The child element does not have its source set until the parent loads.
    ImageElement child = Document.get().createImageElement();
    child.setSrc("largeImage0.jpg");
    assertTrue(child.getSrc().endsWith("largeImage0.jpg"));

    child.setSrc("smallImage1.jpg");
    assertTrue(child.getSrc().endsWith("smallImage1.jpg"));
  }

  public void testSetSrcCloneParent() {
    // The parent element will actually load the image.
    ImageElement parent = Document.get().createImageElement();
    parent.setSrc("largeImage1.jpg");
    assertTrue(parent.getSrc().endsWith("largeImage1.jpg"));

    // The child element does not have its source set until the parent loads.
    ImageElement child = Document.get().createImageElement();
    child.setSrc("largeImage1.jpg");
    assertTrue(child.getSrc().endsWith("largeImage1.jpg"));

    // The parent clone will have its source set. We call setSrc to convert it
    // to a child.
    final ImageElement cloneParent = parent.cloneNode(true).cast();
    cloneParent.setSrc("largeImage1.jpg");
    assertTrue(cloneParent.getSrc().endsWith("largeImage1.jpg"));

    cloneParent.setSrc("smallImage1.jpg");
    assertTrue(cloneParent.getSrc().endsWith("smallImage1.jpg"));
  }

  public void testSetSrcCloneChild() {
    // The parent element will actually load the image.
    ImageElement parent = Document.get().createImageElement();
    parent.setSrc("largeImage2.jpg");
    assertTrue(parent.getSrc().endsWith("largeImage2.jpg"));

    // The child element does not have its source set until the parent loads.
    ImageElement child = Document.get().createImageElement();
    child.setSrc("largeImage2.jpg");
    assertTrue(child.getSrc().endsWith("largeImage2.jpg"));

    // The child clone will not have its source set. We call setSrc to ensure
    // it is registered as a child.
    final ImageElement cloneChild = parent.cloneNode(true).cast();
    cloneChild.setSrc("largeImage2.jpg");
    assertTrue(cloneChild.getSrc().endsWith("largeImage2.jpg"));

    cloneChild.setSrc("smallImage2.jpg");
    assertTrue(cloneChild.getSrc().endsWith("smallImage2.jpg"));
  }

}
