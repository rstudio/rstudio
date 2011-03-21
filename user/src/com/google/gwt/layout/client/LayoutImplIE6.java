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

import static com.google.gwt.dom.client.Style.Unit.PX;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Window;

/**
 * IE6-specific implementation, which uses the "onresize" event, along with a
 * series of measurement tools, to deal with several IE6 bugs. Specifically,
 * IE6 doesn't support simultaneous left-right and top-bottom values, and it
 * misplaces by one pixel elements whose right/bottom properties are set.
 * 
 * Because this implementation gets compiled in for both IE6 and 7, it
 * dynamically detects IE7 and punts to the super implementation.
 */
class LayoutImplIE6 extends LayoutImplIE8 {

  private static Element createStyleRuler(Element parent) {
    DivElement styleRuler = Document.get().createDivElement();
    DivElement styleInner = Document.get().createDivElement();

    styleRuler.getStyle().setPosition(Position.ABSOLUTE);
    styleRuler.getStyle().setLeft(-10000, PX);

    parent.appendChild(styleRuler);
    styleRuler.appendChild(styleInner);
    return styleRuler;
  }

  private static void hookWindowResize(final Element elem) {
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        resizeRelativeToParent(elem);
      }
    });
  }

  private static native void measureDecoration(Element elem) /*-{
    var ruler = elem.__styleRuler;
    var inner = ruler.children[0];
    var s = inner.style, cs = elem.currentStyle;

    s.borderLeftStyle = cs.borderLeftStyle;
    s.borderRightStyle = cs.borderRightStyle;
    s.borderTopStyle = cs.borderTopStyle;
    s.borderBottomStyle = cs.borderBottomStyle;

    s.borderLeftWidth = cs.borderLeftWidth;
    s.borderRightWidth = cs.borderRightWidth;
    s.borderTopWidth = cs.borderTopWidth;
    s.borderBottomWidth = cs.borderBottomWidth;

    // Oddly enough, allowing the word 'auto' to creep into the style
    // ruler's margin causes it to take up all of its parent's space.
    s.marginLeft = (cs.marginLeft == 'auto') ? '' : cs.marginLeft;
    s.marginRight = (cs.marginRight == 'auto') ? '' : cs.marginRight;
    s.marginTop = (cs.marginTop == 'auto') ? '' : cs.marginTop;
    s.marginBottom = (cs.marginBottom == 'auto') ? '' : cs.marginBottom;

    s.paddingLeft = cs.paddingLeft;
    s.paddingRight = cs.paddingRight;
    s.paddingTop = cs.paddingTop;
    s.paddingBottom = cs.paddingBottom;

    s.width = s.height = 32;
    elem.__decoWidth = ruler.offsetWidth - 32;
    elem.__decoHeight = ruler.offsetHeight - 32;
  }-*/;

  private static native void resizeRelativeToParent(Element elem) /*-{
    var parent = elem.__resizeParent;
    if (parent) {
      @com.google.gwt.layout.client.LayoutImplIE6::measureDecoration(Lcom/google/gwt/dom/client/Element;)(elem);
      elem.style.left = 0;
      elem.style.top = 0;
      elem.style.width = parent.clientWidth - elem.__decoWidth;
      elem.style.height = parent.clientHeight - elem.__decoHeight;
    }
  }-*/;

  private static native void setLayer(Element container, Layer layer) /*-{
    // Potential leak: This is cleaned up in detach().
    container.__layer = layer;
  }-*/;

  private static native void setPropertyElement(Element elem, String name,
      Element value) /*-{
    elem[name] = value;
  }-*/;

  @Override
  public Element attachChild(Element parent, Element child, Element before) {
    DivElement container = Document.get().createDivElement();
    container.appendChild(child);

    container.getStyle().setPosition(Position.ABSOLUTE);
    container.getStyle().setOverflow(Overflow.HIDDEN);
    child.getStyle().setPosition(Position.ABSOLUTE);

    // Hang the style ruler from the container element, but associate it with
    // the child element, so that measureDecoration(child) will work.
    setPropertyElement(child, "__styleRuler", createStyleRuler(container));

    Element beforeContainer = null;
    if (before != null) {
      beforeContainer = before.getParentElement();
      assert beforeContainer.getParentElement() == parent : "Element to insert before must be a sibling";
    }
    parent.insertBefore(container, beforeContainer);
    return container;
  }

  @Override
  public void fillParent(Element elem) {
    fillParentImpl(elem);
  }

  @Override
  public void finalizeLayout(Element parent) {
    resizeRelativeToParent(parent);
    resizeHandler(parent, true);
  }

  @Override
  public void initParent(Element parent) {
    super.initParent(parent);
    setPropertyElement(parent, "__styleRuler", createStyleRuler(parent));
  }

  @Override
  public void layout(Layer layer) {
    Element elem = layer.container;
    Style style = elem.getStyle();

    if (layer.visible) {
      style.clearDisplay();
    } else {
      style.setDisplay(Display.NONE);
    }

    setLayer(elem, layer);
  }

  @Override
  public void onAttach(Element parent) {
    // No need to re-connect layer refs. This will be taken care of
    // automatically in layout().
    initResizeHandler(parent);
    initUnitChangeHandler(parent, relativeRuler);
  }

  @Override
  public void onDetach(Element parent) {
    removeLayerRefs(parent);
    removeResizeHandler(parent);
    removeUnitChangeHandler(relativeRuler);
  }

  private native void fillParentImpl(Element elem) /*-{
    // Hook the parent element's onresize event. If the parent is the <body>,
    // then we have to go through the Window class to get the resize event,
    // because IE6 requires a bunch of foul hackery to safely hook it.
    var parent = elem.parentElement;
    if (parent.tagName.toLowerCase() == 'body') {
      elem.style.position = 'absolute';
      var docElem = parent.parentElement;
      elem.__resizeParent = docElem;
      @com.google.gwt.layout.client.LayoutImplIE6::hookWindowResize(Lcom/google/gwt/dom/client/Element;)(elem);
      return;
    }

    function resize() {
      @com.google.gwt.layout.client.LayoutImplIE6::resizeRelativeToParent(Lcom/google/gwt/dom/client/Element;)(elem);
    }

    elem.__resizeParent = parent;
    parent.onresize = resize;
    resize();
  }-*/;

  /**
   * This does not call $entry() because no user code is reachable from
   * resizeHandler.
   */
  private native void initResizeHandler(Element parent) /*-{
    // Potential leak: This is cleaned up in detach().
    var self = this;
    parent.onresize = function() {
      self.@com.google.gwt.layout.client.LayoutImplIE6::resizeHandler(Lcom/google/gwt/dom/client/Element;)(parent);
    };
  }-*/;

  /**
   * This does not call $entry() because no user code is reachable from
   * resizeHandler.
   */
  private native void initUnitChangeHandler(Element parent, Element ruler) /*-{
    // Potential leak: This is cleaned up in detach().
    var self = this;
    ruler.onresize = function() {
      self.@com.google.gwt.layout.client.LayoutImplIE6::resizeHandler(Lcom/google/gwt/dom/client/Element;Z)(parent, true);
    };
  }-*/;

  private native void removeLayerRefs(Element parent) /*-{
    for (var i = 0; i < parent.childNodes.length; ++i) {
      var container = parent.childNodes[i];
      if (container.__layer) {
        container.__layer = null;
      }
    }
  }-*/;

  private native void removeResizeHandler(Element parent) /*-{
    parent.onresize = null;
  }-*/;

  private native void removeUnitChangeHandler(Element ruler) /*-{
    ruler.onresize = null;
  }-*/;

  private void resizeHandler(Element parent) {
    resizeHandler(parent, false);
  }
  
  private native void resizeHandler(Element parent, boolean force) /*-{
    if (!force &&
        ((parent.offsetWidth == parent.__oldWidth) &&
         (parent.offsetHeight == parent.__oldHeight))) {
      return;
    }
    parent.__oldWidth = parent.offsetWidth;
    parent.__oldHeight = parent.offsetHeight;

    var parentWidth = parent.clientWidth;
    var parentHeight = parent.clientHeight;

    // Iterate over the children, assuming each of them is an unstyled
    // container element.
    for (var i = 0; i < parent.childNodes.length; ++i) {
      // Don't attempt to layout the rulers.
      var container = parent.childNodes[i];
      var layer = container.__layer;
      if (!layer) {
        continue;
      }

      // This is ugly, but it's less ugly than writing all these JSNI refs inline.
      var _setLeft = layer.@com.google.gwt.layout.client.Layout.Layer::setLeft;
      var _setTop = layer.@com.google.gwt.layout.client.Layout.Layer::setTop;
      var _setWidth = layer.@com.google.gwt.layout.client.Layout.Layer::setWidth;
      var _setHeight = layer.@com.google.gwt.layout.client.Layout.Layer::setHeight;
      var _setRight = layer.@com.google.gwt.layout.client.Layout.Layer::setRight;
      var _setBottom = layer.@com.google.gwt.layout.client.Layout.Layer::setBottom;

      var _left = layer.@com.google.gwt.layout.client.Layout.Layer::left;
      var _top = layer.@com.google.gwt.layout.client.Layout.Layer::top;
      var _width = layer.@com.google.gwt.layout.client.Layout.Layer::width;
      var _height = layer.@com.google.gwt.layout.client.Layout.Layer::height;
      var _right = layer.@com.google.gwt.layout.client.Layout.Layer::right;
      var _bottom = layer.@com.google.gwt.layout.client.Layout.Layer::bottom;

      var _leftUnit = layer.@com.google.gwt.layout.client.Layout.Layer::leftUnit;
      var _topUnit = layer.@com.google.gwt.layout.client.Layout.Layer::topUnit;
      var _widthUnit = layer.@com.google.gwt.layout.client.Layout.Layer::widthUnit;
      var _heightUnit = layer.@com.google.gwt.layout.client.Layout.Layer::heightUnit;
      var _rightUnit = layer.@com.google.gwt.layout.client.Layout.Layer::rightUnit;
      var _bottomUnit = layer.@com.google.gwt.layout.client.Layout.Layer::bottomUnit;

      var _hPos = layer.@com.google.gwt.layout.client.Layout.Layer::hPos
                  .@com.google.gwt.layout.client.Layout.Alignment::ordinal()();
      var _vPos = layer.@com.google.gwt.layout.client.Layout.Layer::vPos
                  .@com.google.gwt.layout.client.Layout.Alignment::ordinal()();

      // Apply the requested position & size values to the element's style.
      var style = container.style;
      style.left = _setLeft ? (_left + _leftUnit.@com.google.gwt.dom.client.Style.Unit::getType()()) : "";
      style.top = _setTop ? (_top + _topUnit.@com.google.gwt.dom.client.Style.Unit::getType()()) : "";
      style.width = _setWidth ? (_width + _widthUnit.@com.google.gwt.dom.client.Style.Unit::getType()()) : "";
      style.height = _setHeight ? (_height + _heightUnit.@com.google.gwt.dom.client.Style.Unit::getType()()) : "";

      // If right is defined, reposition/size the container horizontally.
      if (_setRight) {
        var ratio = this.@com.google.gwt.layout.client.LayoutImplIE6::getUnitSizeInPixels(Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/dom/client/Style$Unit;Z)(parent, _rightUnit, false);
        var right = parentWidth - (_right * ratio);

        if (!_setLeft) {
          // There's no left defined; adjust the left position move the element to the right edge.
          container.style.left = (right - container.offsetWidth) + 'px';
        } else {
          // Both left and right are defined; calculate the width and set it.
          var leftRatio = this.@com.google.gwt.layout.client.LayoutImplIE6::getUnitSizeInPixels(Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/dom/client/Style$Unit;Z)(parent, _leftUnit, false);
          var left = (_left * leftRatio);
          if (right > left) {
            container.style.width = (right - left) + 'px';
          }
        }
      }

      // If bottom is defined, reposition/size the container vertically.
      if (_setBottom) {
        var ratio = this.@com.google.gwt.layout.client.LayoutImplIE6::getUnitSizeInPixels(Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/dom/client/Style$Unit;Z)(parent, _bottomUnit, true);
        var bottom = parentHeight - (_bottom * ratio);

        if (!_setTop) {
          // There's no top defined; adjust the left position move the element to the bottom edge.
          container.style.top = (bottom - container.offsetHeight) + 'px';
        } else {
          // Both top and bottom are defined; calculate the height and set it.
          var topRatio = this.@com.google.gwt.layout.client.LayoutImplIE6::getUnitSizeInPixels(Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/dom/client/Style$Unit;Z)(parent, _topUnit, true);
          var top = (_top * topRatio);
          if (bottom > top) {
            container.style.height = (bottom - top) + 'px';
          }
        }
      }

      // Resize and position the child based on the layer's [hv]Pos.
      var child = container.firstChild;
      @com.google.gwt.layout.client.LayoutImplIE6::measureDecoration(Lcom/google/gwt/dom/client/Element;)(child);
      var childDecoWidth = child.__decoWidth;
      var childDecoHeight = child.__decoHeight;

      if (container.offsetWidth > childDecoWidth) {
        switch (_hPos) {
          case 0: // BEGIN
            child.style.left = '0px';
            break;
          case 1: // END
            child.style.left = (container.offsetWidth - childDecoWidth - child.offsetWidth) + 'px';
            break;
          case 2: // STRETCH
            child.style.left = '0px';
            child.style.width = (container.offsetWidth - childDecoWidth) + 'px';
            break;
        }
      }
      if (container.offsetHeight > childDecoHeight) {
        switch (_vPos) {
          case 0: // BEGIN
            child.style.top = '0px';
            break;
          case 1: // END
            child.style.top = (container.offsetHeight - childDecoHeight - child.offsetHeight) + 'px';
            break;
          case 2: // STRETCH
            child.style.top = '0px';
            child.style.height = (container.offsetHeight - childDecoHeight) + 'px';
            break;
        }
      }
    }
  }-*/;
}
