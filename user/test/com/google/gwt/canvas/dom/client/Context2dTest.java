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
package com.google.gwt.canvas.dom.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d.Composite;
import com.google.gwt.canvas.dom.client.Context2d.LineCap;
import com.google.gwt.canvas.dom.client.Context2d.LineJoin;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.canvas.dom.client.Context2d.TextBaseline;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests {@link Context2d}.
 * 
 * Because HtmlUnit does not support HTML5, you will need to run these tests manually in order to
 * have them run. To do that, go to "run configurations" or "debug configurations", select the test
 * you would like to run, and put this line in the VM args under the arguments tab:
 * -Dgwt.args="-runStyle Manual:1"
 */
@DoNotRunWith(Platform.HtmlUnitUnknown)
public class Context2dTest extends GWTTestCase {
  protected Canvas canvas1;
  protected Canvas canvas2;

  native boolean isGecko190OrBefore() /*-{
    return @com.google.gwt.dom.client.DOMImplMozilla::isGecko190OrBefore()();
  }-*/;

  native boolean isWebkit525OrBefore() /*-{
    return @com.google.gwt.dom.client.DOMImplWebkit::isWebkit525OrBefore()();
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.gwt.canvas.Canvas";
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
    RootPanel.get().remove(canvas1);
    RootPanel.get().remove(canvas2);
  }

  public void testArc() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(80);
    canvas1.setCoordinateSpaceWidth(120);

    // get a 2d context
    Context2d context = canvas1.getContext2d();

    // make sure there are no issues drawing an arc
    try {
      context.beginPath();
      context.arc(50, 50, 40, 0, Math.PI);
      context.closePath();
      context.stroke();
    } catch (Exception e) {
      fail("Should not throw an exception drawing an arc: " + e.getMessage());
    }
  }

  public void testFillRect() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // Safari 3.0 does not support getImageData(), so the following tests are disabled for
    // Safari 3.0 and before.
    if (isWebkit525OrBefore()) {
      return;
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(80);
    canvas1.setCoordinateSpaceWidth(120);
    assertEquals(40, canvas1.getOffsetHeight());
    assertEquals(60, canvas1.getOffsetWidth());
    assertEquals(80, canvas1.getCoordinateSpaceHeight());
    assertEquals(120, canvas1.getCoordinateSpaceWidth());

    // get a 2d context
    Context2d context = canvas1.getContext2d();

    // draw green rectangle filling 1st half of canvas
    context.setFillStyle(CssColor.make("#00fF00"));
    context.fillRect(0, 0, 60, 80);
    // draw red rectangle filling 2nd half of canvas
    context.setFillStyle("#fF0000");
    context.fillRect(60, 0, 60, 80);

    // test that the first pixel is green and the last pixel is red
    ImageData imageData = context.getImageData(0, 0, 120, 80);
    CanvasPixelArray pixelArray = imageData.getData();
    assertEquals(000, pixelArray.get(0));
    assertEquals(255, pixelArray.get(1));
    assertEquals(000, pixelArray.get(2));
    assertEquals(255, pixelArray.get(3));
    assertEquals(255, pixelArray.get((40 * 20 - 1) * 4 + 0));
    assertEquals(000, pixelArray.get((40 * 20 - 1) * 4 + 1));
    assertEquals(000, pixelArray.get((40 * 20 - 1) * 4 + 2));
    assertEquals(255, pixelArray.get((40 * 20 - 1) * 4 + 3));
    
    // draw a blue square in the top left
    context.setFillStyle("#0000fF");
    context.fillRect(0, 0, 20, 20);
    imageData = context.getImageData(0, 0, 20, 20);
    pixelArray = imageData.getData();
    
    // test that the first pixel is blue
    assertEquals(000, pixelArray.get(0));
    assertEquals(000, pixelArray.get(1));
    assertEquals(255, pixelArray.get(2));
    assertEquals(255, pixelArray.get(3));
  }

  public void testFillStyle() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // get the 2d contexts
    Context2d context1 = canvas1.getContext2d();
    Context2d context2 = canvas2.getContext2d();

    // test that a color can be set and is correct
    context1.setFillStyle(CssColor.make("#ffff00"));
    FillStrokeStyle fillStyleCol = context1.getFillStyle();
    assertTrue("fillStyleCol is a color", fillStyleCol.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertFalse("fillStyleCol is a color", fillStyleCol.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertFalse("fillStyleCol is a color", fillStyleCol.getType() == FillStrokeStyle.TYPE_PATTERN);

    // test that a gradient can be set and is correct
    CanvasGradient gradient = context1.createLinearGradient(0, 0, 10, 20);
    gradient.addColorStop(0.5f, "#ffaaff");
    context1.setFillStyle(gradient);
    FillStrokeStyle fillStyleGrad = context1.getFillStyle();
    assertFalse("fillStyleGrad is a gradient", fillStyleGrad.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertTrue("fillStyleGrad is a gradient", fillStyleGrad.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertFalse("fillStyleGrad is a gradient", fillStyleGrad.getType() == FillStrokeStyle.TYPE_PATTERN);

    // test that a pattern can be set and is correct
    CanvasPattern pattern = context1.createPattern(canvas1.getCanvasElement(), 
        Context2d.Repetition.REPEAT);
    context1.setFillStyle(pattern);
    FillStrokeStyle fillStylePat = context1.getFillStyle();
    assertFalse("fillStylePat is a pattern", fillStylePat.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertFalse("fillStylePat is a pattern", fillStylePat.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertTrue("fillStylePat is a pattern", fillStylePat.getType() == FillStrokeStyle.TYPE_PATTERN);
    
    // test that a StrokeStyle can be passed around w/o knowing what it is
    FillStrokeStyle fillStyle = context1.getFillStyle();
    context2.setFillStyle(fillStyle);
    FillStrokeStyle fillStyle2 = context2.getFillStyle();
    assertFalse("fillStyle2 is a pattern", fillStyle2.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertFalse("fillStyle2 is a pattern", fillStyle2.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertTrue("fillStyle2 is a pattern", fillStyle2.getType() == FillStrokeStyle.TYPE_PATTERN);
  }

  public void testFont() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    Context2d context = canvas1.getContext2d();
    context.setFont("40px Times New Roman");
    assertEquals("40px Times New Roman", context.getFont());
  }

  public void testGlobalAlpha() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    Context2d context = canvas1.getContext2d();
    context.setGlobalAlpha(0.5);
    assertEquals(0.5, context.getGlobalAlpha());
  }

  // This test currently fails on IE9 and is disabled.
  public void disabled_testGlobalComposite() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    Context2d context = canvas1.getContext2d();
    context.setGlobalCompositeOperation(Composite.SOURCE_OVER);
    assertEquals(Composite.SOURCE_OVER.getValue(), context.getGlobalCompositeOperation());
    context.setGlobalCompositeOperation(Composite.DESTINATION_OVER);
    assertEquals(Composite.DESTINATION_OVER.getValue(), context.getGlobalCompositeOperation());
  }

  public void testGradient() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // Safari 3.0 does not support getImageData(), so the following tests are disabled for
    // Safari 3.0 and before.
    if (isWebkit525OrBefore()) {
      return;
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(40);
    canvas1.setCoordinateSpaceWidth(60);
    Context2d context = canvas1.getContext2d();

    // fill the canvas with black
    context.setFillStyle("#000000");
    context.fillRect(0, 0, 60, 40);
    
    // create a linear gradient from the top-left to the bottom-right.
    CanvasGradient linearGradient = context.createLinearGradient(0, 0, 60, 40);
    linearGradient.addColorStop(0, "#ff0000");
    linearGradient.addColorStop(1, "#00ffff");
    context.setFillStyle(linearGradient);
    context.fillRect(0, 0, 60, 40);
    
    // test that the first pixel is ff0000, the last is 000fff, and the middle is something else
    // isn't exact due to rounding, give 5px approx wiggle-room
    int approx = 5;
    ImageData imageData = context.getImageData(0, 0, 60, 40);
    CanvasPixelArray pixelArray = imageData.getData();

    assertTrue(255 - pixelArray.get(0) < approx);
    assertTrue(pixelArray.get(1) - 000 < approx);
    assertTrue(pixelArray.get(2) - 000 < approx);
    assertTrue(255 - pixelArray.get(3) < approx);
    assertFalse(000 == pixelArray.get((60 * 40 / 2) * 4 + 0));
    assertFalse(255 == pixelArray.get((60 * 40 / 2) * 4 + 0));
    assertTrue(pixelArray.get((60 * 40 - 1) * 4 + 0) - 000 < approx);
    assertTrue(255 - pixelArray.get((60 * 40 - 1) * 4 + 1) < approx);
    assertTrue(255 - pixelArray.get((60 * 40 - 1) * 4 + 2) < approx);
    assertTrue(255 - pixelArray.get((60 * 40 - 1) * 4 + 3) < approx);
  }

  public void testImageData() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }
    
    // Firefox 3.0 does not support createImageData(), so the following tests are disabled
    // for FF 3.0 and before.
    if (isGecko190OrBefore()) {
      return;
    }

    // Safari 3.0 does not support getImageData(), so the following tests are disabled for
    // Safari 3.0 and before.
    if (isWebkit525OrBefore()) {
      return;
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(40);
    canvas1.setCoordinateSpaceWidth(60);
    Context2d context = canvas1.getContext2d();

    // fill the canvas with ffff00
    context.setFillStyle("#ffff00");
    context.fillRect(0, 0, 60, 40);
    
    // create a 1 x 1 image
    ImageData onePx = context.createImageData(1, 1); // will fail on FF3.0 (not such method)
    assertEquals(1, onePx.getHeight());
    assertEquals(1, onePx.getWidth());
    
    // set the image to a single blue pixel
    CanvasPixelArray onePxArray = onePx.getData();
    onePxArray.set(0, 000);
    onePxArray.set(1, 000);
    onePxArray.set(2, 255);
    onePxArray.set(3, 255);
    
    // put the pixel at location 10, 10 on the canvas
    context.putImageData(onePx, 10, 10);
    ImageData verifyPx = context.getImageData(10, 10, 1, 1);
    assertEquals(1, verifyPx.getWidth());
    assertEquals(1, verifyPx.getHeight());
    CanvasPixelArray verifyPxArray = verifyPx.getData();
    assertEquals(onePxArray.get(0), verifyPxArray.get(0));
    assertEquals(onePxArray.get(1), verifyPxArray.get(1));
    assertEquals(onePxArray.get(2), verifyPxArray.get(2));
    assertEquals(onePxArray.get(3), verifyPxArray.get(3));

    // test that edge cases don't blow up: values outside the range 0...255
    ImageData clampPixels = context.createImageData(3, 3); // will fail on FF3.0 (not such method)
    CanvasPixelArray clampArraySet = clampPixels.getData();
    try {
      clampArraySet.set(2, -2);
      clampArraySet.set(3, 270);
      context.putImageData(clampPixels, 4, 4);
    } catch (Exception e) {
      fail("Should not throw exception when setting values outside the range of 0...255");
    }
    clampPixels = context.getImageData(4, 4, 3, 3);
    CanvasPixelArray clampArrayGet = clampPixels.getData();
    
    // test that edge cases don't blow up: fall off the CanvasPixelArray end
    int aPixel = clampArrayGet.get(9999);
    assertEquals("CanvasPixelArray should return 0 for values off its end", aPixel, 0);
    int bPixel = clampArrayGet.get(-9999);
    assertEquals("CanvasPixelArray should return 0 for values off its end", bPixel, 0);
  }

  public void testIsPointInPath() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(40);
    canvas1.setCoordinateSpaceWidth(60);

    Context2d context = canvas1.getContext2d();
    context.beginPath();
    context.moveTo(10, 10);
    context.lineTo(20, 20);
    context.lineTo(20, 10);
    context.closePath();
    
    assertTrue("Point should be in path", context.isPointInPath(18, 12));
    assertFalse("Point should not be in path", context.isPointInPath(1, 1));
  }

  public void testLines() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(40);
    canvas1.setCoordinateSpaceWidth(60);
    
    Context2d context = canvas1.getContext2d();
    context.setFillStyle("#ff00ff");
    context.setLineCap(LineCap.BUTT);
    context.setLineJoin(LineJoin.BEVEL);
    context.setLineWidth(2);

    context.beginPath();
    context.moveTo(10, 10);
    context.lineTo(20, 20);
    context.lineTo(20, 10);
    context.closePath();

    assertEquals(LineCap.BUTT.getValue(), context.getLineCap());
    assertEquals(LineJoin.BEVEL.getValue(), context.getLineJoin());
    assertEquals(2.0, context.getLineWidth());
  }

  public void testMiter() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    Context2d context = canvas1.getContext2d();
    context.setMiterLimit(3);
    assertEquals(3.0, context.getMiterLimit());
  }

  public void testPixelManipulation() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // Safari 3.0 does not support getImageData(), so the following tests are disabled for
    // Safari 3.0 and before.
    if (isWebkit525OrBefore()) {
      return;
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(40);
    canvas1.setCoordinateSpaceWidth(60);
    Context2d context = canvas1.getContext2d();

    // fill the canvas with ff0000
    context.setFillStyle("#ff0000");
    context.fillRect(0, 0, 60, 40);
    // fill the 1st and 2nd quadrants with green
    context.setFillStyle("#00ff00");
    context.fillRect(0, 0, 60, 20);
    
    ImageData imageData = context.getImageData(0, 0, 60, 40);
    assertEquals("Pixels in first quadrant should be green", 0, imageData.getRedAt(45, 10));
    assertEquals("Pixels in first quadrant should be green", 255, imageData.getGreenAt(45, 10));
    assertEquals("Pixels in third quadrant should be red", 255, imageData.getRedAt(15, 30));
    assertEquals("Pixels in third quadrant should be red", 0, imageData.getGreenAt(15, 30));
  }

  public void testShadows() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }
    
    // Firefox 3.0 returns the incorrect shadowBlur value so the following tests are disabled
    // for FF 3.0 and before.
    if (isGecko190OrBefore()) {
      return;
    }

    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    canvas1.setCoordinateSpaceHeight(40);
    canvas1.setCoordinateSpaceWidth(60);

    Context2d context = canvas1.getContext2d();
    context.setShadowBlur(3);
    context.setShadowColor("#ff00ff");
    context.setShadowOffsetX(3);
    context.setShadowOffsetY(4);
    context.lineTo(60, 40);
    assertEquals(3.0, context.getShadowBlur());
    assertEquals("#ff00ff", context.getShadowColor().toLowerCase());
    assertEquals(3.0, context.getShadowOffsetX());
    assertEquals(4.0, context.getShadowOffsetY());
  }

  public void testStrokeStyle() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }

    // get the 2d contexts
    Context2d context1 = canvas1.getContext2d();
    Context2d context2 = canvas2.getContext2d();

    // test that a color can be set and is correct
    context1.setStrokeStyle(CssColor.make("#ffff00"));
    FillStrokeStyle strokeStyleCol = context1.getStrokeStyle();
    assertTrue("strokeStyleCol is a color", strokeStyleCol.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertFalse("strokeStyleCol is a color", strokeStyleCol.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertFalse("strokeStyleCol is a color", strokeStyleCol.getType() == FillStrokeStyle.TYPE_PATTERN);

    // test that a gradient can be set and is correct
    CanvasGradient gradient = context1.createLinearGradient(0, 0, 10, 20);
    gradient.addColorStop(0.5, "#ff000f");
    context1.setStrokeStyle(gradient);
    FillStrokeStyle strokeStyleGrad = context1.getStrokeStyle();
    assertFalse("strokeStyleGrad is a gradient", strokeStyleGrad.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertTrue("strokeStyleGrad is a gradient", strokeStyleGrad.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertFalse("strokeStyleGrad is a gradient", strokeStyleGrad.getType() == FillStrokeStyle.TYPE_PATTERN);

    // test that a pattern can be set and is correct
    CanvasPattern pattern = context1.createPattern(canvas1.getCanvasElement(), 
        Context2d.Repetition.REPEAT);
    context1.setStrokeStyle(pattern);
    FillStrokeStyle strokeStylePat = context1.getStrokeStyle();
    assertFalse("strokeStylePat is a pattern", strokeStylePat.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertFalse("strokeStylePat is a pattern", strokeStylePat.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertTrue("strokeStylePat is a pattern", strokeStylePat.getType() == FillStrokeStyle.TYPE_PATTERN);
    
    // test that a StrokeStyle can be passed around w/o knowing what it is
    FillStrokeStyle strokeStyle = context1.getStrokeStyle();
    context2.setStrokeStyle(strokeStyle);
    FillStrokeStyle strokeStyle2 = context2.getStrokeStyle();
    assertFalse("strokeStyle2 is a pattern", strokeStyle2.getType() == FillStrokeStyle.TYPE_CSSCOLOR);
    assertFalse("strokeStyle2 is a pattern", strokeStyle2.getType() == FillStrokeStyle.TYPE_GRADIENT);
    assertTrue("strokeStyle2 is a pattern", strokeStyle2.getType() == FillStrokeStyle.TYPE_PATTERN);
  }

  public void testText() {
    if (canvas1 == null) {
      return; // don't continue if not supported
    }
    
    canvas1.setHeight("40px");
    canvas1.setWidth("60px");
    Context2d context = canvas1.getContext2d();
    context.setFont("bold 40px sans-serif");
    context.setTextAlign(TextAlign.CENTER);
    context.setTextBaseline(TextBaseline.HANGING);
    context.fillText("GWT", 50, 60);
    assertEquals(TextAlign.CENTER.getValue(), context.getTextAlign());
    assertEquals(TextBaseline.HANGING.getValue(), context.getTextBaseline());
  }
}
