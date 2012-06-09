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
package elemental.js.html;
import elemental.js.dom.JsElement;
import elemental.dom.Element;
import elemental.events.EventListener;
import elemental.js.events.JsEventListener;
import elemental.html.BodyElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsBodyElement extends JsElement  implements BodyElement {
  protected JsBodyElement() {}

  public final native String getALink() /*-{
    return this.aLink;
  }-*/;

  public final native void setALink(String param_aLink) /*-{
    this.aLink = param_aLink;
  }-*/;

  public final native String getBackground() /*-{
    return this.background;
  }-*/;

  public final native void setBackground(String param_background) /*-{
    this.background = param_background;
  }-*/;

  public final native String getBgColor() /*-{
    return this.bgColor;
  }-*/;

  public final native void setBgColor(String param_bgColor) /*-{
    this.bgColor = param_bgColor;
  }-*/;

  public final native String getLink() /*-{
    return this.link;
  }-*/;

  public final native void setLink(String param_link) /*-{
    this.link = param_link;
  }-*/;

  public final native EventListener getOnbeforeunload() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onbeforeunload);
  }-*/;

  public final native void setOnbeforeunload(EventListener listener) /*-{
    this.onbeforeunload = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnhashchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onhashchange);
  }-*/;

  public final native void setOnhashchange(EventListener listener) /*-{
    this.onhashchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmessage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmessage);
  }-*/;

  public final native void setOnmessage(EventListener listener) /*-{
    this.onmessage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnoffline() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onoffline);
  }-*/;

  public final native void setOnoffline(EventListener listener) /*-{
    this.onoffline = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnonline() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ononline);
  }-*/;

  public final native void setOnonline(EventListener listener) /*-{
    this.ononline = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnpopstate() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onpopstate);
  }-*/;

  public final native void setOnpopstate(EventListener listener) /*-{
    this.onpopstate = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnresize() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onresize);
  }-*/;

  public final native void setOnresize(EventListener listener) /*-{
    this.onresize = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnstorage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onstorage);
  }-*/;

  public final native void setOnstorage(EventListener listener) /*-{
    this.onstorage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnunload() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onunload);
  }-*/;

  public final native void setOnunload(EventListener listener) /*-{
    this.onunload = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getText() /*-{
    return this.text;
  }-*/;

  public final native void setText(String param_text) /*-{
    this.text = param_text;
  }-*/;

  public final native String getVLink() /*-{
    return this.vLink;
  }-*/;

  public final native void setVLink(String param_vLink) /*-{
    this.vLink = param_vLink;
  }-*/;
}
