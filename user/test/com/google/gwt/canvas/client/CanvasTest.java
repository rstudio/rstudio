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
package com.google.gwt.canvas.client;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests {@link Canvas}.
 *
 *  Because HtmlUnit does not support HTML5, you will need to run these tests
 * manually in order to have them run. To do that, go to "run configurations" or
 * "debug configurations", select the test you would like to run, and put this
 * line in the VM args under the arguments tab: -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public class CanvasTest extends GWTTestCase {
  private static native boolean isFirefox35OrLater() /*-{
    var geckoVersion = @com.google.gwt.dom.client.DOMImplMozilla::getGeckoVersion()();
    return (geckoVersion != -1) && (geckoVersion >= 1009001);
  }-*/;

  private static native boolean isIE6() /*-{
    return @com.google.gwt.dom.client.DOMImplIE6::isIE6()();
  }-*/;

  private static native boolean isWebkit525OrBefore() /*-{
    return @com.google.gwt.dom.client.DOMImplWebkit::isWebkit525OrBefore()();
  }-*/;

  protected Canvas canvas1;

  protected Canvas canvas2;

  @Override
  public String getModuleName() {
    return "com.google.gwt.canvas.Canvas";
  }

  /*
   * If the canvas has no pixels (i.e. either its horizontal dimension or its
   * vertical dimension is zero) then the method must return the string
   * "data:,". (This is the shortest data: URL; it represents the empty string
   * in a text/plain resource.)
   *
   * Due to browser inconsistencies, we just check for data:something.
   */
  public void testBlankDataUrl() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // Safari 3.0 does not support toDataURL(), so the following tests are
    // disabled for Safari 3.0 and before.
    if (isWebkit525OrBefore()) {
      return;
    }

    canvas1.setHeight("0px");
    canvas1.setWidth("0px");
    assertEquals(0, canvas1.getOffsetHeight());
    assertEquals(0, canvas1.getOffsetWidth());
    canvas1.setCoordinateSpaceHeight(0);
    canvas1.setCoordinateSpaceWidth(0);

    String dataUrl = canvas1.toDataUrl();
    assertTrue("toDataURL() should return data:something",
        dataUrl.startsWith("data:"));
  }

  public void testDataUrlWithType() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // Safari 3.0 does not support toDataURL(), so the following tests are
    // disabled for Safari 3.0 and before.
    if (isWebkit525OrBefore()) {
      return;
    }

    canvas1.setHeight("10px");
    canvas1.setWidth("10px");
    canvas1.setCoordinateSpaceHeight(10);
    canvas1.setCoordinateSpaceWidth(10);

    String dataUrl = canvas1.toDataUrl("image/png");
    assertTrue("toDataURL(image/png) should return data:image/png[data]",
        dataUrl.startsWith("data:image/png"));
  }

  public void testHeightAndWidth() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    assertEquals(40, canvas1.getOffsetHeight());
    assertEquals(60, canvas1.getOffsetWidth());

    // resize
    canvas1.setHeight("41px");
    canvas1.setWidth("61px");
    assertEquals(41, canvas1.getOffsetHeight());
    assertEquals(61, canvas1.getOffsetWidth());

    // add 2d context, resize internal size, should have no effect
    Context2d context = canvas1.getContext2d();
    canvas1.setCoordinateSpaceHeight(140);
    canvas1.setCoordinateSpaceWidth(160);
    context.fillRect(2, 2, 300, 300);

    assertEquals(41, canvas1.getOffsetHeight());
    assertEquals(61, canvas1.getOffsetWidth());
  }

  public void testInternalHeightAndWidth() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    assertEquals(40, canvas1.getOffsetHeight());
    assertEquals(60, canvas1.getOffsetWidth());

    // resize internal
    canvas1.setCoordinateSpaceHeight(140);
    canvas1.setCoordinateSpaceWidth(160);
    assertEquals(140, canvas1.getCoordinateSpaceHeight());
    assertEquals(160, canvas1.getCoordinateSpaceWidth());

    // resize element, should have no effect on internal size
    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    assertEquals(40, canvas1.getOffsetHeight());
    assertEquals(60, canvas1.getOffsetWidth());
    assertEquals(140, canvas1.getCoordinateSpaceHeight());
    assertEquals(160, canvas1.getCoordinateSpaceWidth());

    // resize internal
    canvas1.setCoordinateSpaceHeight(141);
    canvas1.setCoordinateSpaceWidth(161);
    assertEquals(141, canvas1.getCoordinateSpaceHeight());
    assertEquals(161, canvas1.getCoordinateSpaceWidth());
  }

  public void testIsSupported() {
    if (canvas1 == null) {
      assertFalse(
          "isSupported() should be false when createIfSupported() returns null",
          Canvas.isSupported());
    } else {
      assertTrue(
          "isSupported() should be true when createIfSupported() returns non-null", Canvas.isSupported());
    }
    // test the isxxxSupported() call if running known-sup or known-not-sup
    // browsers
    if (isFirefox35OrLater()) {
      assertTrue(Canvas.isSupported());
      assertTrue(Canvas.isSupported());
    }
    if (isIE6()) {
      assertFalse(Canvas.isSupported());
      assertFalse(Canvas.isSupported());
    }
  }

  @Override
  protected void gwtSetUp() throws Exception {
    canvas1 = Canvas.createIfSupported();
    canvas2 = Canvas.createIfSupported();

    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    RootPanel.get().add(canvas1);
    RootPanel.get().add(canvas2);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    RootPanel.get().remove(canvas1);
    RootPanel.get().remove(canvas2);
  }
}
