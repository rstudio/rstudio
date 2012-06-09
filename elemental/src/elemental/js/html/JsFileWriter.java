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
import elemental.html.FileError;
import elemental.html.FileWriter;
import elemental.js.events.JsEvent;
import elemental.html.Blob;
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

public class JsFileWriter extends JsElementalMixinBase  implements FileWriter {
  protected JsFileWriter() {}

  public final native JsFileError getError() /*-{
    return this.error;
  }-*/;

  public final native double getLength() /*-{
    return this.length;
  }-*/;

  public final native EventListener getOnabort() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onabort);
  }-*/;

  public final native void setOnabort(EventListener listener) /*-{
    this.onabort = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnprogress() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onprogress);
  }-*/;

  public final native void setOnprogress(EventListener listener) /*-{
    this.onprogress = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwrite() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwrite);
  }-*/;

  public final native void setOnwrite(EventListener listener) /*-{
    this.onwrite = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwriteend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwriteend);
  }-*/;

  public final native void setOnwriteend(EventListener listener) /*-{
    this.onwriteend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwritestart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwritestart);
  }-*/;

  public final native void setOnwritestart(EventListener listener) /*-{
    this.onwritestart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native double getPosition() /*-{
    return this.position;
  }-*/;

  public final native int getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native void abort() /*-{
    this.abort();
  }-*/;

  public final native void seek(double position) /*-{
    this.seek(position);
  }-*/;

  public final native void truncate(double size) /*-{
    this.truncate(size);
  }-*/;

  public final native void write(Blob data) /*-{
    this.write(data);
  }-*/;
}
