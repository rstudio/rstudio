/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.builder.shared.HtmlElementBuilder;
import com.google.gwt.dom.builder.shared.HtmlBuilderFactory;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * A simple {@link Element} implementation (<strong>not</strong> an actual dom
 * object) that can serve as stand in to be used by {@link IsRenderable} widgets
 * before they are fully built. For example, it can accumulate simple set*()
 * values to be used when the widget is actually ready to render. Thus, most
 * {@link IsRenderable} widget code can be written without taking into account
 * whether or not the widget has yet been rendered.
 * <p>
 * {@link DOM#appendChild} is aware of PotentialElement, and calls its
 * resolve() method. This triggers a call to
 * {@link UIObject#resolvePotentialElement()}, which widgets can customize
 * to get a real {@link Element} in place at the last moment.
 *
 * TODO(rdcastro): Cover all unsupported methods with helpful error messages.
 */
public class PotentialElement extends Element {

  static {
    declareShim();
  }

  public static PotentialElement as(Element e) {
    assert isPotential(e);
    return (PotentialElement) e;
  }

  /**
   * Builds a new PotentialElement with the tag name set to "div".
   *
   * @see #build(UIObject,String)
   */
  public static PotentialElement build(UIObject o) {
    return build(o, "div");
  }

  /**
   * Builds a new PotentialElement. This element keeps track of the
   * {@link UIObject} so that it can call
   * {@link UIObject#resolvePotentialElement} to get a real element when
   * that is needed.
   */
  public static native PotentialElement build(UIObject o, String tagName) /*-{
    var el = new $wnd.GwtPotentialElementShim();
    el.tagName = tagName;
    el.__gwt_resolve = @com.google.gwt.user.client.ui.PotentialElement::buildResolveCallback(Lcom/google/gwt/user/client/ui/UIObject;)(o);
    return @com.google.gwt.dom.client.Element::as(Lcom/google/gwt/core/client/JavaScriptObject;)(el);
  }-*/;

  /**
   * Creates an {@link HtmlElementBuilder} instance inheriting all attributes
   * set for the given PotentialElement.
   *
   * @param potentialElement assumed to be a PotentialElement, used as basis for
   *     the builder
   * @return a propertly configured {@link HtmlElementBuilder} instance
   */
  public static HtmlElementBuilder createBuilderFor(Element potentialElement) {
    PotentialElement el = PotentialElement.as(potentialElement);
    HtmlElementBuilder builder = HtmlBuilderFactory.get().trustedCreate(
        el.getTagName());
    el.mergeInto(builder);
    return builder;
  }

  /**
   * Tests whether a given {@link JavaScriptObject} represents a PotentialElement.
   *
   * @param o the {@link JavaScriptObject} to be tested
   * @return true if the given object is a PotentialElement instance
   */
  public static native boolean isPotential(JavaScriptObject o) /*-{
    try {
      return (!!o) &&  (!!o.__gwt_resolve);
    } catch (e) {
      return false;
    }
  }-*/;

  /**
   * If given a PotentialElement, returns the real Element to be
   * built from it. Otherwise returns the given Element itself.
   * <p>
   * Note that a PotentialElement can only be resolved once.
   * Making repeated calls to this method with the same PotentialElement
   * is an error.
   */
  public static Element resolve(Element maybePotential) {
    return maybePotential.<PotentialElement>cast().resolve();
  }

  private static native JavaScriptObject buildResolveCallback(UIObject resolver) /*-{
    return function() {
        this.__gwt_resolve = @com.google.gwt.user.client.ui.PotentialElement::cannotResolveTwice();
        return resolver.@com.google.gwt.user.client.ui.UIObject::resolvePotentialElement()();
      };
  }-*/;

  private static final native void cannotResolveTwice() /*-{
    throw "A PotentialElement cannot be resolved twice.";
  }-*/;

  private static final native void declareShim() /*-{
    var shim = function() {};
    shim.prototype = {
      className: '',
      clientHeight: 0,
      clientWidth: 0,
      dir: '',
      getAttribute: function(name, value) {
        return this[name];
      },
      href: '',
      id: '',
      lang: '',
      // should be @com.google.gwt.dom.client.Node.ELEMENT_MODE, but the compiler
      // doesn't like that.
      nodeType: 1,
      removeAttribute: function(name, value) {
        this[name] = undefined;
      },
      setAttribute: function(name, value) {
        this[name] = value;
      },
      src: '',
      style: {},
      title: ''
    };
    $wnd.GwtPotentialElementShim = shim;
  }-*/;

  protected PotentialElement() {
  }

  final native Element setResolver(UIObject resolver) /*-{
    this.__gwt_resolve = @com.google.gwt.user.client.ui.PotentialElement::buildResolveCallback(Lcom/google/gwt/user/client/ui/UIObject;)(resolver);
  }-*/;

  /**
   * Copy only the fields that have actually changed from the values in the shim
   * prototype. Do this by severing the __proto__ link, allowing us to iterate
   * only on the fields set in this specific instance.
   */
  private native void mergeInto(HtmlElementBuilder builder) /*-{
    var savedProto = this.__proto__;
    var tagName = this.tagName;
    var gwtResolve = this.__gwt_resolve;
    var className = this.className;

    try {
      this.__proto__ = null;
      this.tagName = null;
      this.__gwt_resolve = null;

      // className needs special treatment because the actual HTML attribute is
      // called "class" and not "className".
      if (this.className) {
        builder.@com.google.gwt.dom.builder.shared.ElementBuilder::className(Ljava/lang/String;)(
            this.className);
        this.className = null;
      }

      // Iterate over all attributes, and copy them to the ElementBuilder.
      // TODO(rdcastro): Deal with the "style" attribute.
      for (attr in this) {
        if (!this[attr]) {
          continue;
        }
        if (typeof this[attr] == 'number') {
          builder.@com.google.gwt.dom.builder.shared.ElementBuilder::attribute(Ljava/lang/String;I)(
              attr, this[attr]);
        } else if (typeof this[attr] == 'string') {
          builder.@com.google.gwt.dom.builder.shared.ElementBuilder::attribute(Ljava/lang/String;Ljava/lang/String;)(
              attr, this[attr]);
        }
      }
    } finally {
      this.__proto__ = savedProto;
      if (className) {
        this.className = className;
      }
      this.__gwt_resolve = gwtResolve;
      this.tagName = tagName;
    }
  }-*/;

  /**
   * Calls the <code>__gwt_resolve</code> method on the underlying
   * JavaScript object if it exists. On objects created via {@link #build}, this
   * method is a call to the {@link UIObject#resolvePotentialElement} method
   * on the associated UIObject.
   */
  private native Element resolve() /*-{
    return this.__gwt_resolve ? this.__gwt_resolve() : this;
  }-*/;
}
