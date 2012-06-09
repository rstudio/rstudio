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
import elemental.html.NotificationPermissionCallback;
import elemental.js.events.JsEvent;
import elemental.html.Notification;
import elemental.events.EventListener;
import elemental.js.events.JsEventListener;
import elemental.events.Event;

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

public class JsNotification extends JsElementalMixinBase  implements Notification {
  protected JsNotification() {}

  public final native String getDir() /*-{
    return this.dir;
  }-*/;

  public final native void setDir(String param_dir) /*-{
    this.dir = param_dir;
  }-*/;

  public final native EventListener getOnclick() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onclick);
  }-*/;

  public final native void setOnclick(EventListener listener) /*-{
    this.onclick = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnclose() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onclose);
  }-*/;

  public final native void setOnclose(EventListener listener) /*-{
    this.onclose = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndisplay() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondisplay);
  }-*/;

  public final native void setOndisplay(EventListener listener) /*-{
    this.ondisplay = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnshow() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onshow);
  }-*/;

  public final native void setOnshow(EventListener listener) /*-{
    this.onshow = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getReplaceId() /*-{
    return this.replaceId;
  }-*/;

  public final native void setReplaceId(String param_replaceId) /*-{
    this.replaceId = param_replaceId;
  }-*/;

  public final native String getTag() /*-{
    return this.tag;
  }-*/;

  public final native void setTag(String param_tag) /*-{
    this.tag = param_tag;
  }-*/;

  public final native void cancel() /*-{
    this.cancel();
  }-*/;

  public final native void close() /*-{
    this.close();
  }-*/;

  public final native void show() /*-{
    this.show();
  }-*/;
}
