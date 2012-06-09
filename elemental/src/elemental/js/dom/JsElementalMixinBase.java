/*
 * Copyright 2012 Google Inc.
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
package elemental.js.dom;
import elemental.svg.SVGStringList;
import elemental.js.svg.JsSVGStringList;
import elemental.dom.ElementalMixinBase;
import elemental.js.events.JsEventListener;
import elemental.svg.SVGAnimatedLength;
import elemental.svg.SVGAnimatedString;
import elemental.js.svg.JsSVGAnimatedTransformList;
import elemental.css.CSSValue;
import elemental.js.svg.JsSVGAnimatedBoolean;
import elemental.js.svg.JsSVGElement;
import elemental.svg.SVGRect;
import elemental.dom.NodeList;
import elemental.dom.Element;
import elemental.svg.SVGAnimatedTransformList;
import elemental.svg.SVGAnimatedBoolean;
import elemental.svg.SVGMatrix;
import elemental.js.svg.JsSVGRect;
import elemental.svg.SVGElement;
import elemental.svg.SVGAnimatedPreserveAspectRatio;
import elemental.js.svg.JsSVGMatrix;
import elemental.js.svg.JsSVGAnimatedString;
import elemental.events.Event;
import elemental.js.css.JsCSSStyleDeclaration;
import elemental.js.events.JsEvent;
import elemental.js.svg.JsSVGAnimatedLength;
import elemental.svg.SVGAnimatedRect;
import elemental.events.EventListener;
import elemental.js.svg.JsSVGAnimatedRect;
import elemental.js.css.JsCSSValue;
import elemental.css.CSSStyleDeclaration;
import elemental.js.svg.JsSVGAnimatedPreserveAspectRatio;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.svg.*;
import elemental.js.util.JsElementalBase;

import java.util.Date;

/**
  * A base class containing all of the IDL interfaces which are shared
  * between disjoint type hierarchies. Because of the GWT compiler
  * SingleJsoImpl restriction that only a single JavaScriptObject
  * may implement a given interface, we hoist all of the explicit
  * mixin classes into a base JSO used by all of elemental.
  */
public class JsElementalMixinBase  extends JsElementalBase  implements ElementalMixinBase, EventTarget, SVGZoomAndPan, SVGLocatable, SVGLangSpace, SVGFilterPrimitiveStandardAttributes, ElementTimeControl, SVGTests, SVGURIReference, IndexableInt, IndexableNumber, SVGFitToViewBox, SVGTransformable, NodeSelector, SVGExternalResourcesRequired, SVGStylable, ElementTraversal {
  protected JsElementalMixinBase() {}

  public final native int getChildElementCount() /*-{
    return this.childElementCount;
  }-*/;

  public final native JsSVGAnimatedString getAnimatedClassName() /*-{
    return this.className;
  }-*/;

  public final native JsSVGAnimatedBoolean getExternalResourcesRequired() /*-{
    return this.externalResourcesRequired;
  }-*/;

  public final native JsSVGElement getFarthestViewportElement() /*-{
    return this.farthestViewportElement;
  }-*/;

  public final native JsElement getFirstElementChild() /*-{
    return this.firstElementChild;
  }-*/;

  public final native JsSVGAnimatedLength getAnimatedHeight() /*-{
    return this.height;
  }-*/;

  public final native JsSVGAnimatedString getAnimatedHref() /*-{
    return this.href;
  }-*/;

  public final native JsElement getLastElementChild() /*-{
    return this.lastElementChild;
  }-*/;

  public final native JsSVGElement getNearestViewportElement() /*-{
    return this.nearestViewportElement;
  }-*/;

  public final native JsElement getNextElementSibling() /*-{
    return this.nextElementSibling;
  }-*/;

  public final native JsSVGAnimatedPreserveAspectRatio getPreserveAspectRatio() /*-{
    return this.preserveAspectRatio;
  }-*/;

  public final native JsElement getPreviousElementSibling() /*-{
    return this.previousElementSibling;
  }-*/;

