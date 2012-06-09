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
import elemental.events.EventListener;
import elemental.html.BatteryManager;
import elemental.js.events.JsEvent;
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

public class JsBatteryManager extends JsElementalMixinBase  implements BatteryManager {
  protected JsBatteryManager() {}

  public final native boolean isCharging() /*-{
    return this.charging;
  }-*/;

  public final native double getChargingTime() /*-{
    return this.chargingTime;
  }-*/;

  public final native double getDischargingTime() /*-{
    return this.dischargingTime;
  }-*/;

  public final native double getLevel() /*-{
    return this.level;
  }-*/;

  public final native EventListener getOnchargingchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onchargingchange);
  }-*/;

  public final native void setOnchargingchange(EventListener listener) /*-{
    this.onchargingchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnchargingtimechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onchargingtimechange);
  }-*/;

  public final native void setOnchargingtimechange(EventListener listener) /*-{
    this.onchargingtimechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndischargingtimechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondischargingtimechange);
  }-*/;

  public final native void setOndischargingtimechange(EventListener listener) /*-{
    this.ondischargingtimechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnlevelchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onlevelchange);
  }-*/;

  public final native void setOnlevelchange(EventListener listener) /*-{
    this.onlevelchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;}
