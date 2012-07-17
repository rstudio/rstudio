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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageTest;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests for the ClippedImagePrototype implementation. Tests are done to ensure
 * that clipped images are generated which match the prototype's specification,
 * and that applications of the prototype to existing images in both
 * clipped/unclipped mode change the image so that it matches the prototype.
 * Tests are also done to ensure that load events fire correctly after the
 * application of the prototype to the image.
 */
public class ClippedImagePrototypeTest extends GWTTestCase {
  private static class TestLoadHandler implements LoadHandler {
    private int onloadEventFireCount = 0;

    public int getOnloadEventFireCount() {
      return onloadEventFireCount;
    }

    @Override
    public void onLoad(LoadEvent event) {
      onloadEventFireCount++;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  /**
   * Tests that a clipped image can be transformed to match a given prototype.
   * Also checks to make sure that a load event is fired on when
   * {@link com.google.gwt.user.client.ui.impl.ClippedImagePrototype#applyTo(com.google.gwt.user.client.ui.Image)}
   * is called.
   *
   * TODO(jlabanca): Enable this test when issue 863 is fixed
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void disabledTestApplyToClippedImage() {
    final Image image = new Image("counting-backwards.png", 12, 13, 8, 8);

    assertEquals(12, image.getOriginLeft());
    assertEquals(13, image.getOriginTop());
    assertEquals(8, image.getWidth());
    assertEquals(8, image.getHeight());
    assertEquals("clipped", ImageTest.getCurrentImageStateName(image));

    final TestLoadHandler handler = new TestLoadHandler() {
      @Override
      public void onLoad(LoadEvent event) {
        super.onLoad(event);

        if (image.getOriginLeft() == 12 && image.getOriginTop() == 13) {
          ClippedImagePrototype clippedImagePrototype = new ClippedImagePrototype(
              UriUtils.fromString("counting-forwards.png"), 16, 16, 16, 16);

          clippedImagePrototype.applyTo(image);

          assertEquals(16, image.getOriginLeft());
          assertEquals(16, image.getOriginTop());
          assertEquals(16, image.getWidth());
          assertEquals(16, image.getHeight());
          assertEquals("clipped", ImageTest.getCurrentImageStateName(image));
        }
      }
    };
    image.addLoadHandler(handler);
    image.addErrorHandler(new ErrorHandler() {
      @Override
      public void onError(ErrorEvent event) {
        fail("The image " + image.getUrl() + " failed to load.");
      }
    });

    RootPanel.get().add(image);
    delayTestFinish(2000);

    Timer t = new Timer() {
      @Override
      public void run() {
        assertEquals(2, handler.getOnloadEventFireCount());
        finishTest();
      }
    };

    t.schedule(1000);
  }

  /**
   * Tests that an unclipped image can be transformed to match a given
   * prototype. Also checks to make sure that a load event is fired on when
   * <code>applyTo(Image)</code> is called.
   */
  /*
   * This test has been commented out because of issue #863
   *
   * public void testApplyToUnclippedImage() {
   * final Image image = new Image("counting-backwards.png");
   *
   * assertEquals(0, image.getOriginLeft()); assertEquals(0,
   * image.getOriginTop()); assertEquals("unclipped",
   * ImageTest.getCurrentImageStateName(image));
   *
   * final ArrayList onloadEventFireCounter = new ArrayList();
   *
   * image.addLoadListener(new LoadListener() { public void onError(Widget
   * sender) { fail("The image " + ((Image) sender).getUrl() + " failed to
   * load."); }
   *
   * public void onLoad(Widget sender) { onloadEventFireCounter.add(new
   * Object());
   *
   * if (ImageTest.getCurrentImageStateName(image).equals("unclipped")) {
   *
   * assertEquals(32, image.getWidth()); assertEquals(32, image.getHeight());
   *
   * ClippedImagePrototype clippedImagePrototype = new
   * ClippedImagePrototype("counting-forwards.png", 16, 16, 16, 16);
   *
   * clippedImagePrototype.adjust(image);
   *
   * assertEquals(16, image.getOriginLeft()); assertEquals(16,
   * image.getOriginTop()); assertEquals(16, image.getWidth()); assertEquals(16,
   * image.getHeight()); assertEquals("counting-forwards.png", image.getUrl());
   * assertEquals("clipped", ImageTest.getCurrentImageStateName(image)); } } });
   *
   * RootPanel.get().add(image); delayTestFinish(2000);
   *
   * Timer t = new Timer() { public void run() { assertEquals(2,
   * onloadEventFireCounter.size()); finishTest(); } };
   *
   * t.schedule(1000); }
   */

  /**
   * Tests that new clipped images can be generated from a prototype by calling
   * {@link ClippedImagePrototype#createImage()}.
   */
  public void testGenerateNewImage() {
    ClippedImagePrototype clippedImagePrototype = new ClippedImagePrototype(
        UriUtils.fromString("counting-forwards.png"), 16, 16, 16, 16);

    Image image = clippedImagePrototype.createImage();

    RootPanel.get().add(image);

    assertEquals(16, image.getOriginLeft());
    assertEquals(16, image.getOriginTop());
    assertEquals(16, image.getWidth());
    assertEquals(16, image.getHeight());
    assertEquals("counting-forwards.png", image.getUrl());
    assertEquals("clipped", ImageTest.getCurrentImageStateName(image));
  }

  /**
   * Tests that making clipped images draggable works as intended.
   */
  public void testMakingClippedImagesDraggable() {
    ClippedImagePrototype clippedImage = new ClippedImagePrototype(
        UriUtils.fromString("test.png"), 0, 0, 0, 0);

    // check that at first the outputted HTML does not contain draggable='true'
    assertFalse(clippedImage.getSafeHtml().asString().contains("draggable"));

    // set the image to draggable and check that the outputted HTML now contains draggable='true'
    clippedImage.setDraggable(true);
    assertTrue(clippedImage.getSafeHtml().asString().contains("draggable"));

    // revert it to non-draggable and check that draggable='true' is gone
    clippedImage.setDraggable(false);
    assertFalse(clippedImage.getSafeHtml().asString().contains("draggable"));
  }
}