  public final native JsSVGStringList getRequiredExtensions() /*-{
    return this.requiredExtensions;
  }-*/;

  public final native JsSVGStringList getRequiredFeatures() /*-{
    return this.requiredFeatures;
  }-*/;

  public final native JsSVGAnimatedString getAnimatedResult() /*-{
    return this.result;
  }-*/;

  public final native JsCSSStyleDeclaration getSvgStyle() /*-{
    return this.style;
  }-*/;

  public final native JsSVGStringList getSystemLanguage() /*-{
    return this.systemLanguage;
  }-*/;

  public final native JsSVGAnimatedTransformList getAnimatedTransform() /*-{
    return this.transform;
  }-*/;

  public final native JsSVGAnimatedRect getViewBox() /*-{
    return this.viewBox;
  }-*/;

  public final native JsSVGAnimatedLength getAnimatedWidth() /*-{
    return this.width;
  }-*/;

  public final native JsSVGAnimatedLength getAnimatedX() /*-{
    return this.x;
  }-*/;

  public final native String getXmllang() /*-{
    return this.xmllang;
  }-*/;

  public final native void setXmllang(String param_xmllang) /*-{
    this.xmllang = param_xmllang;
  }-*/;

  public final native String getXmlspace() /*-{
    return this.xmlspace;
  }-*/;

  public final native void setXmlspace(String param_xmlspace) /*-{
    this.xmlspace = param_xmlspace;
  }-*/;

  public final native JsSVGAnimatedLength getAnimatedY() /*-{
    return this.y;
  }-*/;

  public final native int getZoomAndPan() /*-{
    return this.zoomAndPan;
  }-*/;

  public final native void setZoomAndPan(int param_zoomAndPan) /*-{
    this.zoomAndPan = param_zoomAndPan;
  }-*/;

  public final native boolean dispatchEvent(Event event) /*-{
    return this.dispatchEvent(event);
  }-*/;

  public final native JsSVGRect getBBox() /*-{
    return this.getBBox();
  }-*/;

  public final native JsSVGMatrix getCTM() /*-{
    return this.getCTM();
  }-*/;

  public final native JsSVGMatrix getScreenCTM() /*-{
    return this.getScreenCTM();
  }-*/;

  public final native JsSVGMatrix getTransformToElement(SVGElement element) /*-{
    return this.getTransformToElement(element);
  }-*/;

  public final native void beginElement() /*-{
    this.beginElement();
  }-*/;

  public final native void beginElementAt(float offset) /*-{
    this.beginElementAt(offset);
  }-*/;

  public final native void endElement() /*-{
    this.endElement();
  }-*/;

  public final native void endElementAt(float offset) /*-{
    this.endElementAt(offset);
  }-*/;

  public final native boolean hasExtension(String extension) /*-{
    return this.hasExtension(extension);
  }-*/;

  public final native JsElement querySelector(String selectors) /*-{
    return this.querySelector(selectors);
  }-*/;

  public final native JsNodeList querySelectorAll(String selectors) /*-{
    return this.querySelectorAll(selectors);
  }-*/;

  public final native JsCSSValue getPresentationAttribute(String name) /*-{
    return this.getPresentationAttribute(name);
  }-*/;


private static class Remover implements EventRemover {
  private final EventTarget target;
  private final String type;
  private final JavaScriptObject handler;
  private final boolean useCapture;

  private Remover(EventTarget target, String type, JavaScriptObject handler,
      boolean useCapture) {
    this.target = target;
    this.type = type;
    this.handler = handler;
    this.useCapture = useCapture;
  }

  @Override
  public void remove() {
    removeEventListener(target, type, handler, useCapture);
  }

