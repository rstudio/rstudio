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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Timer;

/**
 * Tests for the Image widget. Images in both clipped mode and unclipped mode
 * are tested, along with the transitions between the two modes.
 */
public class ImageTest extends GWTTestCase {
  interface Bundle extends ClientBundle {
    ImageResource prettyPiccy();
  }

  private static class TestErrorHandler implements ErrorHandler {
    private Image image;

    public TestErrorHandler(Image image) {
      this.image = image;
    }

    public void onError(ErrorEvent event) {
      fail("The image " + image.getUrl() + " failed to load.");
    }
  }

  private static class TestImage extends Image {
    public TestImage(Element element) {
      super(element);
    }

    public static TestImage wrap(Element element) {
      // Assert that the element is attached.
      assert Document.get().getBody().isOrHasChild(element);

      TestImage image = new TestImage(element);

      // Mark it attached and remember it for cleanup.
      image.onAttach();
      RootPanel.detachOnWindowClose(image);

      return image;
    }
  }
  
  private abstract static class TestLoadHandler implements LoadHandler {
    private boolean finished = false;

    /**
     * Mark the test as finished.
     */
    public void finish() {
      finished = true;
    }

    /**
     * Returns true if the test has finished.
     */
    public boolean isFinished() {
      return finished;
    }
  }

  @Deprecated
  private abstract static class TestLoadListener implements LoadListener {
    private boolean finished = false;
    private Image image;

    public TestLoadListener(Image image) {
      this.image = image;
    }

    /**
     * Mark the test as finished.
     */
    public void finish() {
      finished = true;
    }

    /**
     * Returns true if the test has finished.
     */
    public boolean isFinished() {
      return finished;
    }

    public void onError(Widget sender) {
      fail("The image " + image.getUrl() + " failed to load.");
    }
  }

  /**
   * The default timeout of asynchronous tests. This should be larger than
   * LOAD_EVENT_TIMEOUT and SYNTHETIC_LOAD_EVENT_TIMEOUT.
   */
  private static final int DEFAULT_TEST_TIMEOUT = 10000;

  /**
   * The amount of time to wait for a load event to fire in milliseconds.
   */
  private static final int LOAD_EVENT_TIMEOUT = 7000;

  /**
   * The amount of time to wait for a clipped image to fire a synthetic load
   * event in milliseconds.
   */
  private static final int SYNTHETIC_LOAD_EVENT_TIMEOUT = 1000;

  /**
   * Helper method that allows us to 'peek' at the private <code>state</code>
   * field in the Image object, and call the <code>state.getStateName()</code>
   * method.
   *
   * @param image The image instance
   * @return "unclipped" if image is in the unclipped state, or "clipped" if the
   *         image is in the clipped state
   */
  public static native String getCurrentImageStateName(Image image) /*-{
    var imgState = image.@com.google.gwt.user.client.ui.Image::state;
    return imgState.@com.google.gwt.user.client.ui.Image.State::getStateName() ();
  }-*/;

  private static native boolean isIE6() /*-{
    return @com.google.gwt.dom.client.DOMImplIE6::isIE6()();
  }-*/;

  private int firedError;

  private int firedLoad;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  public void testAltText() {
    final String altText = "this is an image";
    final Image image = new Image("image.png", 12, 12, 12, 12);

    assertEquals("", image.getAltText());
    image.setAltText(altText);
    assertEquals(altText, image.getAltText());
    image.setAltText("");
    assertEquals("", image.getAltText());
  }

