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
package com.google.gwt.resources.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.client.impl.ImageResourcePrototype;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests ImageResource generation.
 */
public class ImageResourceTest extends GWTTestCase {
  interface ExternalResources extends ClientBundle {
    @ImageOptions(preventInlining = true)
    @Source("16x16.png")
    ImageResource i16x16();

    @ImageOptions(preventInlining = true)
    @Source("32x32.png")
    ImageResource i32x32();    
  }

  interface Resources extends ClientBundle {
    @Source("animated.gif")
    ImageResource animated();

    /**
     * This image shouldn't be re-encoded as a PNG or it will dramatically
     * increase in size, although it's still small enough to be encoded as a
     * data URL as-is.
     */
    ImageResource complexLossy();

    @Source("16x16.png")
    ImageResource i16x16();

    @Source("16x16.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource i16x16Horizontal();

    @Source("16x16.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource i16x16Vertical();

    @Source("32x32.png")
    ImageResource i32x32();

    @Source("32x32.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource i32x32Horizontal();

    @Source("32x32.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical)
    ImageResource i32x32Vertical();

    @Source("64x64.png")
    ImageResource i64x64();

    @Source("64x64.png")
    ImageResource i64x64Dup();

    @Source("64x64-dup.png")
    ImageResource i64x64Dup2();

    // Test default filename lookup while we're at it
    ImageResource largeLossless();

    // Test default filename lookup while we're at it
    ImageResource largeLossy();
  
    @Source("64x64.png")
    @ImageOptions(width = 32)
    ImageResource scaledDown();
    
    @Source("64x64.png")
    @ImageOptions(width = 128)
    ImageResource scaledUp();
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.resources.Resources";
  }

  public void testAnimated() {
    Resources r = GWT.create(Resources.class);

    ImageResource a = r.animated();

    assertTrue(a.isAnimated());
    assertFalse(((ImageResourcePrototype) a).isLossy());
    assertEquals(16, a.getWidth());
    assertEquals(16, a.getHeight());
    assertEquals(0, a.getLeft());
    assertEquals(0, a.getTop());

    // Make sure the animated image is encoded separately
    assertFalse(a.getSafeUri().equals(r.i16x16().getSafeUri()));
  }

  public void testDedup() {
    Resources r = GWT.create(Resources.class);

    ImageResource a = r.i64x64();
    ImageResource b = r.i64x64Dup();
    ImageResource c = r.i64x64Dup2();
    assertEquals(64, a.getHeight());
    assertEquals(64, a.getWidth());

    assertEquals(a.getLeft(), b.getLeft());
    assertEquals(a.getLeft(), c.getLeft());

    assertEquals(a.getTop(), b.getTop());
    assertEquals(a.getTop(), c.getTop());

    delayTestFinish(10000);
    // See if the size of the image strip is what we expect
    Image i = new Image(a.getSafeUri());
    i.addLoadHandler(new LoadHandler() {
      public void onLoad(LoadEvent event) {
        finishTest();
      }
    });
    i.addErrorHandler(new ErrorHandler() {
      public void onError(ErrorEvent event) {
        fail("ErrorEvent");
      }
    });

    RootPanel.get().add(i);
  }

  public void testPacking() {
    Resources r = GWT.create(Resources.class);

    ImageResource i64 = r.i64x64();
    ImageResource lossy = r.largeLossy();
    ImageResource lossless = r.largeLossless();

    assertFalse(((ImageResourcePrototype) lossless).isLossy());

    // Make sure that the large, lossy image isn't bundled with the rest
    assertTrue(((ImageResourcePrototype) lossy).isLossy());
    assertTrue(!i64.getSafeUri().equals(lossy.getSafeUri()));

    assertEquals(16, r.i16x16Vertical().getWidth());
    assertEquals(16, r.i16x16Vertical().getHeight());

    assertEquals(16, r.i16x16Horizontal().getWidth());
    assertEquals(16, r.i16x16Horizontal().getHeight());

    // Check scaling and aspect ratio
    assertEquals(32, r.scaledDown().getWidth());
    assertEquals(32, r.scaledDown().getHeight());
    assertEquals(128, r.scaledUp().getWidth());
    assertEquals(128, r.scaledUp().getHeight());
  }

  public void testPreventInlining() {
    ExternalResources r = GWT.create(ExternalResources.class);
    ImageResource a = r.i16x16();
    ImageResource b = r.i32x32();

    // Should never be a data URL
    assertFalse(a.getURL().startsWith("data:"));
    assertFalse(b.getURL().startsWith("data:"));
    // Should be fetched from different URLs
    assertFalse(a.getURL().equals(b.getURL()));

    // No image packing
    assertEquals(0, a.getTop());
    assertEquals(0, a.getLeft());
    assertEquals(0, b.getTop());
    assertEquals(0, b.getLeft());
  }
  
  @SuppressWarnings("deprecation")
  public void testSafeUri() {
    Resources r = GWT.create(Resources.class);

    assertEquals(r.i64x64().getSafeUri().asString(), r.i64x64().getURL());
  }
}
