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
import elemental.dom.Node;
import elemental.dom.Element;
import elemental.js.css.JsCSSStyleDeclaration;
import elemental.html.HTMLCollection;
import elemental.dom.NodeList;
import elemental.html.ClientRect;
import elemental.js.html.JsHTMLCollection;
import elemental.js.util.JsMappable;
import elemental.js.html.JsClientRect;
import elemental.util.Mappable;
import elemental.events.EventListener;
import elemental.js.html.JsClientRectList;
import elemental.dom.Attr;
import elemental.dom.DOMTokenList;
import elemental.css.CSSStyleDeclaration;
import elemental.js.events.JsEventListener;
import elemental.html.ClientRectList;

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

public class JsElement extends JsNode  implements Element {
  protected JsElement() {}

  public final native String getAccessKey() /*-{
    return this.accessKey;
  }-*/;

  public final native void setAccessKey(String param_accessKey) /*-{
    this.accessKey = param_accessKey;
  }-*/;

  public final native JsHTMLCollection getChildren() /*-{
    return this.children;
  }-*/;

  public final native JsDOMTokenList getClassList() /*-{
    return this.classList;
  }-*/;

  public final native String getClassName() /*-{
    return this.className;
  }-*/;

  public final native void setClassName(String param_className) /*-{
    this.className = param_className;
  }-*/;

  public final native int getClientHeight() /*-{
    return this.clientHeight;
  }-*/;

  public final native int getClientLeft() /*-{
    return this.clientLeft;
  }-*/;

  public final native int getClientTop() /*-{
    return this.clientTop;
  }-*/;

  public final native int getClientWidth() /*-{
    return this.clientWidth;
  }-*/;

  public final native String getContentEditable() /*-{
    return this.contentEditable;
  }-*/;

  public final native void setContentEditable(String param_contentEditable) /*-{
    this.contentEditable = param_contentEditable;
  }-*/;

  public final native JsMappable getDataset() /*-{
    return this.dataset;
  }-*/;

  public final native String getDir() /*-{
    return this.dir;
  }-*/;

  public final native void setDir(String param_dir) /*-{
    this.dir = param_dir;
  }-*/;

  public final native boolean isDraggable() /*-{
    return this.draggable;
  }-*/;

  public final native void setDraggable(boolean param_draggable) /*-{
    this.draggable = param_draggable;
  }-*/;

  public final native boolean isHidden() /*-{
    return this.hidden;
  }-*/;

  public final native void setHidden(boolean param_hidden) /*-{
    this.hidden = param_hidden;
  }-*/;

  public final native String getId() /*-{
    return this.id;
  }-*/;

  public final native void setId(String param_id) /*-{
    this.id = param_id;
  }-*/;

  public final native String getInnerHTML() /*-{
    return this.innerHTML;
  }-*/;

  public final native void setInnerHTML(String param_innerHTML) /*-{
    this.innerHTML = param_innerHTML;
  }-*/;

  public final native String getInnerText() /*-{
    return this.innerText;
  }-*/;

  public final native void setInnerText(String param_innerText) /*-{
    this.innerText = param_innerText;
  }-*/;

  public final native boolean isContentEditable() /*-{
    return this.isContentEditable;
  }-*/;

  public final native String getLang() /*-{
    return this.lang;
  }-*/;

  public final native void setLang(String param_lang) /*-{
    this.lang = param_lang;
  }-*/;

  public final native int getOffsetHeight() /*-{
    return this.offsetHeight;
  }-*/;

  public final native int getOffsetLeft() /*-{
    return this.offsetLeft;
  }-*/;

  public final native JsElement getOffsetParent() /*-{
    return this.offsetParent;
  }-*/;

  public final native int getOffsetTop() /*-{
    return this.offsetTop;
  }-*/;

  public final native int getOffsetWidth() /*-{
    return this.offsetWidth;
  }-*/;

