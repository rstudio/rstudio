/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.layout.client;

import static com.google.gwt.dom.client.Style.Unit.CM;
import static com.google.gwt.dom.client.Style.Unit.EM;
import static com.google.gwt.dom.client.Style.Unit.EX;
import static com.google.gwt.dom.client.Style.Unit.IN;
import static com.google.gwt.dom.client.Style.Unit.MM;
import static com.google.gwt.dom.client.Style.Unit.PC;
import static com.google.gwt.dom.client.Style.Unit.PCT;
import static com.google.gwt.dom.client.Style.Unit.PT;
import static com.google.gwt.dom.client.Style.Unit.PX;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.layout.client.Layout.Alignment;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowTest;

/**
 * Tests for the {@link Layout} class.
 */
public class LayoutTest extends GWTTestCase {

  /**
   * The amount of time to wait for asynchronous tests to finish.
   */
  private static final int TEST_DELAY = 2000;

  private static interface LayerInitializer {
    void setupLayers(Layer l0, Layer l1);
  }

  private DivElement parent, child0, child1;
  private Element wrapper0, wrapper1;
  private Layout layout;
  private Layer layer0, layer1;
  private boolean resized;

  @Override
  public String getModuleName() {
    return "com.google.gwt.layout.LayoutTest";
  }

