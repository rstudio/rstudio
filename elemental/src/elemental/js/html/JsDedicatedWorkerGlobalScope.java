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
import elemental.html.DedicatedWorkerGlobalScope;
import elemental.util.Indexable;
import elemental.html.WorkerGlobalScope;
import elemental.js.util.JsIndexable;
import elemental.events.EventListener;
import elemental.js.events.JsEventListener;

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

public class JsDedicatedWorkerGlobalScope extends JsWorkerGlobalScope  implements DedicatedWorkerGlobalScope {
  protected JsDedicatedWorkerGlobalScope() {}

  public final native EventListener getOnmessage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmessage);
  }-*/;

  public final native void setOnmessage(EventListener listener) /*-{
    this.onmessage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native void postMessage(Object message) /*-{
    this.postMessage(message);
  }-*/;

  public final native void postMessage(Object message, Indexable messagePorts) /*-{
    this.postMessage(message, messagePorts);
  }-*/;

  public final native void webkitPostMessage(Object message) /*-{
    this.webkitPostMessage(message);
  }-*/;

  public final native void webkitPostMessage(Object message, Indexable transferList) /*-{
    this.webkitPostMessage(message, transferList);
  }-*/;
}
