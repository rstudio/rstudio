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
import elemental.html.FileSystemCallback;
import elemental.js.dom.JsDocument;
import elemental.html.BarProp;
import elemental.html.Screen;
import elemental.js.dom.JsNode;
import elemental.html.Point;
import elemental.dom.TimeoutHandler;
import elemental.html.Database;
import elemental.html.History;
import elemental.js.events.JsEventListener;
import elemental.dom.Document;
import elemental.dom.Node;
import elemental.html.NotificationCenter;
import elemental.html.Console;
import elemental.css.CSSRuleList;
import elemental.html.DatabaseCallback;
import elemental.dom.RequestAnimationFrameCallback;
import elemental.html.Location;
import elemental.html.StyleMedia;
import elemental.html.ErrorCallback;
import elemental.html.Selection;
import elemental.html.MediaQueryList;
import elemental.dom.Element;
import elemental.js.util.JsIndexable;
import elemental.html.Storage;
import elemental.html.EntryCallback;
import elemental.util.Indexable;
import elemental.html.StorageInfo;
import elemental.html.Navigator;
import elemental.html.PagePopupController;
import elemental.events.Event;
import elemental.js.dom.JsElement;
import elemental.html.ApplicationCache;
import elemental.js.css.JsCSSStyleDeclaration;
import elemental.html.Crypto;
import elemental.js.events.JsEvent;
import elemental.html.Performance;
import elemental.events.EventListener;
import elemental.js.css.JsCSSRuleList;
import elemental.html.IDBFactory;
import elemental.html.Window;
import elemental.css.CSSStyleDeclaration;

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
import elemental.xpath.*;
import elemental.xml.*;
import elemental.js.xpath.*;
import elemental.js.xml.*;

import java.util.Date;

public class JsWindow extends JsElementalMixinBase  implements Window {
  protected JsWindow() {}

  public final native void clearOpener() /*-{
    this.opener = null;
  }-*/;


  public final native JsApplicationCache getApplicationCache() /*-{
    return this.applicationCache;
  }-*/;

  public final native JsNavigator getClientInformation() /*-{
    return this.clientInformation;
  }-*/;

  public final native boolean isClosed() /*-{
    return this.closed;
  }-*/;

  public final native JsConsole getConsole() /*-{
    return this.console;
  }-*/;

  public final native JsCrypto getCrypto() /*-{
    return this.crypto;
  }-*/;

  public final native String getDefaultStatus() /*-{
    return this.defaultStatus;
  }-*/;

  public final native void setDefaultStatus(String param_defaultStatus) /*-{
    this.defaultStatus = param_defaultStatus;
  }-*/;

  public final native String getDefaultstatus() /*-{
    return this.defaultstatus;
  }-*/;

  public final native void setDefaultstatus(String param_defaultstatus) /*-{
    this.defaultstatus = param_defaultstatus;
  }-*/;

  public final native double getDevicePixelRatio() /*-{
    return this.devicePixelRatio;
  }-*/;

  public final native JsDocument getDocument() /*-{
    return this.document;
  }-*/;

  public final native JsEvent getEvent() /*-{
    return this.event;
  }-*/;

  public final native JsElement getFrameElement() /*-{
    return this.frameElement;
  }-*/;

  public final native JsWindow getFrames() /*-{
    return this.frames;
  }-*/;

  public final native JsHistory getHistory() /*-{
    return this.history;
  }-*/;

  public final native int getInnerHeight() /*-{
    return this.innerHeight;
  }-*/;

  public final native int getInnerWidth() /*-{
    return this.innerWidth;
  }-*/;

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native JsStorage getLocalStorage() /*-{
    return this.localStorage;
  }-*/;

  public final native JsLocation getLocation() /*-{
    return this.location;
  }-*/;

  public final native void setLocation(Location param_location) /*-{
    this.location = param_location;
  }-*/;