  // All testAnimationTransitions_* tests are disabled because they are flaky
  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_LTRB_PX_CM() {
    testAnimationTransitions_LTWH_LTRB(PX, CM);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_LTRB_PX_EM() {
    testAnimationTransitions_LTWH_LTRB(PX, EM);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_LTRB_PX_EX() {
    testAnimationTransitions_LTWH_LTRB(PX, EX);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_LTRB_PX_PCT() {
    testAnimationTransitions_LTWH_LTRB(PX, PCT);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_RBWH_PX_CM() {
    testAnimationTransitions_LTWH_RBWH(PX, CM);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_RBWH_PX_EM() {
    testAnimationTransitions_LTWH_RBWH(PX, EM);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_RBWH_PX_EX() {
    testAnimationTransitions_LTWH_RBWH(PX, EX);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_LTWH_RBWH_PX_PCT() {
    testAnimationTransitions_LTWH_RBWH(PX, PCT);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_RBWH_LTRB_PX_CM() {
    testAnimationTransitions_RBWH_LTRB(PX, CM);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_RBWH_LTRB_PX_EM() {
    testAnimationTransitions_RBWH_LTRB(PX, EM);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_RBWH_LTRB_PX_EX() {
    testAnimationTransitions_RBWH_LTRB(PX, EX);
  }

  /**
   * Tests animation constraint- and unit-transitions.
   */
  public void notestAnimationTransitions_RBWH_LTRB_PX_PCT() {
    testAnimationTransitions_RBWH_LTRB(PX, PCT);
  }

  /**
   * Tests child alignment within a layer.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testChildAlignment() {
    layer0.setLeftWidth(0, PX, 128, PX);
    layer0.setTopHeight(0, PX, 256, PX);

    layer0.setChildHorizontalPosition(Alignment.STRETCH);
    layer0.setChildVerticalPosition(Alignment.STRETCH);
    layout.layout();
    assertEquals(0, child0.getOffsetLeft());
    assertEquals(0, child0.getOffsetTop());
    assertEquals(128, child0.getOffsetWidth());
    assertEquals(256, child0.getOffsetHeight());

    child0.getStyle().setWidth(64, PX);
    child0.getStyle().setHeight(128, PX);

    layer0.setChildHorizontalPosition(Alignment.BEGIN);
    layer0.setChildVerticalPosition(Alignment.BEGIN);
    layout.layout();
    assertEquals(0, child0.getOffsetLeft());
    assertEquals(0, child0.getOffsetTop());
    assertEquals(64, child0.getOffsetWidth());
    assertEquals(128, child0.getOffsetHeight());

    layer0.setChildHorizontalPosition(Alignment.END);
    layer0.setChildVerticalPosition(Alignment.END);
    layout.layout();
    assertEquals(64, child0.getOffsetLeft());
    assertEquals(128, child0.getOffsetTop());
    assertEquals(64, child0.getOffsetWidth());
    assertEquals(128, child0.getOffsetHeight());
  }

  /**
   * Test that fillParent() works properly when the outer div is a child of
   * another div, and that it correctly follows that div's size.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testFillParent() {
    // We don't use the default elements created in gwtSetUp() because we need
    // to test the behavior when the layout is contained by an element other
    // than the <body>.
    Document doc = Document.get();
    DivElement container = doc.createDivElement();
    DivElement parent = doc.createDivElement();
    DivElement child = doc.createDivElement();
    child.setInnerHTML("&nbsp;");
    doc.getBody().appendChild(container);
    container.appendChild(parent);

    // The container has to be position:relative so that it serves as an offset
    // parent.
    container.getStyle().setPosition(Position.RELATIVE);
    container.getStyle().setWidth(128, PX);
    container.getStyle().setHeight(256, PX);

    Layout layout = new Layout(parent);
    layout.onAttach();
    Layer layer = layout.attachChild(child);
    layer.setTopBottom(0, PX, 0, PX);
    layer.setLeftRight(0, PX, 0, PX);

    layout.fillParent();
    layout.layout();

    // Test 128x256.
    assertEquals(128, container.getOffsetWidth());
    assertEquals(256, container.getOffsetHeight());
    assertEquals(128, parent.getOffsetWidth());
    assertEquals(256, parent.getOffsetHeight());
    assertEquals(128, child.getOffsetWidth());
    assertEquals(256, child.getOffsetHeight());

    // Expand to 256x256. The layout should react automatically.
    container.getStyle().setWidth(256, PX);
    container.getStyle().setHeight(128, PX);
    assertEquals(256, container.getOffsetWidth());
    assertEquals(256, parent.getOffsetWidth());
    assertEquals(256, child.getOffsetWidth());

    layout.onDetach();
  }

  /**
   * Test that fillParent() works properly when the outer div is a child of the
   * document body.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testFillWindow() {
    layer0.setTopBottom(0, PX, 0, PX);
    layer0.setLeftRight(0, PX, 0, PX);
    layout.layout();

    int w = Window.getClientWidth();
    int h = Window.getClientHeight();
    assertEquals(w, parent.getOffsetWidth());
    assertEquals(h, parent.getOffsetHeight());
    assertEquals(w, child0.getOffsetWidth());
    assertEquals(h, child0.getOffsetHeight());
  }

  /**
   * Tests that the layout reacts to font-size changes.
   * 
   * TODO(jgw): Enable this test when it is fixed for IE8.
   */
  public void disabledTestFontSizeChange() {
    layer0.setLeftWidth(0, PX, 1, EM);
    layer0.setTopHeight(0, PX, 1, EM);
    layout.layout();

    parent.getStyle().setFontSize(12, PT);
    int cw = child0.getOffsetWidth();
    int ch = child0.getOffsetHeight();

    parent.getStyle().setFontSize(24, PT);
    int nw = child0.getOffsetWidth();
    int nh = child0.getOffsetHeight();
    assertTrue(nw > cw);
    assertTrue(nh > ch);

    parent.getStyle().clearFontSize();
  }

  /**
   * Ensures that two children laid out using various units in such a way that
   * they should abut one another actually do so.
   */
  public void testLayoutStructure() {
    testHorizontalSplit(CM);
    testHorizontalSplit(EM);
    testHorizontalSplit(EX);
    testHorizontalSplit(IN);
    testHorizontalSplit(MM);
    testHorizontalSplit(PC);
    testHorizontalSplit(PT);
    testHorizontalSplit(PX);

    testVerticalSplit(CM);
    testVerticalSplit(EM);
    testVerticalSplit(EX);
    testVerticalSplit(IN);
    testVerticalSplit(MM);
    testVerticalSplit(PC);
    testVerticalSplit(PT);
    testVerticalSplit(PX);
  }

  /**
   * Tests (left-right, left-width, right-width) x (top-bottom, top-height,
   * bottom-height). Ok, so we don't test the *entire* cross-product, but enough
   * to be comfortable.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testStaticConstraints() {
    // This test assumes enough size. Ignore it if size cannot be guaranteed.
    if (!resized) {
      return;
    }

    // left-right, top-bottom
    layer0.setTopBottom(32, PX, 32, PX);
    layer0.setLeftRight(32, PX, 32, PX);
    layout.layout();

    int w = parent.getClientWidth();
    int h = parent.getClientHeight();
    assertEquals(32, wrapper0.getOffsetLeft());
    assertEquals(32, wrapper0.getOffsetTop());
    assertEquals(w - 64, wrapper0.getOffsetWidth());
    assertEquals(h - 64, wrapper0.getOffsetHeight());

    // left-width, top-height
    layer0.setTopHeight(16, PX, 128, PX);
    layer0.setLeftWidth(16, PX, 128, PX);
    layout.layout();

    assertEquals(16, wrapper0.getOffsetLeft());
    assertEquals(16, wrapper0.getOffsetTop());
    assertEquals(128, wrapper0.getOffsetWidth());
    assertEquals(128, wrapper0.getOffsetHeight());

    // right-width, bottom-height
    layer0.setBottomHeight(16, PX, 128, PX);
    layer0.setRightWidth(16, PX, 128, PX);
    layout.layout();

    assertEquals(w - (16 + 128), wrapper0.getOffsetLeft());
    assertEquals(h - (16 + 128), wrapper0.getOffsetTop());
    assertEquals(128, wrapper0.getOffsetWidth());
    assertEquals(128, wrapper0.getOffsetHeight());
  }

  /**
   * Tests all unit types.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testUnits() {
    // This test assumes enough size. Ignore it if size cannot be guaranteed.
    if (!resized) {
      return;
    }

    // CM
    layer0.setTopBottom(1, CM, 1, CM);
    layer0.setLeftRight(1, CM, 1, CM);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // MM
    layer0.setTopBottom(1, MM, 1, MM);
    layer0.setLeftRight(1, MM, 1, MM);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // IN
    layer0.setTopBottom(1, IN, 1, IN);
    layer0.setLeftRight(1, IN, 1, IN);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // EM
    layer0.setTopBottom(1, EM, 1, EM);
    layer0.setLeftRight(1, EM, 1, EM);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // EX
    layer0.setTopBottom(1, EX, 1, EX);
    layer0.setLeftRight(1, EX, 1, EX);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // PC
    layer0.setTopBottom(1, PC, 1, PC);
    layer0.setLeftRight(1, PC, 1, PC);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // PT
    layer0.setTopBottom(10, PT, 10, PT);
    layer0.setLeftRight(10, PT, 10, PT);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);

    // PCT
    layer0.setTopBottom(10, PCT, 10, PCT);
    layer0.setLeftRight(10, PCT, 10, PCT);
    layout.layout();
    assertLeftRightTopBottomUnitsMakeSense(wrapper0);
  }

  /**
   * Tests layout in the presence of decorations on the parent and child
   * elements.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testWithDecorations() {
    layer0.setTopBottom(0, PX, 0, PX);
    layer0.setLeftRight(0, PX, 0, PX);
    layout.layout();

    // Give each of the parent and child 1px margin and 1px border.
    parent.getStyle().setMargin(1, PX);
    parent.getStyle().setProperty("border", "1px solid black");

    child0.getStyle().setMargin(1, PX);
    child0.getStyle().setProperty("border", "1px solid black");
    layout.layout();

    int w = Window.getClientWidth();
    int h = Window.getClientHeight();
    int pw = parent.getOffsetWidth();
    int ph = parent.getOffsetHeight();

    // The parent's offsetSize should be 2px smaller than the window's client
    // area, because of the margin (the border is *included* in the offsetSize).
    assertEquals(w - 2, pw);
    assertEquals(h - 2, ph);

    // The child's offsetSize (actually that of its wrapper element), should be
    // 2px smaller than the parent, for precisely the same reason.
    assertEquals(pw - 2, wrapper0.getOffsetWidth());
    assertEquals(ph - 2, wrapper0.getOffsetHeight());
  }

  @Override
  protected void gwtSetUp() throws Exception {
    // ensure enough sizes for this test
    resized = WindowTest.ResizeHelper.resizeTo(800, 600);

    Window.enableScrolling(false);

    Document doc = Document.get();
    parent = doc.createDivElement();
    child0 = doc.createDivElement();
    child1 = doc.createDivElement();
    doc.getBody().appendChild(parent);

    layout = new Layout(parent);
    layout.onAttach();
    layout.fillParent();

    layer0 = layout.attachChild(child0);
    layer1 = layout.attachChild(child1);

    wrapper0 = child0.getParentElement();
    wrapper1 = child1.getParentElement();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    Window.enableScrolling(true);
    Document.get().getBody().removeChild(parent);
    layout.onDetach();
    WindowTest.ResizeHelper.restoreSize();
  }

  private void assertLeftRightTopBottomUnitsMakeSense(Element elem) {
    // Assume that the element has been laid out to (l, t, r, b) = (1u, 1u, 1u,
    // 1u). Assert that the element's clientLeft/Top are non-zero, and that the
    // clientLeft/Top/Width/Height are consistent with the parent's size.
    int w = parent.getClientWidth();
    int h = parent.getClientHeight();
    int cl = elem.getOffsetLeft();
    int ct = elem.getOffsetTop();
    int cw = elem.getOffsetWidth();
    int ch = elem.getOffsetHeight();

    // Assert that the left-top unit came out at least non-zero size.
    assertTrue(cl > 0);
    assertTrue(ct > 0);

    // Assert that the right-bottom also came out non-zero. We should be able
    // to assert that it came out the same size as the top-left, but it turns
    // out that this isn't quite reliable because of rounding errors.
    assertTrue(w - (cl + cw) > 0);
    assertTrue(h - (ct + ch) > 0);
  }

  // This method may only be called once per test, as it uses delayTestFinish()
  // internally.
  private void testAnimationTransitions_LTWH_LTRB(final Unit unit0,
      final Unit unit1) {
    testAnimationTransitionsHelper(new LayerInitializer() {
      public void setupLayers(Layer l0, Layer l1) {
        l0.setLeftWidth(0, unit0, 10, unit0);
        l0.setTopHeight(0, unit0, 10, unit0);
        l1.setLeftRight(1, unit1, 1, unit1);
        l1.setTopBottom(1, unit1, 1, unit1);
      }
    }, new LayerInitializer() {
      public void setupLayers(Layer l0, Layer l1) {
        l1.setLeftWidth(0, unit0, 10, unit0);
        l1.setTopHeight(0, unit0, 10, unit0);
        l0.setLeftRight(1, unit1, 1, unit1);
        l0.setTopBottom(1, unit1, 1, unit1);
      }
    });
  }

  // This method may only be called once per test, as it uses delayTestFinish()
  // internally.
  private void testAnimationTransitions_LTWH_RBWH(final Unit unit0,
      final Unit unit1) {
    testAnimationTransitionsHelper(new LayerInitializer() {
      public void setupLayers(Layer l0, Layer l1) {
        l0.setLeftWidth(0, unit0, 10, unit0);
        l0.setTopHeight(0, unit0, 10, unit0);
        l1.setRightWidth(0, unit1, 10, unit1);
        l1.setBottomHeight(0, unit1, 10, unit1);
      }
    }, new LayerInitializer() {
      public void setupLayers(Layer l0, Layer l1) {
        l1.setLeftWidth(0, unit0, 10, unit0);
        l1.setTopHeight(0, unit0, 10, unit0);
        l0.setRightWidth(0, unit1, 10, unit1);
        l0.setBottomHeight(0, unit1, 10, unit1);
      }
    });
  }

  // This method may only be called once per test, as it uses delayTestFinish()
  // internally.
  private void testAnimationTransitions_RBWH_LTRB(final Unit unit0,
      final Unit unit1) {
    testAnimationTransitionsHelper(new LayerInitializer() {
      public void setupLayers(Layer l0, Layer l1) {
        l0.setRightWidth(0, unit0, 10, unit0);
        l0.setBottomHeight(0, unit0, 10, unit0);
        l1.setLeftRight(1, unit1, 1, unit1);
        l1.setTopBottom(1, unit1, 1, unit1);
      }
    }, new LayerInitializer() {
      public void setupLayers(Layer l0, Layer l1) {
        l1.setRightWidth(0, unit0, 10, unit0);
        l1.setBottomHeight(0, unit0, 10, unit0);
        l0.setLeftRight(1, unit1, 1, unit1);
        l0.setTopBottom(1, unit1, 1, unit1);
      }
    });
  }

  // This method may only be called once per test, as it uses delayTestFinish()
  // internally.
  private void testAnimationTransitionsHelper(LayerInitializer before,
      LayerInitializer after) {
    before.setupLayers(layer0, layer1);
    layout.layout();

    final int l0 = wrapper0.getOffsetLeft();
    final int t0 = wrapper0.getOffsetTop();
    final int w0 = wrapper0.getOffsetWidth();
    final int h0 = wrapper0.getOffsetHeight();

    final int l1 = wrapper1.getOffsetLeft();
    final int t1 = wrapper1.getOffsetTop();
    final int w1 = wrapper1.getOffsetWidth();
    final int h1 = wrapper1.getOffsetHeight();

    after.setupLayers(layer0, layer1);
    delayTestFinish(TEST_DELAY);
    layout.layout(100, new Layout.AnimationCallback() {
      public void onAnimationComplete() {
        // Assert that the two layers have swapped positions.
        assertEquals(l0, wrapper1.getOffsetLeft());
        assertEquals(t0, wrapper1.getOffsetTop());
        assertEquals(w0, wrapper1.getOffsetWidth());
        assertEquals(h0, wrapper1.getOffsetHeight());

        assertEquals(l1, wrapper0.getOffsetLeft());
        assertEquals(t1, wrapper0.getOffsetTop());
        assertEquals(w1, wrapper0.getOffsetWidth());
        assertEquals(h1, wrapper0.getOffsetHeight());

        finishTest();
      }

      public void onLayout(Layer layer, double progress) {
      }
    });
  }

  private void testHorizontalSplit(Unit unit) {
    // Line them up horizontally, split at 5 units.
    layer0.setTopBottom(0, PX, 0, PX);
    layer0.setLeftWidth(0, PX, 5, unit);

    layer1.setTopBottom(0, PX, 0, PX);
    layer1.setLeftRight(5, unit, 0, PX);
    layout.layout();

    int child0Right = wrapper0.getOffsetWidth();
    int child1Left = wrapper1.getOffsetLeft();
    assertEquals(child0Right, child1Left);
  }

  private void testVerticalSplit(Unit unit) {
    // Line them up vertically, split at 5em.
    layer0.setTopHeight(0, PX, 5, unit);
    layer0.setLeftRight(0, PX, 0, PX);

    layer1.setTopBottom(5, unit, 0, PX);
    layer1.setLeftRight(0, PX, 0, PX);
    layout.layout();

    int child0Bottom = wrapper0.getOffsetHeight();
    int child1Top = wrapper1.getOffsetTop();
    assertEquals(child0Bottom, child1Top);
  }
}