  private static Remover create(EventTarget target, String type, JavaScriptObject handler,
      boolean useCapture) {
    return new Remover(target, type, handler, useCapture);
  }
}

// NOTES:
// - This handler/listener structure is currently the same in DevMode and ProdMode but it is
//   subject to change. In fact, I would like to use:
//     { listener : listener, handleEvent : function() }
//   but Firefox doesn't properly support that form of handler for onEvent type events.
// - The handler property on listener can be removed when removeEventListener is removed.
private native static JavaScriptObject createHandler(EventListener listener) /*-{
  var handler = listener.handler;
  if (!handler) {
    handler = $entry(function(event) {
      @elemental.js.dom.JsElementalMixinBase::handleEvent(Lelemental/events/EventListener;Lelemental/events/Event;)(listener, event);
    });
    handler.listener = listener;
    // TODO(knorton): Remove at Christmas when removeEventListener is removed.
    listener.handler = handler;
  }
  return handler;
}-*/;

private static class ForDevMode {
  private static java.util.Map<EventListener, JavaScriptObject> handlers;

  static {
    if (!com.google.gwt.core.client.GWT.isScript()) {
      handlers = new java.util.HashMap<EventListener, JavaScriptObject>();
    }
  }

  private static JavaScriptObject getHandlerFor(EventListener listener) {
    if (listener == null) {
      return null;
    }

    JavaScriptObject handler = handlers.get(listener);
    if (handler == null) {
      handler = createHandler(listener);
      handlers.put(listener, handler);
    }
    return handler;
  }

  private native static JavaScriptObject createHandler(EventListener listener) /*-{
    var handler = $entry(function(event) {
      @elemental.js.dom.JsElementalMixinBase::handleEvent(Lelemental/events/EventListener;Lelemental/events/Event;)(listener, event);
    });
    handler.listener = listener;
    return handler;
  }-*/;

  private native static EventListener getListenerFor(JavaScriptObject handler) /*-{
    return handler && handler.listener;
  }-*/;
}

private static class ForProdMode {
  private static JavaScriptObject getHandlerFor(EventListener listener) {
    return listener == null ? null : createHandler(listener);
  }

  private native static EventListener getListenerFor(JavaScriptObject handler) /*-{
    return handler && handler.listener;
  }-*/;
}

private static void handleEvent(EventListener listener, Event event) {
  listener.handleEvent(event);
}

private static EventListener getListenerFor(JavaScriptObject handler) {
  return com.google.gwt.core.client.GWT.isScript()
    ? ForProdMode.getListenerFor(handler)
    : ForDevMode.getListenerFor(handler);
}

private static JavaScriptObject getHandlerFor(EventListener listener) {
  return com.google.gwt.core.client.GWT.isScript()
    ? ForProdMode.getHandlerFor(listener)
    : ForDevMode.getHandlerFor(listener);
}
 
public native final EventRemover addEventListener(String type, EventListener listener, boolean useCapture) /*-{
  var handler = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  this.addEventListener(type, handler, useCapture);
  return @elemental.js.dom.JsElementalMixinBase.Remover::create(Lelemental/events/EventTarget;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Z)
      (this, type, handler, useCapture);
}-*/;

public native final EventRemover addEventListener(String type, EventListener listener) /*-{
  var handler = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  this.addEventListener(type, handler);
  return @elemental.js.dom.JsElementalMixinBase.Remover::create(Lelemental/events/EventTarget;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Z)
      (this, type, handler, useCapture);
}-*/;

@Deprecated
public final void removeEventListener(String type, EventListener listener, boolean useCapture) {
  final JavaScriptObject handler = getHandlerFor(listener);
  if (handler != null) {
    removeEventListener(this, type, handler, useCapture);
  }
}

@Deprecated
public final void removeEventListener(String type, EventListener listener) {
  final JavaScriptObject handler = getHandlerFor(listener);
  if (handler != null) {
    removeEventListener(this, type, handler);
  }
}

private static native void removeEventListener(EventTarget target, String type,
    JavaScriptObject handler, boolean useCapture) /*-{
  target.removeEventListener(type, handler, useCapture);
}-*/;

private static native void removeEventListener(EventTarget target, String type,
    JavaScriptObject handler) /*-{
  target.removeEventListener(type, handler);
}-*/;

}
