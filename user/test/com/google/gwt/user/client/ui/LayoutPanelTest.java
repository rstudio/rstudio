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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.layout.client.LayerFriend;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link LayoutPanel}. Note that this only tests LayoutPanel-specific
 * behavior, not general layout correctness, which is covered by
 * {@link com.google.gwt.layout.client.LayoutTest}.
 */
public class LayoutPanelTest extends WidgetTestBase {

  /**
   * Tests for a bug in LayoutCommand, which caused an animate() call, just
   * before an unnecessary forceLayout(), to get stuck. See issue 4360.
   */
  @SuppressWarnings("deprecation")
  public void testRedundantForceLayout() {
    final LayoutPanel p = new LayoutPanel();
    Label l = new Label("foo");
    p.add(l);

    p.setWidgetTopHeight(l, 0, Unit.PX, 10, Unit.PX);
    p.forceLayout();

    delayTestFinish(5000);
    // Fully qualified to avoid the deprecation warning in the import section
    com.google.gwt.user.client.DeferredCommand.addCommand(new Command() {
      @Override
      public void execute() {
        p.animate(100, new AnimationCallback() {
          @Override
          public void onLayout(Layer layer, double progress) {
          }

          @Override
          public void onAnimationComplete() {
            // If LayoutCommand is broken, this will never happen.
            finishTest();
          }
        });
      }
    });
  }

