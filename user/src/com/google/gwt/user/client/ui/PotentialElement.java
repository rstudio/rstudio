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

  public static PotentialElement as(Element e) {
    assert isPotential(e);
    return (PotentialElement) e;
  }

  /**
   * Builds a new PotentialElement. This element keeps track of the
   * {@link UIObject} so that it can call
   * {@link UIObject#resolvePotentialElement} to get a real element when
   * that is needed.
   */
  public static native PotentialElement build(UIObject o) /*-{
    return @com.google.gwt.dom.client.Element::as(Lcom/google/gwt/core/client/JavaScriptObject;)({
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
      __gwt_resolve: @com.google.gwt.user.client.ui.PotentialElement::buildResolveCallback(Lcom/google/gwt/user/client/ui/UIObject;)(o),
      title: ''
    });
  }-*/;

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

  protected PotentialElement() {
  }

  final native Element setResolver(UIObject resolver) /*-{
    this.__gwt_resolve = @com.google.gwt.user.client.ui.PotentialElement::buildResolveCallback(Lcom/google/gwt/user/client/ui/UIObject;)(resolver);
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