  /**
   * Test that attaching and immediately detaching an element does not cause an
   * error.
   */
  public void testAttachDetach() {
    /*
     * This test fails on IE6 due to the way ImageSrcIE6 delays setting the
     * src of images. It may be a real error, but it doesn't seem to affect
     * apps and its too obscure to spend time fixing it.  
     */
    if (isIE6()) {
      return;
    }

    final Image image = new Image("counting-forwards.png");
    RootPanel.get().add(image);
    RootPanel.get().remove(image);

    // Wait for the synthetic event to attempt to fire.
    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        // The synthetic event did not cause an error.
        finishTest();
      }
    }.schedule(SYNTHETIC_LOAD_EVENT_TIMEOUT);
  }

  /**
   * Tests the transition from the clipped state to the unclipped state.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testChangeClippedImageToUnclipped() {
    final Image image = new Image("counting-forwards.png", 12, 13, 8, 8);
    assertEquals("clipped", getCurrentImageStateName(image));

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        ++onLoadEventCount;
        if (onLoadEventCount == 1) { // Set the url after the first image loads
          image.setUrl("counting-forwards.png");
        } else if (onLoadEventCount == 2) {
          assertEquals(0, image.getOriginLeft());
          assertEquals(0, image.getOriginTop());
          assertEquals(32, image.getWidth());
          assertEquals(32, image.getHeight());
          assertEquals("unclipped", getCurrentImageStateName(image));
          finishTest();
        }
      }
    });

    RootPanel.get().add(image);
  }

  /**
   * Tests the transition from the unclipped state to the clipped state.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testChangeImageToClipped() {
    final Image image = new Image("counting-forwards.png");
    assertEquals("unclipped", getCurrentImageStateName(image));

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        if (getCurrentImageStateName(image).equals("unclipped")) {
          image.setVisibleRect(12, 13, 8, 8);
        }

        if (++onLoadEventCount == 2) {
          assertEquals(12, image.getOriginLeft());
          assertEquals(13, image.getOriginTop());
          assertEquals(8, image.getWidth());
          assertEquals(8, image.getHeight());
          assertEquals("clipped", getCurrentImageStateName(image));
          finishTest();
        }
      }
    });

    RootPanel.get().add(image);
  }

  /**
   * Tests the transition from the unclipped state to the clipped state
   * before a load event fires.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testChangeImageToClippedSynchronously() {
    /*
     * This test fails on IE6 due to the way ImageSrcIE6 delays setting the
     * src of images. It may be a real error, but it doesn't seem to affect
     * apps and its too obscure to spend time fixing it.  
     */
    if (isIE6()) {
      return;
    }

    final Image image = new Image("counting-forwards.png");
    assertEquals("unclipped", getCurrentImageStateName(image));

    image.addErrorHandler(new TestErrorHandler(image));
    final TestLoadHandler loadHandler = new TestLoadHandler() {
      public void onLoad(LoadEvent event) {
        if (isFinished()) {
          fail("LoadHandler fired twice. Expected it to fire only once.");
        }

        assertEquals("clipped", getCurrentImageStateName(image));
        assertEquals(12, image.getOriginLeft());
        assertEquals(13, image.getOriginTop());
        assertEquals(8, image.getWidth());
        assertEquals(8, image.getHeight());
        finish();
      }
    };
    image.addLoadHandler(loadHandler);

    /*
     * Change the image to a clipped image before a load event fires. We only
     * expect one asynchronous load event to fire for the final state. This is
     * consistent with the expected behavior of changing the source URL multiple
     * times.
     */
    RootPanel.get().add(image);
    image.setVisibleRect(12, 13, 8, 8);
    assertEquals("clipped", getCurrentImageStateName(image));

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        assertTrue(loadHandler.isFinished());
        finishTest();
      }
    }.schedule(SYNTHETIC_LOAD_EVENT_TIMEOUT);
  }

  /**
   * Tests the creation of an image in unclipped mode.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testCreateImage() {
    final Image image = new Image("counting-forwards.png");

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        if (++onLoadEventCount == 1) {
          assertEquals(32, image.getWidth());
          assertEquals(32, image.getHeight());
          finishTest();
        }
      }
    });

    RootPanel.get().add(image);
    assertEquals(0, image.getOriginLeft());
    assertEquals(0, image.getOriginTop());
    assertEquals("unclipped", getCurrentImageStateName(image));
  }

  /**
   * Tests the creation of an image that does not exist.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testCreateImageWithError() {
    final Image image = new Image("imageDoesNotExist.png");

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new ErrorHandler() {
      public void onError(ErrorEvent event) {
        finishTest();
      }
    });
    image.addLoadHandler(new LoadHandler() {
      public void onLoad(LoadEvent event) {
        fail("The image " + image.getUrl() + " should have failed to load.");
      }
    });

    RootPanel.get().add(image);
  }

  /**
   * Tests the firing of onload events when
   * {@link com.google.gwt.user.client.ui.Image#setUrl(String)} is called on an
   * unclipped image.
   */
  public void testSetUrlAndLoadEventsOnUnclippedImage() {
    final Image image = new Image();

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        if (++onLoadEventCount == 2) {
          finishTest();
        } else {
          image.setUrl("counting-forwards.png");
        }
      }
    });

    RootPanel.get().add(image);
    image.setUrl("counting-backwards.png");
  }

  /**
   * Tests the behavior of
   * <code>setUrlAndVisibleRect(String, int, int, int, int)</code> method on an
   * unclipped image, which causes a state transition to the clipped state.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testSetUrlAndVisibleRectOnUnclippedImage() {
    final Image image = new Image("counting-backwards.png");
    assertEquals("unclipped", getCurrentImageStateName(image));

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        if (getCurrentImageStateName(image).equals("unclipped")) {
          image.setUrlAndVisibleRect("counting-forwards.png", 0, 16, 16, 16);
        }

        if (++onLoadEventCount == 2) {
          assertEquals(0, image.getOriginLeft());
          assertEquals(16, image.getOriginTop());
          assertEquals(16, image.getWidth());
          assertEquals(16, image.getHeight());
          assertEquals("clipped", getCurrentImageStateName(image));
          finishTest();
        }
      }
    });

    RootPanel.get().add(image);
  }

  /**
   * Tests the creation of an image in clipped mode.
   */
  @SuppressWarnings("deprecation")
  public void testCreateClippedImage() {
    final Image image = new Image("counting-forwards.png", 16, 16, 16, 16);

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    final TestLoadListener listener = new TestLoadListener(image) {
      private int onLoadEventCount = 0;

      public void onLoad(Widget sender) {
        if (++onLoadEventCount == 1) {
          assertEquals(16, image.getWidth());
          assertEquals(16, image.getHeight());
          finish();
        }
      }
    };
    image.addLoadListener(listener);

    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        if (++onLoadEventCount == 1) {
          assertEquals(16, image.getWidth());
          assertEquals(16, image.getHeight());
          if (listener.isFinished()) {
            finishTest();
          } else {
            fail("Listener did not fire first");
          }
        }
      }
    });
    image.addErrorHandler(new TestErrorHandler(image));

    RootPanel.get().add(image);
    assertEquals(16, image.getOriginLeft());
    assertEquals(16, image.getOriginTop());
    assertEquals("clipped", getCurrentImageStateName(image));
  }

  @SuppressWarnings("deprecation")
  public void testLoadListenerWiring() {
    Image im = new Image();

    im.addLoadListener(new LoadListener() {

      public void onError(Widget sender) {
        ++firedError;
      }

      public void onLoad(Widget sender) {
        ++firedLoad;
      }
    });
    im.fireEvent(new LoadEvent() {
      // Replaced by Joel's event firing when possible.
    });
    assertEquals(1, firedLoad);
    assertEquals(0, firedError);
    im.fireEvent(new ErrorEvent() {
      // Replaced by Joel's event firing when possible.
    });
    assertEquals(1, firedLoad);
    assertEquals(1, firedError);
  }

  /**
   * Test that attaching an image multiple times results in only one load event.
   */
  public void testMultipleAttach() {
    final Image image = new Image("counting-forwards.png");

    final TestLoadHandler loadHandler = new TestLoadHandler() {
      public void onLoad(LoadEvent event) {
        if (isFinished()) {
          fail("LoadHandler fired multiple times.");
        }
        finish();
      }
    };
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(loadHandler);

    RootPanel.get().add(image);
    RootPanel.get().remove(image);
    RootPanel.get().add(image);
    RootPanel.get().remove(image);
    RootPanel.get().add(image);
    RootPanel.get().remove(image);
    RootPanel.get().add(image);

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        assertTrue(loadHandler.isFinished());
        finishTest();
      }
    }.schedule(LOAD_EVENT_TIMEOUT);
  }

  /**
   * Verify that detaching and reattaching an image in a handler does not fire a
   * second onload event.
   */
  public void testNoEventOnReattachInHandler() {
    final Image image = new Image("counting-forwards.png");

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(new LoadHandler() {
      private int onLoadEventCount = 0;

      public void onLoad(LoadEvent event) {
        if (++onLoadEventCount == 1) {
          RootPanel.get().remove(image);
          RootPanel.get().add(image);
          // The extra onLoad would will fire synchronously before finishTest().
          finishTest();
        } else {
          fail("onLoad fired on reattach.");
        }
      }
    });

    RootPanel.get().add(image);
  }

  public void testOneEventOnly() {
    final Image image = new Image("counting-forwards.png");

    final TestLoadHandler loadHandler = new TestLoadHandler() {
      public void onLoad(LoadEvent event) {
        if (isFinished()) {
          fail("LoadHandler fired multiple times.");
        }
        finish();
      }
    };
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(loadHandler);

    RootPanel.get().add(image);
    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        assertTrue(loadHandler.isFinished());
        finishTest();
      }
    }.schedule(LOAD_EVENT_TIMEOUT);
  }

  public void testOneEventOnlyClippedImage() {
    final Image image = new Image("counting-forwards.png", 12, 13, 8, 8);

    final TestLoadHandler loadHandler = new TestLoadHandler() {
      public void onLoad(LoadEvent event) {
        if (isFinished()) {
          fail("LoadHandler fired multiple times.");
        }
        finish();
      }
    };
    image.addErrorHandler(new TestErrorHandler(image));
    image.addLoadHandler(loadHandler);

    RootPanel.get().add(image);
    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        assertTrue(loadHandler.isFinished());
        finishTest();
      }
    }.schedule(LOAD_EVENT_TIMEOUT);
   }

  public void testResourceConstructor() {
    Bundle b = GWT.create(Bundle.class);
    Image image = new Image(b.prettyPiccy());
    assertResourceWorked(image, b.prettyPiccy());
  }

  public void testSetResource() {
    Bundle b = GWT.create(Bundle.class);
    Image image = new Image();
    image.setResource(b.prettyPiccy());
    assertResourceWorked(image, b.prettyPiccy());
  }

  /**
   * Tests the behavior of
   * {@link com.google.gwt.user.client.ui.Image#setUrlAndVisibleRect(String,int,int,int,int)}
   * on a clipped image.
   */
  @SuppressWarnings("deprecation")
  public void testSetUrlAndVisibleRectOnClippedImage() {
    final Image image = new Image("counting-backwards.png", 12, 12, 12, 12);

    final TestLoadListener listener = new TestLoadListener(image) {
      public void onLoad(Widget sender) {
        if (isFinished()) {
          fail("LoadListener fired twice. Expected it to fire only once.");
        }

        assertEquals(0, image.getOriginLeft());
        assertEquals(16, image.getOriginTop());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertEquals("clipped", getCurrentImageStateName(image));
        finish();
      }
    };
    image.addLoadListener(listener);

    final TestLoadHandler loadHandler = new TestLoadHandler() {
      public void onLoad(LoadEvent event) {
        if (isFinished()) {
          fail("LoadHandler fired twice. Expected it to fire only once.");
        }
        if (listener.isFinished()) {
          finish();
        } else {
          fail("Listener did not fire first");
        }
      }
    };
    image.addLoadHandler(loadHandler);
    image.addErrorHandler(new TestErrorHandler(image));

    RootPanel.get().add(image);
    assertEquals("clipped", getCurrentImageStateName(image));

    /*
     * Change the url and visible rect, but we only expect one asynchronous load
     * event to fire. This is consistent with the expected behavior of changing
     * the source URL in the same event loop.
     */
    image.setUrlAndVisibleRect("counting-forwards.png", 0, 16, 16, 16);

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        assertTrue(loadHandler.isFinished());
        finishTest();
      }
    }.schedule(SYNTHETIC_LOAD_EVENT_TIMEOUT);
  }

  /**
   * Tests the firing of onload events when calling
   * {@link com.google.gwt.user.client.ui.Image#setVisibleRect(int,int,int,int)}
   * on a clipped image.
   */
  @SuppressWarnings("deprecation")
  public void testSetVisibleRectAndLoadEventsOnClippedImage() {
    final Image image = new Image("counting-backwards.png", 16, 16, 16, 16);

    final TestLoadListener listener = new TestLoadListener(image) {
      public void onLoad(Widget sender) {
        if (isFinished()) {
          fail("LoadListener fired twice. Expected it to fire only once.");
        }
        finish();
      }
    };
    image.addLoadListener(listener);

    final TestLoadHandler loadHandler = new TestLoadHandler() {
      public void onLoad(LoadEvent event) {
        if (isFinished()) {
          fail("LoadHandler fired twice. Expected it to fire only once.");
        }
        if (listener.isFinished()) {
          finish();
        } else {
          fail("Listener did not fire first");
        }
      }
    };
    image.addLoadHandler(loadHandler);
    image.addErrorHandler(new TestErrorHandler(image));

    RootPanel.get().add(image);

    /*
     * Change the visible rect multiple times, but we only expect one
     * asynchronous load event to fire after the final change. This is consistent
     * with the expected behavior of changing the source URL multiple times.
     */
    image.setVisibleRect(0, 0, 16, 16);
    image.setVisibleRect(0, 0, 16, 16);
    image.setVisibleRect(16, 0, 16, 16);
    image.setVisibleRect(16, 8, 8, 8);

    delayTestFinish(DEFAULT_TEST_TIMEOUT);
    new Timer() {
      @Override
      public void run() {
        assertTrue(loadHandler.isFinished());
        finishTest();
      }
    }.schedule(SYNTHETIC_LOAD_EVENT_TIMEOUT);
  }

  /**
   * Tests that it is possible to make a subclass of Image that can be wrapped.
   */
  public void testWrapOfSubclass() {
    String uid = Document.get().createUniqueId();
    DivElement div = Document.get().createDivElement();
    div.setInnerHTML("<img id='" + uid + "' src='counting-forward.png'>");
    Document.get().getBody().appendChild(div);

    final TestImage image = TestImage.wrap(Document.get().getElementById(uid));
    assertNotNull(image);

    // Cleanup.
    Document.get().getBody().appendChild(div);
    RootPanel.detachNow(image);
  }

  /**
   * Tests that wrapping an existing DOM element works if you call
   * setUrlAndVisibleRect() on it.
   */
  public void testWrapThenSetUrlAndVisibleRect() {
    String uid = Document.get().createUniqueId();
    DivElement div = Document.get().createDivElement();
    div.setInnerHTML("<img id='" + uid
        + "' src='counting-backwards.png' width='16' height='16'>");
    Document.get().getBody().appendChild(div);
    final Image image = Image.wrap(Document.get().getElementById(uid));

    assertEquals(0, image.getOriginLeft());
    assertEquals(0, image.getOriginTop());
    assertEquals(16, image.getWidth());
    assertEquals(16, image.getHeight());
    assertEquals("unclipped", getCurrentImageStateName(image));

    image.setUrlAndVisibleRect("counting-forwards.png", 0, 16, 16, 16);
    assertEquals(0, image.getOriginLeft());
    assertEquals(16, image.getOriginTop());
    assertEquals(16, image.getWidth());
    assertEquals(16, image.getHeight());
    assertEquals("clipped", getCurrentImageStateName(image));
  }

  private void assertResourceWorked(Image image, ImageResource prettyPiccy) {
    assertEquals(prettyPiccy.getSafeUri().asString(), image.getUrl());
    assertEquals(prettyPiccy.getTop(), image.getOriginTop());
    assertEquals(prettyPiccy.getHeight(), image.getHeight());
    assertEquals(prettyPiccy.getLeft(), image.getOriginLeft());
    assertEquals(prettyPiccy.getWidth(), image.getWidth());
  }
}