  public final native JsBarProp getLocationbar() /*-{
    return this.locationbar;
  }-*/;

  public final native JsBarProp getMenubar() /*-{
    return this.menubar;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native JsNavigator getNavigator() /*-{
    return this.navigator;
  }-*/;

  public final native boolean isOffscreenBuffering() /*-{
    return this.offscreenBuffering;
  }-*/;

  public final native EventListener getOnabort() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onabort);
  }-*/;

  public final native void setOnabort(EventListener listener) /*-{
    this.onabort = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnbeforeunload() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onbeforeunload);
  }-*/;

  public final native void setOnbeforeunload(EventListener listener) /*-{
    this.onbeforeunload = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnblur() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onblur);
  }-*/;

  public final native void setOnblur(EventListener listener) /*-{
    this.onblur = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOncanplay() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncanplay);
  }-*/;

  public final native void setOncanplay(EventListener listener) /*-{
    this.oncanplay = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOncanplaythrough() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncanplaythrough);
  }-*/;

  public final native void setOncanplaythrough(EventListener listener) /*-{
    this.oncanplaythrough = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onchange);
  }-*/;

  public final native void setOnchange(EventListener listener) /*-{
    this.onchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnclick() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onclick);
  }-*/;

  public final native void setOnclick(EventListener listener) /*-{
    this.onclick = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOncontextmenu() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncontextmenu);
  }-*/;

  public final native void setOncontextmenu(EventListener listener) /*-{
    this.oncontextmenu = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndblclick() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondblclick);
  }-*/;

  public final native void setOndblclick(EventListener listener) /*-{
    this.ondblclick = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndevicemotion() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondevicemotion);
  }-*/;

  public final native void setOndevicemotion(EventListener listener) /*-{
    this.ondevicemotion = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndeviceorientation() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondeviceorientation);
  }-*/;

  public final native void setOndeviceorientation(EventListener listener) /*-{
    this.ondeviceorientation = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndrag() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondrag);
  }-*/;

  public final native void setOndrag(EventListener listener) /*-{
    this.ondrag = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndragend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondragend);
  }-*/;

  public final native void setOndragend(EventListener listener) /*-{
    this.ondragend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndragenter() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondragenter);
  }-*/;

  public final native void setOndragenter(EventListener listener) /*-{
    this.ondragenter = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndragleave() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondragleave);
  }-*/;

  public final native void setOndragleave(EventListener listener) /*-{
    this.ondragleave = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndragover() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondragover);
  }-*/;

  public final native void setOndragover(EventListener listener) /*-{
    this.ondragover = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndragstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondragstart);
  }-*/;

  public final native void setOndragstart(EventListener listener) /*-{
    this.ondragstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndrop() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondrop);
  }-*/;

  public final native void setOndrop(EventListener listener) /*-{
    this.ondrop = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndurationchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondurationchange);
  }-*/;

  public final native void setOndurationchange(EventListener listener) /*-{
    this.ondurationchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnemptied() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onemptied);
  }-*/;

  public final native void setOnemptied(EventListener listener) /*-{
    this.onemptied = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnended() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onended);
  }-*/;

  public final native void setOnended(EventListener listener) /*-{
    this.onended = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnfocus() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onfocus);
  }-*/;

  public final native void setOnfocus(EventListener listener) /*-{
    this.onfocus = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnhashchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onhashchange);
  }-*/;

  public final native void setOnhashchange(EventListener listener) /*-{
    this.onhashchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOninput() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oninput);
  }-*/;

  public final native void setOninput(EventListener listener) /*-{
    this.oninput = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOninvalid() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oninvalid);
  }-*/;

  public final native void setOninvalid(EventListener listener) /*-{
    this.oninvalid = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnkeydown() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onkeydown);
  }-*/;

  public final native void setOnkeydown(EventListener listener) /*-{
    this.onkeydown = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnkeypress() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onkeypress);
  }-*/;

  public final native void setOnkeypress(EventListener listener) /*-{
    this.onkeypress = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnkeyup() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onkeyup);
  }-*/;

  public final native void setOnkeyup(EventListener listener) /*-{
    this.onkeyup = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnload() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onload);
  }-*/;

  public final native void setOnload(EventListener listener) /*-{
    this.onload = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnloadeddata() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onloadeddata);
  }-*/;

  public final native void setOnloadeddata(EventListener listener) /*-{
    this.onloadeddata = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnloadedmetadata() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onloadedmetadata);
  }-*/;

  public final native void setOnloadedmetadata(EventListener listener) /*-{
    this.onloadedmetadata = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnloadstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onloadstart);
  }-*/;

  public final native void setOnloadstart(EventListener listener) /*-{
    this.onloadstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmessage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmessage);
  }-*/;

  public final native void setOnmessage(EventListener listener) /*-{
    this.onmessage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmousedown() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmousedown);
  }-*/;

  public final native void setOnmousedown(EventListener listener) /*-{
    this.onmousedown = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmousemove() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmousemove);
  }-*/;

  public final native void setOnmousemove(EventListener listener) /*-{
    this.onmousemove = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmouseout() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmouseout);
  }-*/;

  public final native void setOnmouseout(EventListener listener) /*-{
    this.onmouseout = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmouseover() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmouseover);
  }-*/;

  public final native void setOnmouseover(EventListener listener) /*-{
    this.onmouseover = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmouseup() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmouseup);
  }-*/;

  public final native void setOnmouseup(EventListener listener) /*-{
    this.onmouseup = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnmousewheel() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onmousewheel);
  }-*/;

  public final native void setOnmousewheel(EventListener listener) /*-{
    this.onmousewheel = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native EventListener getOnpagehide() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onpagehide);
  }-*/;

  public final native void setOnpagehide(EventListener listener) /*-{
    this.onpagehide = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnpageshow() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onpageshow);
  }-*/;

  public final native void setOnpageshow(EventListener listener) /*-{
    this.onpageshow = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnpause() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onpause);
  }-*/;

  public final native void setOnpause(EventListener listener) /*-{
    this.onpause = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnplay() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onplay);
  }-*/;

  public final native void setOnplay(EventListener listener) /*-{
    this.onplay = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnplaying() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onplaying);
  }-*/;

  public final native void setOnplaying(EventListener listener) /*-{
    this.onplaying = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnpopstate() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onpopstate);
  }-*/;

  public final native void setOnpopstate(EventListener listener) /*-{
    this.onpopstate = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnprogress() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onprogress);
  }-*/;

  public final native void setOnprogress(EventListener listener) /*-{
    this.onprogress = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnratechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onratechange);
  }-*/;

  public final native void setOnratechange(EventListener listener) /*-{
    this.onratechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnreset() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onreset);
  }-*/;

  public final native void setOnreset(EventListener listener) /*-{
    this.onreset = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnresize() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onresize);
  }-*/;

  public final native void setOnresize(EventListener listener) /*-{
    this.onresize = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnscroll() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onscroll);
  }-*/;

  public final native void setOnscroll(EventListener listener) /*-{
    this.onscroll = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsearch() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsearch);
  }-*/;

  public final native void setOnsearch(EventListener listener) /*-{
    this.onsearch = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnseeked() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onseeked);
  }-*/;

  public final native void setOnseeked(EventListener listener) /*-{
    this.onseeked = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnseeking() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onseeking);
  }-*/;

  public final native void setOnseeking(EventListener listener) /*-{
    this.onseeking = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnselect() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onselect);
  }-*/;

  public final native void setOnselect(EventListener listener) /*-{
    this.onselect = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnstalled() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onstalled);
  }-*/;

  public final native void setOnstalled(EventListener listener) /*-{
    this.onstalled = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnstorage() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onstorage);
  }-*/;

  public final native void setOnstorage(EventListener listener) /*-{
    this.onstorage = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsubmit() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsubmit);
  }-*/;

  public final native void setOnsubmit(EventListener listener) /*-{
    this.onsubmit = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsuspend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsuspend);
  }-*/;

  public final native void setOnsuspend(EventListener listener) /*-{
    this.onsuspend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOntimeupdate() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ontimeupdate);
  }-*/;

  public final native void setOntimeupdate(EventListener listener) /*-{
    this.ontimeupdate = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOntouchcancel() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ontouchcancel);
  }-*/;

  public final native void setOntouchcancel(EventListener listener) /*-{
    this.ontouchcancel = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOntouchend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ontouchend);
  }-*/;

  public final native void setOntouchend(EventListener listener) /*-{
    this.ontouchend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOntouchmove() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ontouchmove);
  }-*/;

  public final native void setOntouchmove(EventListener listener) /*-{
    this.ontouchmove = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOntouchstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ontouchstart);
  }-*/;

  public final native void setOntouchstart(EventListener listener) /*-{
    this.ontouchstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnunload() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onunload);
  }-*/;

  public final native void setOnunload(EventListener listener) /*-{
    this.onunload = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnvolumechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onvolumechange);
  }-*/;

  public final native void setOnvolumechange(EventListener listener) /*-{
    this.onvolumechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwaiting() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwaiting);
  }-*/;

  public final native void setOnwaiting(EventListener listener) /*-{
    this.onwaiting = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitanimationend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitanimationend);
  }-*/;

  public final native void setOnwebkitanimationend(EventListener listener) /*-{
    this.onwebkitanimationend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitanimationiteration() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitanimationiteration);
  }-*/;

  public final native void setOnwebkitanimationiteration(EventListener listener) /*-{
    this.onwebkitanimationiteration = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitanimationstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitanimationstart);
  }-*/;

  public final native void setOnwebkitanimationstart(EventListener listener) /*-{
    this.onwebkitanimationstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkittransitionend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkittransitionend);
  }-*/;

  public final native void setOnwebkittransitionend(EventListener listener) /*-{
    this.onwebkittransitionend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native JsWindow getOpener() /*-{
    return this.opener;
  }-*/;

  public final native int getOuterHeight() /*-{
    return this.outerHeight;
  }-*/;

  public final native int getOuterWidth() /*-{
    return this.outerWidth;
  }-*/;

  public final native JsPagePopupController getPagePopupController() /*-{
    return this.pagePopupController;
  }-*/;

  public final native int getPageXOffset() /*-{
    return this.pageXOffset;
  }-*/;

  public final native int getPageYOffset() /*-{
    return this.pageYOffset;
  }-*/;

  public final native JsWindow getParent() /*-{
    return this.parent;
  }-*/;

  public final native JsPerformance getPerformance() /*-{
    return this.performance;
  }-*/;

  public final native JsBarProp getPersonalbar() /*-{
    return this.personalbar;
  }-*/;

  public final native JsScreen getScreen() /*-{
    return this.screen;
  }-*/;

  public final native int getScreenLeft() /*-{
    return this.screenLeft;
  }-*/;

  public final native int getScreenTop() /*-{
    return this.screenTop;
  }-*/;

  public final native int getScreenX() /*-{
    return this.screenX;
  }-*/;

  public final native int getScreenY() /*-{
    return this.screenY;
  }-*/;

  public final native int getScrollX() /*-{
    return this.scrollX;
  }-*/;

  public final native int getScrollY() /*-{
    return this.scrollY;
  }-*/;

  public final native JsBarProp getScrollbars() /*-{
    return this.scrollbars;
  }-*/;

  public final native JsWindow getSelf() /*-{
    return this.self;
  }-*/;

  public final native JsStorage getSessionStorage() /*-{
    return this.sessionStorage;
  }-*/;

  public final native String getStatus() /*-{
    return this.status;
  }-*/;

  public final native void setStatus(String param_status) /*-{
    this.status = param_status;
  }-*/;

  public final native JsBarProp getStatusbar() /*-{
    return this.statusbar;
  }-*/;

  public final native JsStyleMedia getStyleMedia() /*-{
    return this.styleMedia;
  }-*/;

  public final native JsBarProp getToolbar() /*-{
    return this.toolbar;
  }-*/;

  public final native JsWindow getTop() /*-{
    return this.top;
  }-*/;

  public final native JsIDBFactory getWebkitIndexedDB() /*-{
    return this.webkitIndexedDB;
  }-*/;

  public final native JsNotificationCenter getWebkitNotifications() /*-{
    return this.webkitNotifications;
  }-*/;

  public final native JsStorageInfo getWebkitStorageInfo() /*-{
    return this.webkitStorageInfo;
  }-*/;

  public final native JsWindow getWindow() /*-{
    return this.window;
  }-*/;

  public final native void alert(String message) /*-{
    this.alert(message);
  }-*/;

  public final native String atob(String string) /*-{
    return this.atob(string);
  }-*/;

  public final native void blur() /*-{
    this.blur();
  }-*/;

  public final native String btoa(String string) /*-{
    return this.btoa(string);
  }-*/;

  public final native void captureEvents() /*-{
    this.captureEvents();
  }-*/;

  public final native void clearInterval(int handle) /*-{
    this.clearInterval(handle);
  }-*/;

  public final native void clearTimeout(int handle) /*-{
    this.clearTimeout(handle);
  }-*/;

  public final native void close() /*-{
    this.close();
  }-*/;

  public final native boolean confirm(String message) /*-{
    return this.confirm(message);
  }-*/;

  public final native boolean find(String string, boolean caseSensitive, boolean backwards, boolean wrap, boolean wholeWord, boolean searchInFrames, boolean showDialog) /*-{
    return this.find(string, caseSensitive, backwards, wrap, wholeWord, searchInFrames, showDialog);
  }-*/;

  public final native void focus() /*-{
    this.focus();
  }-*/;

  public final native JsCSSStyleDeclaration getComputedStyle(Element element, String pseudoElement) /*-{
    return this.getComputedStyle(element, pseudoElement);
  }-*/;

  public final native JsCSSRuleList getMatchedCSSRules(Element element, String pseudoElement) /*-{
    return this.getMatchedCSSRules(element, pseudoElement);
  }-*/;

  public final native JsSelection getSelection() /*-{
    return this.getSelection();
  }-*/;

  public final native JsMediaQueryList matchMedia(String query) /*-{
    return this.matchMedia(query);
  }-*/;

  public final native void moveBy(float x, float y) /*-{
    this.moveBy(x, y);
  }-*/;

  public final native void moveTo(float x, float y) /*-{
    this.moveTo(x, y);
  }-*/;

  public final native JsWindow open(String url, String name) /*-{
    return this.open(url, name);
  }-*/;

  public final native JsWindow open(String url, String name, String options) /*-{
    return this.open(url, name, options);
  }-*/;

  public final native JsDatabase openDatabase(String name, String version, String displayName, int estimatedSize, DatabaseCallback creationCallback) /*-{
    return this.openDatabase(name, version, displayName, estimatedSize, $entry(creationCallback.@elemental.html.DatabaseCallback::onDatabaseCallback(Ljava/lang/Object;)).bind(creationCallback));
  }-*/;

  public final native JsDatabase openDatabase(String name, String version, String displayName, int estimatedSize) /*-{
    return this.openDatabase(name, version, displayName, estimatedSize);
  }-*/;

  public final native void postMessage(Object message, String targetOrigin) /*-{
    this.postMessage(message, targetOrigin);
  }-*/;

  public final native void postMessage(Object message, String targetOrigin, Indexable messagePorts) /*-{
    this.postMessage(message, targetOrigin, messagePorts);
  }-*/;

  public final native void print() /*-{
    this.print();
  }-*/;

  public final native String prompt(String message, String defaultValue) /*-{
    return this.prompt(message, defaultValue);
  }-*/;

  public final native void releaseEvents() /*-{
    this.releaseEvents();
  }-*/;

  public final native void resizeBy(float x, float y) /*-{
    this.resizeBy(x, y);
  }-*/;

  public final native void resizeTo(float width, float height) /*-{
    this.resizeTo(width, height);
  }-*/;

  public final native void scroll(int x, int y) /*-{
    this.scroll(x, y);
  }-*/;

  public final native void scrollBy(int x, int y) /*-{
    this.scrollBy(x, y);
  }-*/;

  public final native void scrollTo(int x, int y) /*-{
    this.scrollTo(x, y);
  }-*/;

  public final native int setInterval(TimeoutHandler handler, int timeout) /*-{
    return this.setInterval($entry(handler.@elemental.dom.TimeoutHandler::onTimeoutHandler()).bind(handler), timeout);
  }-*/;

  public final native int setTimeout(TimeoutHandler handler, int timeout) /*-{
    return this.setTimeout($entry(handler.@elemental.dom.TimeoutHandler::onTimeoutHandler()).bind(handler), timeout);
  }-*/;

  public final native Object showModalDialog(String url) /*-{
    return this.showModalDialog(url);
  }-*/;

  public final native Object showModalDialog(String url, Object dialogArgs) /*-{
    return this.showModalDialog(url, dialogArgs);
  }-*/;

  public final native Object showModalDialog(String url, Object dialogArgs, String featureArgs) /*-{
    return this.showModalDialog(url, dialogArgs, featureArgs);
  }-*/;

  public final native void stop() /*-{
    this.stop();
  }-*/;

  public final native void webkitCancelAnimationFrame(int id) /*-{
    this.webkitCancelAnimationFrame(id);
  }-*/;

  public final native void webkitCancelRequestAnimationFrame(int id) /*-{
    this.webkitCancelRequestAnimationFrame(id);
  }-*/;

  public final native JsPoint webkitConvertPointFromNodeToPage(Node node, Point p) /*-{
    return this.webkitConvertPointFromNodeToPage(node, p);
  }-*/;

  public final native JsPoint webkitConvertPointFromPageToNode(Node node, Point p) /*-{
    return this.webkitConvertPointFromPageToNode(node, p);
  }-*/;

  public final native void webkitPostMessage(Object message, String targetOrigin) /*-{
    this.webkitPostMessage(message, targetOrigin);
  }-*/;

  public final native void webkitPostMessage(Object message, String targetOrigin, Indexable transferList) /*-{
    this.webkitPostMessage(message, targetOrigin, transferList);
  }-*/;

  public final native int webkitRequestAnimationFrame(RequestAnimationFrameCallback callback) /*-{
    return this.webkitRequestAnimationFrame($entry(callback.@elemental.dom.RequestAnimationFrameCallback::onRequestAnimationFrameCallback(D)).bind(callback));
  }-*/;

  public final native void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback, ErrorCallback errorCallback) /*-{
    this.webkitRequestFileSystem(type, size, $entry(successCallback.@elemental.html.FileSystemCallback::onFileSystemCallback(Lelemental/html/DOMFileSystem;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native void webkitRequestFileSystem(int type, double size, FileSystemCallback successCallback) /*-{
    this.webkitRequestFileSystem(type, size, $entry(successCallback.@elemental.html.FileSystemCallback::onFileSystemCallback(Lelemental/html/DOMFileSystem;)).bind(successCallback));
  }-*/;

  public final native void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback) /*-{
    this.webkitResolveLocalFileSystemURL(url, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback));
  }-*/;

  public final native void webkitResolveLocalFileSystemURL(String url) /*-{
    this.webkitResolveLocalFileSystemURL(url);
  }-*/;

  public final native void webkitResolveLocalFileSystemURL(String url, EntryCallback successCallback, ErrorCallback errorCallback) /*-{
    this.webkitResolveLocalFileSystemURL(url, $entry(successCallback.@elemental.html.EntryCallback::onEntryCallback(Lelemental/html/Entry;)).bind(successCallback), $entry(errorCallback.@elemental.html.ErrorCallback::onErrorCallback(Lelemental/html/FileError;)).bind(errorCallback));
  }-*/;

  public final native JsAudioElement newAudioElement(String src) /*-{ return new AudioElement(src); }-*/;

  public final native JsCSSMatrix newCSSMatrix(String cssValue) /*-{ return new CSSMatrix(cssValue); }-*/;

  public final native JsDOMParser newDOMParser() /*-{ return new DOMParser(); }-*/;

  public final native JsDOMURL newDOMURL() /*-{ return new DOMURL(); }-*/;

  public final native JsDeprecatedPeerConnection newDeprecatedPeerConnection(String serverConfiguration, SignalingCallback signalingCallback) /*-{ return new DeprecatedPeerConnection(serverConfiguration, $entry(signalingCallback.@elemental.html.SignalingCallback::onSignalingCallback(Ljava/lang/String;Lelemental/html/DeprecatedPeerConnection;)).bind(signalingCallback)); }-*/;

  public final native JsEventSource newEventSource(String scriptUrl) /*-{ return new EventSource(scriptUrl); }-*/;

  public final native JsFileReader newFileReader() /*-{ return new FileReader(); }-*/;

  public final native JsFileReaderSync newFileReaderSync() /*-{ return new FileReaderSync(); }-*/;

  public final native JsFloat32Array newFloat32Array(int length) /*-{ return new Float32Array(length); }-*/;

  public final native JsFloat32Array newFloat32Array(IndexableNumber list) /*-{ return new Float32Array(list); }-*/;

  public final native JsFloat32Array newFloat32Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Float32Array(buffer, byteOffset, length); }-*/;

  public final native JsFloat64Array newFloat64Array(int length) /*-{ return new Float64Array(length); }-*/;

  public final native JsFloat64Array newFloat64Array(IndexableNumber list) /*-{ return new Float64Array(list); }-*/;

  public final native JsFloat64Array newFloat64Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Float64Array(buffer, byteOffset, length); }-*/;

  public final native JsIceCandidate newIceCandidate(String label, String candidateLine) /*-{ return new IceCandidate(label, candidateLine); }-*/;

  public final native JsInt16Array newInt16Array(int length) /*-{ return new Int16Array(length); }-*/;

  public final native JsInt16Array newInt16Array(IndexableNumber list) /*-{ return new Int16Array(list); }-*/;

  public final native JsInt16Array newInt16Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Int16Array(buffer, byteOffset, length); }-*/;

  public final native JsInt32Array newInt32Array(int length) /*-{ return new Int32Array(length); }-*/;

  public final native JsInt32Array newInt32Array(IndexableNumber list) /*-{ return new Int32Array(list); }-*/;

  public final native JsInt32Array newInt32Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Int32Array(buffer, byteOffset, length); }-*/;

  public final native JsInt8Array newInt8Array(int length) /*-{ return new Int8Array(length); }-*/;

  public final native JsInt8Array newInt8Array(IndexableNumber list) /*-{ return new Int8Array(list); }-*/;

  public final native JsInt8Array newInt8Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Int8Array(buffer, byteOffset, length); }-*/;

  public final native JsMediaController newMediaController() /*-{ return new MediaController(); }-*/;

  public final native JsMediaStream newMediaStream(MediaStreamTrackList audioTracks, MediaStreamTrackList videoTracks) /*-{ return new MediaStream(audioTracks, videoTracks); }-*/;

  public final native JsMessageChannel newMessageChannel() /*-{ return new MessageChannel(); }-*/;

  public final native JsNotification newNotification(String title, Mappable options) /*-{ return new Notification(title, options); }-*/;

  public final native JsOptionElement newOptionElement(String data, String value, boolean defaultSelected, boolean selected) /*-{ return new OptionElement(data, value, defaultSelected, selected); }-*/;

  public final native JsPeerConnection00 newPeerConnection00(String serverConfiguration, IceCallback iceCallback) /*-{ return new PeerConnection00(serverConfiguration, $entry(iceCallback.@elemental.html.IceCallback::onIceCallback(Lelemental/html/IceCandidate;ZLelemental/html/PeerConnection00;)).bind(iceCallback)); }-*/;

  public final native JsSessionDescription newSessionDescription(String sdp) /*-{ return new SessionDescription(sdp); }-*/;

  public final native JsShadowRoot newShadowRoot(Element host) /*-{ return new ShadowRoot(host); }-*/;

  public final native JsSharedWorker newSharedWorker(String scriptURL, String name) /*-{ return new SharedWorker(scriptURL, name); }-*/;

  public final native JsSpeechGrammar newSpeechGrammar() /*-{ return new SpeechGrammar(); }-*/;

  public final native JsSpeechGrammarList newSpeechGrammarList() /*-{ return new SpeechGrammarList(); }-*/;

  public final native JsSpeechRecognition newSpeechRecognition() /*-{ return new SpeechRecognition(); }-*/;

  public final native JsTextTrackCue newTextTrackCue(String id, double startTime, double endTime, String text, String settings, boolean pauseOnExit) /*-{ return new TextTrackCue(id, startTime, endTime, text, settings, pauseOnExit); }-*/;

  public final native JsUint16Array newUint16Array(int length) /*-{ return new Uint16Array(length); }-*/;

  public final native JsUint16Array newUint16Array(IndexableNumber list) /*-{ return new Uint16Array(list); }-*/;

  public final native JsUint16Array newUint16Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Uint16Array(buffer, byteOffset, length); }-*/;

  public final native JsUint32Array newUint32Array(int length) /*-{ return new Uint32Array(length); }-*/;

  public final native JsUint32Array newUint32Array(IndexableNumber list) /*-{ return new Uint32Array(list); }-*/;

  public final native JsUint32Array newUint32Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Uint32Array(buffer, byteOffset, length); }-*/;

  public final native JsUint8Array newUint8Array(int length) /*-{ return new Uint8Array(length); }-*/;

  public final native JsUint8Array newUint8Array(IndexableNumber list) /*-{ return new Uint8Array(list); }-*/;

  public final native JsUint8Array newUint8Array(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Uint8Array(buffer, byteOffset, length); }-*/;

  public final native JsUint8ClampedArray newUint8ClampedArray(int length) /*-{ return new Uint8ClampedArray(length); }-*/;

  public final native JsUint8ClampedArray newUint8ClampedArray(IndexableNumber list) /*-{ return new Uint8ClampedArray(list); }-*/;

  public final native JsUint8ClampedArray newUint8ClampedArray(ArrayBuffer buffer, int byteOffset, int length) /*-{ return new Uint8ClampedArray(buffer, byteOffset, length); }-*/;

  public final native JsWorker newWorker(String scriptUrl) /*-{ return new Worker(scriptUrl); }-*/;

  public final native JsXMLHttpRequest newXMLHttpRequest() /*-{ return new XMLHttpRequest(); }-*/;

  public final native JsXMLSerializer newXMLSerializer() /*-{ return new XMLSerializer(); }-*/;

  public final native JsXPathEvaluator newXPathEvaluator() /*-{ return new XPathEvaluator(); }-*/;

  public final native JsXSLTProcessor newXSLTProcessor() /*-{ return new XSLTProcessor(); }-*/;
}