  /**
   * Ensures that the popup implementation doesn't interfere with layout. This
   * cropped up on IE7 as a result of CSS expressions used in PopupImplIE6, as
   * described in issue 4532.
   */
  @SuppressWarnings("deprecation")
  public void testWeirdPopupInteraction() {
    assertTrue(Document.get().isCSS1Compat());

    final LayoutPanel lp = new LayoutPanel();
    lp.add(new HTML("foo"));
    RootLayoutPanel.get().add(lp);

    PopupPanel popup = new PopupPanel();
    popup.center();

    delayTestFinish(2000);
    // Fully qualified to avoid the deprecation warning in the import section
    com.google.gwt.user.client.DeferredCommand.addCommand(new Command() {
      @Override
      public void execute() {
        int offsetWidth = lp.getOffsetWidth();
        int offsetHeight = lp.getOffsetHeight();
        assertTrue(offsetWidth > 0);
        assertTrue(offsetHeight > 0);
        finishTest();
      }
    });
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetBottomHeight(Widget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetBottomHeight() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    layoutPanel.setWidgetBottomHeight(label, 1.5, Unit.EM, 2.0, Unit.EM);

    assertLayerProperties(
        label,
        new LayerProperties().bottom(1.5).bottomUnit(Unit.EM).height(2.0).heightUnit(
            Unit.EM));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetBottomHeight(IsWidget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetBottomHeightAsIsWidget() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    // IsWidget cast to call the overloaded version
    layoutPanel.setWidgetBottomHeight((IsWidget) label, 10.0, Unit.PX, 15.0,
        Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().bottom(10.0).bottomUnit(Unit.PX).height(15.0).heightUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetLeftRight(Widget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetLeftRight() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    layoutPanel.setWidgetLeftRight(label, 10.0, Unit.PX, 20.0, Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().left(10.0).leftUnit(Unit.PX).right(20.0).rightUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetLeftRight(IsWidget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetLeftRightAsIsWidget() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    // IsWidget cast to call the overloaded version
    layoutPanel.setWidgetLeftRight((IsWidget) label, 10.0, Unit.PX, 15.0,
        Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().left(10.0).leftUnit(Unit.PX).right(15.0).rightUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetLeftWidth(Widget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetLeftWidth() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    layoutPanel.setWidgetLeftWidth(label, 10.0, Unit.PX, 20.0, Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().left(10.0).leftUnit(Unit.PX).width(20.0).widthUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetLeftWidth(IsWidget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetLeftWidthAsIsWidget() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    // IsWidget cast to call the overloaded version
    layoutPanel.setWidgetLeftWidth((IsWidget) label, 10.0, Unit.PX, 15.0,
        Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().left(10.0).leftUnit(Unit.PX).width(15.0).widthUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetRightWidth(Widget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetRightWidth() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    layoutPanel.setWidgetRightWidth(label, 10.0, Unit.PX, 20.0, Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().right(10.0).rightUnit(Unit.PX).width(20.0).widthUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetRightWidth(IsWidget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetRightWidthAsIsWidget() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    // IsWidget cast to call the overloaded version
    layoutPanel.setWidgetRightWidth((IsWidget) label, 10.0, Unit.PX, 15.0,
        Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().right(10.0).rightUnit(Unit.PX).width(15.0).widthUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetTopBottom(Widget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetTopBottom() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    layoutPanel.setWidgetTopBottom(label, 10.0, Unit.PX, 20.0, Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().top(10.0).topUnit(Unit.PX).bottom(20.0).bottomUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetTopBottom(IsWidget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetTopBottomAsIsWidget() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    // IsWidget cast to call the overloaded version
    layoutPanel.setWidgetTopBottom((IsWidget) label, 10.0, Unit.PX, 15.0,
        Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().top(10.0).topUnit(Unit.PX).bottom(15.0).bottomUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetTopHeight(Widget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetTopHeight() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    layoutPanel.setWidgetTopHeight(label, 10.0, Unit.PX, 20.0, Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().top(10.0).topUnit(Unit.PX).height(20.0).heightUnit(
            Unit.PX));
  }

  /**
   * Tests
   * {@link LayoutPanel#setWidgetTopHeight(IsWidget, double, Unit, double, Unit)}
   * .
   */
  public void testSetWidgetTopHeightAsIsWidget() {
    LayoutPanel layoutPanel = createLayoutPanel();
    Widget label = createLabelInPanel(layoutPanel);

    // IsWidget cast to call the overloaded version
    layoutPanel.setWidgetTopHeight((IsWidget) label, 10.0, Unit.PX, 15.0,
        Unit.PX);

    assertLayerProperties(
        label,
        new LayerProperties().top(10.0).topUnit(Unit.PX).height(15.0).heightUnit(
            Unit.PX));
  }

  /**
   * Test that forcing layout will call onResize only once.
   */
  public void testForceLayoutNoRedundantOnResize() {
    final List<Boolean> called = new ArrayList<>();
    LayoutPanel layoutPanel = new LayoutPanel();
    SimpleLayoutPanel child = new SimpleLayoutPanel() {
      @Override
      public void onResize() {
        super.onResize();
        called.add(true);
      }
    };
    layoutPanel.add(child);
    layoutPanel.forceLayout();
    assertEquals(1,called.size());
  }

  protected LayoutPanel createLayoutPanel() {
    return new LayoutPanel();
  }

  protected Widget createLabelInPanel(LayoutPanel layoutPanel) {
    Widget label = new Label("foo");
    layoutPanel.add(label);
    return label;
  }

  /**
   * <p>
   * Parameter class that hold a set of properties of a {@link Layout.Layer}. The
   * default value for all the six properties (top, right, bottom, left, height
   * and width) is 0.0, value that matches the default values for this
   * properties in the actual {@link Layout.Layer}. The default values for the
   * units also match the default values in {@link Layout.Layer}: <br>
   * <code>topUnit = rightUnit = bottomUnit = leftUnit = {@link Unit#PX}</code>
   * <br>
   * <code>heigthUnit = widthUnit = null</code>
   * </p>
   * This class must be used as a parameter of
   * {@link LayoutPanelTest#assertLayerProperties(Widget, LayerProperties)}, and
   * was thought to be used as a builder in order to improve readability and
   * reduce errors with the parameters of
   * {@link LayoutPanelTest#assertLayerProperties(Widget, LayerProperties)}.
   * </p>
   * 
   * <p>
   * <h3>Example:</h3>
   * <code>assertLayerProperties(label,
   *    new LayerProperties().bottom(10.0).bottomUnit(Unit.PX)
   *    .height(15.0).heightUnit(Unit.PX);</code>
   * </p>
   */
  static class LayerProperties {
    private double top, right, bottom, left, height, width;
    private Unit topUnit = Unit.PX, rightUnit = Unit.PX, bottomUnit = Unit.PX,
        leftUnit = Unit.PX, heightUnit, widthUnit;

    LayerProperties top(double top) {
      this.top = top;
      return this;
    }

    double getTop() {
      return top;
    }

    LayerProperties topUnit(Unit topUnit) {
      this.topUnit = topUnit;
      return this;
    }

    Unit getTopUnit() {
      return topUnit;
    }

    LayerProperties right(double right) {
      this.right = right;
      return this;
    }

    double getRight() {
      return right;
    }

    LayerProperties rightUnit(Unit rightUnit) {
      this.rightUnit = rightUnit;
      return this;
    }

    Unit getRightUnit() {
      return rightUnit;
    }

    LayerProperties bottom(double bottom) {
      this.bottom = bottom;
      return this;
    }

    double getBottom() {
      return bottom;
    }

    LayerProperties bottomUnit(Unit bottomUnit) {
      this.bottomUnit = bottomUnit;
      return this;
    }

    Unit getBottomUnit() {
      return bottomUnit;
    }

    LayerProperties left(double left) {
      this.left = left;
      return this;
    }

    double getLeft() {
      return left;
    }

    LayerProperties leftUnit(Unit leftUnit) {
      this.leftUnit = leftUnit;
      return this;
    }

    Unit getLeftUnit() {
      return leftUnit;
    }

    LayerProperties height(double height) {
      this.height = height;
      return this;
    }

    double getHeight() {
      return height;
    }

    LayerProperties heightUnit(Unit heightUnit) {
      this.heightUnit = heightUnit;
      return this;
    }

    Unit getHeightUnit() {
      return heightUnit;
    }

    LayerProperties width(double width) {
      this.width = width;
      return this;
    }

    double getWidth() {
      return width;
    }

    LayerProperties widthUnit(Unit widthUnit) {
      this.widthUnit = widthUnit;
      return this;
    }

    Unit getWidthUnit() {
      return widthUnit;
    }
  }
  
  /**
   * <p>
   * Asserts that the layer properties of the <b>widget</b> are the given ones.
   * </p>
   * 
   * @param widget the widget being tested
   * @param expectedLayerProperties the expected properties of <b>widget</b>
   */
  private void assertLayerProperties(final Widget widget,
      final LayerProperties expectedLayerProperties) {
    delayTestFinish(2000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {

      @Override
      public void execute() {
        Layer layer = (Layout.Layer) widget.getLayoutData();
        LayerFriend helper = new LayerFriend(layer);

        assertEquals("Top", expectedLayerProperties.getTop(), helper.getTop());
        assertEquals("Right", expectedLayerProperties.getRight(),
            helper.getRight());
        assertEquals("Bottom", expectedLayerProperties.getBottom(),
            helper.getBottom());
        assertEquals("Left", expectedLayerProperties.getLeft(),
            helper.getLeft());
        assertEquals("Height", expectedLayerProperties.getHeight(),
            helper.getHeight());
        assertEquals("Width", expectedLayerProperties.getWidth(),
            helper.getWidth());

        assertEquals("Top Units", expectedLayerProperties.getTopUnit(),
            helper.getTopUnit());
        assertEquals("Right Units", expectedLayerProperties.getRightUnit(),
            helper.getRightUnit());
        assertEquals("Bottom Units", expectedLayerProperties.getBottomUnit(),
            helper.getBottomUnit());
        assertEquals("Left Units", expectedLayerProperties.getLeftUnit(),
            helper.getLeftUnit());
        assertEquals("Height Units", expectedLayerProperties.getHeightUnit(),
            helper.getHeightUnit());
        assertEquals("Width Units", expectedLayerProperties.getWidthUnit(),
            helper.getWidthUnit());
        finishTest();
      }
    });
  }
}