  public final native EventListener getOnabort() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onabort);
  }-*/;

  public final native void setOnabort(EventListener listener) /*-{
    this.onabort = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnbeforecopy() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onbeforecopy);
  }-*/;

  public final native void setOnbeforecopy(EventListener listener) /*-{
    this.onbeforecopy = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnbeforecut() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onbeforecut);
  }-*/;

  public final native void setOnbeforecut(EventListener listener) /*-{
    this.onbeforecut = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnbeforepaste() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onbeforepaste);
  }-*/;

  public final native void setOnbeforepaste(EventListener listener) /*-{
    this.onbeforepaste = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnblur() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onblur);
  }-*/;

  public final native void setOnblur(EventListener listener) /*-{
    this.onblur = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native EventListener getOncopy() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncopy);
  }-*/;

  public final native void setOncopy(EventListener listener) /*-{
    this.oncopy = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOncut() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncut);
  }-*/;

  public final native void setOncut(EventListener listener) /*-{
    this.oncut = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOndblclick() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.ondblclick);
  }-*/;

  public final native void setOndblclick(EventListener listener) /*-{
    this.ondblclick = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native EventListener getOnpaste() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onpaste);
  }-*/;

  public final native void setOnpaste(EventListener listener) /*-{
    this.onpaste = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnreset() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onreset);
  }-*/;

  public final native void setOnreset(EventListener listener) /*-{
    this.onreset = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native EventListener getOnselect() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onselect);
  }-*/;

  public final native void setOnselect(EventListener listener) /*-{
    this.onselect = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnselectstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onselectstart);
  }-*/;

  public final native void setOnselectstart(EventListener listener) /*-{
    this.onselectstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsubmit() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsubmit);
  }-*/;

  public final native void setOnsubmit(EventListener listener) /*-{
    this.onsubmit = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native EventListener getOnwebkitfullscreenchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitfullscreenchange);
  }-*/;

  public final native void setOnwebkitfullscreenchange(EventListener listener) /*-{
    this.onwebkitfullscreenchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnwebkitfullscreenerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onwebkitfullscreenerror);
  }-*/;

  public final native void setOnwebkitfullscreenerror(EventListener listener) /*-{
    this.onwebkitfullscreenerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native String getOuterHTML() /*-{
    return this.outerHTML;
  }-*/;

  public final native void setOuterHTML(String param_outerHTML) /*-{
    this.outerHTML = param_outerHTML;
  }-*/;

  public final native String getOuterText() /*-{
    return this.outerText;
  }-*/;

  public final native void setOuterText(String param_outerText) /*-{
    this.outerText = param_outerText;
  }-*/;

  public final native int getScrollHeight() /*-{
    return this.scrollHeight;
  }-*/;

  public final native int getScrollLeft() /*-{
    return this.scrollLeft;
  }-*/;

  public final native void setScrollLeft(int param_scrollLeft) /*-{
    this.scrollLeft = param_scrollLeft;
  }-*/;

  public final native int getScrollTop() /*-{
    return this.scrollTop;
  }-*/;

  public final native void setScrollTop(int param_scrollTop) /*-{
    this.scrollTop = param_scrollTop;
  }-*/;

  public final native int getScrollWidth() /*-{
    return this.scrollWidth;
  }-*/;

  public final native boolean isSpellcheck() /*-{
    return this.spellcheck;
  }-*/;

  public final native void setSpellcheck(boolean param_spellcheck) /*-{
    this.spellcheck = param_spellcheck;
  }-*/;

  public final native JsCSSStyleDeclaration getStyle() /*-{
    return this.style;
  }-*/;

  public final native int getTabIndex() /*-{
    return this.tabIndex;
  }-*/;

  public final native void setTabIndex(int param_tabIndex) /*-{
    this.tabIndex = param_tabIndex;
  }-*/;

  public final native String getTagName() /*-{
    return this.tagName;
  }-*/;

  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  public final native void setTitle(String param_title) /*-{
    this.title = param_title;
  }-*/;

  public final native boolean isTranslate() /*-{
    return this.translate;
  }-*/;

  public final native void setTranslate(boolean param_translate) /*-{
    this.translate = param_translate;
  }-*/;

  public final native String getWebkitRegionOverflow() /*-{
    return this.webkitRegionOverflow;
  }-*/;

  public final native String getWebkitdropzone() /*-{
    return this.webkitdropzone;
  }-*/;

  public final native void setWebkitdropzone(String param_webkitdropzone) /*-{
    this.webkitdropzone = param_webkitdropzone;
  }-*/;

  public final native void blur() /*-{
    this.blur();
  }-*/;

  public final native void focus() /*-{
    this.focus();
  }-*/;

  public final native String getAttribute(String name) /*-{
    return this.getAttribute(name);
  }-*/;

  public final native String getAttributeNS(String namespaceURI, String localName) /*-{
    return this.getAttributeNS(namespaceURI, localName);
  }-*/;

  public final native JsAttr getAttributeNode(String name) /*-{
    return this.getAttributeNode(name);
  }-*/;

  public final native JsAttr getAttributeNodeNS(String namespaceURI, String localName) /*-{
    return this.getAttributeNodeNS(namespaceURI, localName);
  }-*/;

  public final native JsClientRect getBoundingClientRect() /*-{
    return this.getBoundingClientRect();
  }-*/;

  public final native JsClientRectList getClientRects() /*-{
    return this.getClientRects();
  }-*/;

  public final native JsNodeList getElementsByClassName(String name) /*-{
    return this.getElementsByClassName(name);
  }-*/;

  public final native JsNodeList getElementsByTagName(String name) /*-{
    return this.getElementsByTagName(name);
  }-*/;

  public final native JsNodeList getElementsByTagNameNS(String namespaceURI, String localName) /*-{
    return this.getElementsByTagNameNS(namespaceURI, localName);
  }-*/;

  public final native boolean hasAttribute(String name) /*-{
    return this.hasAttribute(name);
  }-*/;

  public final native boolean hasAttributeNS(String namespaceURI, String localName) /*-{
    return this.hasAttributeNS(namespaceURI, localName);
  }-*/;

  public final native void removeAttribute(String name) /*-{
    this.removeAttribute(name);
  }-*/;

  public final native void removeAttributeNS(String namespaceURI, String localName) /*-{
    this.removeAttributeNS(namespaceURI, localName);
  }-*/;

  public final native JsAttr removeAttributeNode(Attr oldAttr) /*-{
    return this.removeAttributeNode(oldAttr);
  }-*/;

  public final native void scrollByLines(int lines) /*-{
    this.scrollByLines(lines);
  }-*/;

  public final native void scrollByPages(int pages) /*-{
    this.scrollByPages(pages);
  }-*/;

  public final native void scrollIntoView() /*-{
    this.scrollIntoView();
  }-*/;

  public final native void scrollIntoView(boolean alignWithTop) /*-{
    this.scrollIntoView(alignWithTop);
  }-*/;

  public final native void scrollIntoViewIfNeeded() /*-{
    this.scrollIntoViewIfNeeded();
  }-*/;

  public final native void scrollIntoViewIfNeeded(boolean centerIfNeeded) /*-{
    this.scrollIntoViewIfNeeded(centerIfNeeded);
  }-*/;

  public final native void setAttribute(String name, String value) /*-{
    this.setAttribute(name, value);
  }-*/;

  public final native void setAttributeNS(String namespaceURI, String qualifiedName, String value) /*-{
    this.setAttributeNS(namespaceURI, qualifiedName, value);
  }-*/;

  public final native JsAttr setAttributeNode(Attr newAttr) /*-{
    return this.setAttributeNode(newAttr);
  }-*/;

  public final native JsAttr setAttributeNodeNS(Attr newAttr) /*-{
    return this.setAttributeNodeNS(newAttr);
  }-*/;

  public final native boolean webkitMatchesSelector(String selectors) /*-{
    return this.webkitMatchesSelector(selectors);
  }-*/;

  public final native void webkitRequestFullScreen(int flags) /*-{
    this.webkitRequestFullScreen(flags);
  }-*/;

  public final native void webkitRequestFullscreen() /*-{
    this.webkitRequestFullscreen();
  }-*/;

  public final native void click() /*-{
    this.click();
  }-*/;

  public final native JsElement insertAdjacentElement(String where, Element element) /*-{
    return this.insertAdjacentElement(where, element);
  }-*/;

  public final native void insertAdjacentHTML(String where, String html) /*-{
    this.insertAdjacentHTML(where, html);
  }-*/;

  public final native void insertAdjacentText(String where, String text) /*-{
    this.insertAdjacentText(where, text);
  }-*/;
}
