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
import elemental.js.css.JsCSSStyleDeclaration;
import elemental.js.ranges.JsRange;
import elemental.js.traversal.JsTreeWalker;
import elemental.js.html.JsHTMLCollection;
import elemental.js.traversal.JsNodeIterator;
import elemental.js.html.JsSelection;
import elemental.css.CSSStyleDeclaration;
import elemental.stylesheets.StyleSheetList;
import elemental.js.stylesheets.JsStyleSheetList;
import elemental.dom.Text;
import elemental.xpath.XPathNSResolver;
import elemental.xpath.XPathExpression;
import elemental.traversal.NodeIterator;
import elemental.traversal.TreeWalker;
import elemental.events.TouchList;
import elemental.js.events.JsEventListener;
import elemental.dom.Document;
import elemental.dom.Node;
import elemental.html.HTMLAllCollection;
import elemental.js.events.JsTouchList;
import elemental.dom.ProcessingInstruction;
import elemental.js.html.JsCanvasRenderingContext;
import elemental.dom.DocumentFragment;
import elemental.ranges.Range;
import elemental.html.Location;
import elemental.xpath.XPathResult;
import elemental.dom.DOMImplementation;
import elemental.dom.Comment;
import elemental.html.Selection;
import elemental.js.xpath.JsXPathExpression;
import elemental.html.HeadElement;
import elemental.dom.Element;
import elemental.js.html.JsWindow;
import elemental.html.HTMLCollection;
import elemental.dom.EntityReference;
import elemental.events.Touch;
import elemental.js.html.JsHeadElement;
import elemental.js.traversal.JsNodeFilter;
import elemental.js.xpath.JsXPathResult;
import elemental.js.events.JsTouch;
import elemental.events.Event;
import elemental.html.CanvasRenderingContext;
import elemental.events.EventTarget;
import elemental.traversal.NodeFilter;
import elemental.dom.NodeList;
import elemental.js.html.JsHTMLAllCollection;
import elemental.js.events.JsEvent;
import elemental.js.xpath.JsXPathNSResolver;
import elemental.js.html.JsLocation;
import elemental.events.EventListener;
import elemental.dom.Attr;
import elemental.html.Window;
import elemental.dom.DocumentType;
import elemental.dom.CDATASection;

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
import elemental.js.svg.*;
import elemental.svg.*;

import java.util.Date;

public class JsDocument extends JsNode  implements Document {
  protected JsDocument() {}

  public final native void clearOpener() /*-{
    this.opener = null;
  }-*/;

  public final JsElement createSvgElement(String tag) {
    return createElementNS("http://www.w3.org/2000/svg", tag).cast();
  }


  public final native String getURL() /*-{
    return this.URL;
  }-*/;

  public final native JsElement getActiveElement() /*-{
    return this.activeElement;
  }-*/;

  public final native String getAlinkColor() /*-{
    return this.alinkColor;
  }-*/;

  public final native void setAlinkColor(String param_alinkColor) /*-{
    this.alinkColor = param_alinkColor;
  }-*/;

  public final native JsHTMLAllCollection getAll() /*-{
    return this.all;
  }-*/;

  public final native void setAll(HTMLAllCollection param_all) /*-{
    this.all = param_all;
  }-*/;

  public final native JsHTMLCollection getAnchors() /*-{
    return this.anchors;
  }-*/;

  public final native JsHTMLCollection getApplets() /*-{
    return this.applets;
  }-*/;

  public final native String getBgColor() /*-{
    return this.bgColor;
  }-*/;

  public final native void setBgColor(String param_bgColor) /*-{
    this.bgColor = param_bgColor;
  }-*/;

  public final native JsElement getBody() /*-{
    return this.body;
  }-*/;

  public final native void setBody(Element param_body) /*-{
    this.body = param_body;
  }-*/;

  public final native String getCharacterSet() /*-{
    return this.characterSet;
  }-*/;

  public final native String getCharset() /*-{
    return this.charset;
  }-*/;

  public final native void setCharset(String param_charset) /*-{
    this.charset = param_charset;
  }-*/;

  public final native String getCompatMode() /*-{
    return this.compatMode;
  }-*/;

  public final native String getCookie() /*-{
    return this.cookie;
  }-*/;

  public final native void setCookie(String param_cookie) /*-{
    this.cookie = param_cookie;
  }-*/;

  public final native String getDefaultCharset() /*-{
    return this.defaultCharset;
  }-*/;

  public final native JsWindow getDefaultView() /*-{
    return this.defaultView;
  }-*/;

  public final native String getDesignMode() /*-{
    return this.designMode;
  }-*/;

  public final native void setDesignMode(String param_designMode) /*-{
    this.designMode = param_designMode;
  }-*/;

  public final native String getDir() /*-{
    return this.dir;
  }-*/;

  public final native void setDir(String param_dir) /*-{
    this.dir = param_dir;
  }-*/;

  public final native JsDocumentType getDoctype() /*-{
    return this.doctype;
  }-*/;

  public final native JsElement getDocumentElement() /*-{
    return this.documentElement;
  }-*/;

  public final native String getDocumentURI() /*-{
    return this.documentURI;
  }-*/;

  public final native void setDocumentURI(String param_documentURI) /*-{
    this.documentURI = param_documentURI;
  }-*/;

  public final native String getDomain() /*-{
    return this.domain;
  }-*/;

  public final native void setDomain(String param_domain) /*-{
    this.domain = param_domain;
  }-*/;

  public final native JsHTMLCollection getEmbeds() /*-{
    return this.embeds;
  }-*/;

  public final native String getFgColor() /*-{
    return this.fgColor;
  }-*/;

  public final native void setFgColor(String param_fgColor) /*-{
    this.fgColor = param_fgColor;
  }-*/;

  public final native JsHTMLCollection getForms() /*-{
    return this.forms;
  }-*/;

  public final native JsHeadElement getHead() /*-{
    return this.head;
  }-*/;

  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  public final native JsHTMLCollection getImages() /*-{
    return this.images;
  }-*/;

  public final native JsDOMImplementation getImplementation() /*-{
    return this.implementation;
  }-*/;

  public final native String getInputEncoding() /*-{
    return this.inputEncoding;
  }-*/;

  public final native String getLastModified() /*-{
    return this.lastModified;
  }-*/;

  public final native String getLinkColor() /*-{
    return this.linkColor;
  }-*/;

  public final native void setLinkColor(String param_linkColor) /*-{
    this.linkColor = param_linkColor;
  }-*/;

  public final native JsHTMLCollection getLinks() /*-{
    return this.links;
  }-*/;

  public final native JsLocation getLocation() /*-{
    return this.location;
  }-*/;

  public final native void setLocation(Location param_location) /*-{
    this.location = param_location;
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
  public final native EventListener getOnreadystatechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onreadystatechange);
  }-*/;

  public final native void setOnreadystatechange(EventListener listener) /*-{
    this.onreadystatechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native EventListener getOnselectionchange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onselectionchange);
  }-*/;

  public final native void setOnselectionchange(EventListener listener) /*-{
    this.onselectionchange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
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
  public final native JsHTMLCollection getPlugins() /*-{
    return this.plugins;
  }-*/;

  public final native String getPreferredStylesheetSet() /*-{
    return this.preferredStylesheetSet;
  }-*/;

  public final native String getReadyState() /*-{
    return this.readyState;
  }-*/;

  public final native String getReferrer() /*-{
    return this.referrer;
  }-*/;

  public final native JsHTMLCollection getScripts() /*-{
    return this.scripts;
  }-*/;

  public final native String getSelectedStylesheetSet() /*-{
    return this.selectedStylesheetSet;
  }-*/;

  public final native void setSelectedStylesheetSet(String param_selectedStylesheetSet) /*-{
    this.selectedStylesheetSet = param_selectedStylesheetSet;
  }-*/;

  public final native JsStyleSheetList getStyleSheets() /*-{
    return this.styleSheets;
  }-*/;

  public final native String getTitle() /*-{
    return this.title;
  }-*/;

  public final native void setTitle(String param_title) /*-{
    this.title = param_title;
  }-*/;

  public final native String getVlinkColor() /*-{
    return this.vlinkColor;
  }-*/;

  public final native void setVlinkColor(String param_vlinkColor) /*-{
    this.vlinkColor = param_vlinkColor;
  }-*/;

  public final native JsElement getWebkitCurrentFullScreenElement() /*-{
    return this.webkitCurrentFullScreenElement;
  }-*/;

  public final native boolean isWebkitFullScreenKeyboardInputAllowed() /*-{
    return this.webkitFullScreenKeyboardInputAllowed;
  }-*/;

  public final native JsElement getWebkitFullscreenElement() /*-{
    return this.webkitFullscreenElement;
  }-*/;

  public final native boolean isWebkitFullscreenEnabled() /*-{
    return this.webkitFullscreenEnabled;
  }-*/;

  public final native boolean isWebkitHidden() /*-{
    return this.webkitHidden;
  }-*/;

  public final native boolean isWebkitIsFullScreen() /*-{
    return this.webkitIsFullScreen;
  }-*/;

  public final native String getWebkitVisibilityState() /*-{
    return this.webkitVisibilityState;
  }-*/;

  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  public final native String getXmlEncoding() /*-{
    return this.xmlEncoding;
  }-*/;

  public final native boolean isXmlStandalone() /*-{
    return this.xmlStandalone;
  }-*/;

  public final native void setXmlStandalone(boolean param_xmlStandalone) /*-{
    this.xmlStandalone = param_xmlStandalone;
  }-*/;

  public final native String getXmlVersion() /*-{
    return this.xmlVersion;
  }-*/;

  public final native void setXmlVersion(String param_xmlVersion) /*-{
    this.xmlVersion = param_xmlVersion;
  }-*/;

  public final native JsNode adoptNode(Node source) /*-{
    return this.adoptNode(source);
  }-*/;

  public final native JsRange caretRangeFromPoint(int x, int y) /*-{
    return this.caretRangeFromPoint(x, y);
  }-*/;

  public final native JsAttr createAttribute(String name) /*-{
    return this.createAttribute(name);
  }-*/;

  public final native JsAttr createAttributeNS(String namespaceURI, String qualifiedName) /*-{
    return this.createAttributeNS(namespaceURI, qualifiedName);
  }-*/;

  public final native JsCDATASection createCDATASection(String data) /*-{
    return this.createCDATASection(data);
  }-*/;

  public final native JsComment createComment(String data) /*-{
    return this.createComment(data);
  }-*/;

  public final native JsDocumentFragment createDocumentFragment() /*-{
    return this.createDocumentFragment();
  }-*/;

  public final native JsElement createElement(String tagName) /*-{
    return this.createElement(tagName);
  }-*/;

  public final native JsElement createElementNS(String namespaceURI, String qualifiedName) /*-{
    return this.createElementNS(namespaceURI, qualifiedName);
  }-*/;

  public final native JsEntityReference createEntityReference(String name) /*-{
    return this.createEntityReference(name);
  }-*/;

  public final native JsEvent createEvent(String eventType) /*-{
    return this.createEvent(eventType);
  }-*/;

  public final native JsXPathExpression createExpression(String expression, XPathNSResolver resolver) /*-{
    return this.createExpression(expression, resolver);
  }-*/;

  public final native JsXPathNSResolver createNSResolver(Node nodeResolver) /*-{
    return this.createNSResolver(nodeResolver);
  }-*/;

  public final native JsNodeIterator createNodeIterator(Node root, int whatToShow, NodeFilter filter, boolean expandEntityReferences) /*-{
    return this.createNodeIterator(root, whatToShow, filter, expandEntityReferences);
  }-*/;

  public final native JsProcessingInstruction createProcessingInstruction(String target, String data) /*-{
    return this.createProcessingInstruction(target, data);
  }-*/;

  public final native JsRange createRange() /*-{
    return this.createRange();
  }-*/;

  public final native JsText createTextNode(String data) /*-{
    return this.createTextNode(data);
  }-*/;

  public final native JsTouch createTouch(Window window, EventTarget target, int identifier, int pageX, int pageY, int screenX, int screenY, int webkitRadiusX, int webkitRadiusY, float webkitRotationAngle, float webkitForce) /*-{
    return this.createTouch(window, target, identifier, pageX, pageY, screenX, screenY, webkitRadiusX, webkitRadiusY, webkitRotationAngle, webkitForce);
  }-*/;

  public final native JsTouchList createTouchList() /*-{
    return this.createTouchList();
  }-*/;

  public final native JsTreeWalker createTreeWalker(Node root, int whatToShow, NodeFilter filter, boolean expandEntityReferences) /*-{
    return this.createTreeWalker(root, whatToShow, filter, expandEntityReferences);
  }-*/;

  public final native JsElement elementFromPoint(int x, int y) /*-{
    return this.elementFromPoint(x, y);
  }-*/;

  public final native JsXPathResult evaluate(String expression, Node contextNode, XPathNSResolver resolver, int type, XPathResult inResult) /*-{
    return this.evaluate(expression, contextNode, resolver, type, inResult);
  }-*/;

  public final native boolean execCommand(String command, boolean userInterface, String value) /*-{
    return this.execCommand(command, userInterface, value);
  }-*/;

  public final native JsCanvasRenderingContext getCSSCanvasContext(String contextId, String name, int width, int height) /*-{
    return this.getCSSCanvasContext(contextId, name, width, height);
  }-*/;

  public final native JsElement getElementById(String elementId) /*-{
    return this.getElementById(elementId);
  }-*/;

  public final native JsNodeList getElementsByClassName(String tagname) /*-{
    return this.getElementsByClassName(tagname);
  }-*/;

  public final native JsNodeList getElementsByName(String elementName) /*-{
    return this.getElementsByName(elementName);
  }-*/;

  public final native JsNodeList getElementsByTagName(String tagname) /*-{
    return this.getElementsByTagName(tagname);
  }-*/;

  public final native JsNodeList getElementsByTagNameNS(String namespaceURI, String localName) /*-{
    return this.getElementsByTagNameNS(namespaceURI, localName);
  }-*/;

  public final native JsCSSStyleDeclaration getOverrideStyle(Element element, String pseudoElement) /*-{
    return this.getOverrideStyle(element, pseudoElement);
  }-*/;

  public final native JsSelection getSelection() /*-{
    return this.getSelection();
  }-*/;

  public final native JsNode importNode(Node importedNode) /*-{
    return this.importNode(importedNode);
  }-*/;

  public final native JsNode importNode(Node importedNode, boolean deep) /*-{
    return this.importNode(importedNode, deep);
  }-*/;

  public final native boolean queryCommandEnabled(String command) /*-{
    return this.queryCommandEnabled(command);
  }-*/;

  public final native boolean queryCommandIndeterm(String command) /*-{
    return this.queryCommandIndeterm(command);
  }-*/;

  public final native boolean queryCommandState(String command) /*-{
    return this.queryCommandState(command);
  }-*/;

  public final native boolean queryCommandSupported(String command) /*-{
    return this.queryCommandSupported(command);
  }-*/;

  public final native String queryCommandValue(String command) /*-{
    return this.queryCommandValue(command);
  }-*/;

  public final native void webkitCancelFullScreen() /*-{
    this.webkitCancelFullScreen();
  }-*/;

  public final native void webkitExitFullscreen() /*-{
    this.webkitExitFullscreen();
  }-*/;

  public final native void captureEvents() /*-{
    this.captureEvents();
  }-*/;

  public final native void clear() /*-{
    this.clear();
  }-*/;

  public final native void close() /*-{
    this.close();
  }-*/;

  public final native boolean hasFocus() /*-{
    return this.hasFocus();
  }-*/;

  public final native void open() /*-{
    this.open();
  }-*/;

  public final native void releaseEvents() /*-{
    this.releaseEvents();
  }-*/;

  public final native void write(String text) /*-{
    this.write(text);
  }-*/;

  public final native void writeln(String text) /*-{
    this.writeln(text);
  }-*/;

  public final JsAnchorElement createAnchorElement() {
    return createElement("anchor").cast();
  }

  public final JsAppletElement createAppletElement() {
    return createElement("applet").cast();
  }

  public final JsAreaElement createAreaElement() {
    return createElement("area").cast();
  }

  public final JsAudioElement createAudioElement() {
    return createElement("audio").cast();
  }

  public final JsBRElement createBRElement() {
    return createElement("br").cast();
  }

  public final JsBaseElement createBaseElement() {
    return createElement("base").cast();
  }

  public final JsBaseFontElement createBaseFontElement() {
    return createElement("basefont").cast();
  }

  public final JsBodyElement createBodyElement() {
    return createElement("body").cast();
  }

  public final JsButtonElement createButtonElement() {
    return createElement("button").cast();
  }

  public final JsCanvasElement createCanvasElement() {
    return createElement("canvas").cast();
  }

  public final JsContentElement createContentElement() {
    return createElement("content").cast();
  }

  public final JsDListElement createDListElement() {
    return createElement("dlist").cast();
  }

  public final JsDetailsElement createDetailsElement() {
    return createElement("details").cast();
  }

  public final JsDirectoryElement createDirectoryElement() {
    return createElement("directory").cast();
  }

  public final JsDivElement createDivElement() {
    return createElement("div").cast();
  }

  public final JsEmbedElement createEmbedElement() {
    return createElement("embed").cast();
  }

  public final JsFieldSetElement createFieldSetElement() {
    return createElement("fieldset").cast();
  }

  public final JsFontElement createFontElement() {
    return createElement("font").cast();
  }

  public final JsFormElement createFormElement() {
    return createElement("form").cast();
  }

  public final JsFrameElement createFrameElement() {
    return createElement("frame").cast();
  }

  public final JsFrameSetElement createFrameSetElement() {
    return createElement("frameset").cast();
  }

  public final JsHRElement createHRElement() {
    return createElement("hr").cast();
  }

  public final JsHeadElement createHeadElement() {
    return createElement("head").cast();
  }

  public final JsHeadingElement createHeadingElement() {
    return createElement("heading").cast();
  }

  public final JsHtmlElement createHtmlElement() {
    return createElement("html").cast();
  }

  public final JsIFrameElement createIFrameElement() {
    return createElement("iframe").cast();
  }

  public final JsImageElement createImageElement() {
    return createElement("image").cast();
  }

  public final JsInputElement createInputElement() {
    return createElement("input").cast();
  }

  public final JsKeygenElement createKeygenElement() {
    return createElement("keygen").cast();
  }

  public final JsLIElement createLIElement() {
    return createElement("li").cast();
  }

  public final JsLabelElement createLabelElement() {
    return createElement("label").cast();
  }

  public final JsLegendElement createLegendElement() {
    return createElement("legend").cast();
  }

  public final JsLinkElement createLinkElement() {
    return createElement("link").cast();
  }

  public final JsMapElement createMapElement() {
    return createElement("map").cast();
  }

  public final JsMarqueeElement createMarqueeElement() {
    return createElement("marquee").cast();
  }

  public final JsMediaElement createMediaElement() {
    return createElement("media").cast();
  }

  public final JsMenuElement createMenuElement() {
    return createElement("menu").cast();
  }

  public final JsMetaElement createMetaElement() {
    return createElement("meta").cast();
  }

  public final JsMeterElement createMeterElement() {
    return createElement("meter").cast();
  }

  public final JsModElement createModElement() {
    return createElement("mod").cast();
  }

  public final JsOListElement createOListElement() {
    return createElement("olist").cast();
  }

  public final JsObjectElement createObjectElement() {
    return createElement("object").cast();
  }

  public final JsOptGroupElement createOptGroupElement() {
    return createElement("optgroup").cast();
  }

  public final JsOptionElement createOptionElement() {
    return createElement("option").cast();
  }

  public final JsOutputElement createOutputElement() {
    return createElement("output").cast();
  }

  public final JsParagraphElement createParagraphElement() {
    return createElement("paragraph").cast();
  }

  public final JsParamElement createParamElement() {
    return createElement("param").cast();
  }

  public final JsPreElement createPreElement() {
    return createElement("pre").cast();
  }

  public final JsProgressElement createProgressElement() {
    return createElement("progress").cast();
  }

  public final JsQuoteElement createQuoteElement() {
    return createElement("quote").cast();
  }

  public final JsSVGAElement createSVGAElement() {
    return createSvgElement("a").cast();
  }

  public final JsSVGAltGlyphDefElement createSVGAltGlyphDefElement() {
    return createSvgElement("altglyphdef").cast();
  }

  public final JsSVGAltGlyphElement createSVGAltGlyphElement() {
    return createSvgElement("altglyph").cast();
  }

  public final JsSVGAltGlyphItemElement createSVGAltGlyphItemElement() {
    return createSvgElement("altglyphitem").cast();
  }

  public final JsSVGAnimateColorElement createSVGAnimateColorElement() {
    return createSvgElement("animatecolor").cast();
  }

  public final JsSVGAnimateElement createSVGAnimateElement() {
    return createSvgElement("animate").cast();
  }

  public final JsSVGAnimateMotionElement createSVGAnimateMotionElement() {
    return createSvgElement("animatemotion").cast();
  }

  public final JsSVGAnimateTransformElement createSVGAnimateTransformElement() {
    return createSvgElement("animatetransform").cast();
  }

  public final JsSVGAnimationElement createSVGAnimationElement() {
    return createSvgElement("animation").cast();
  }

  public final JsSVGCircleElement createSVGCircleElement() {
    return createSvgElement("circle").cast();
  }

  public final JsSVGClipPathElement createSVGClipPathElement() {
    return createSvgElement("clippath").cast();
  }

  public final JsSVGComponentTransferFunctionElement createSVGComponentTransferFunctionElement() {
    return createSvgElement("componenttransferfunction").cast();
  }

  public final JsSVGCursorElement createSVGCursorElement() {
    return createSvgElement("cursor").cast();
  }

  public final JsSVGDefsElement createSVGDefsElement() {
    return createSvgElement("defs").cast();
  }

  public final JsSVGDescElement createSVGDescElement() {
    return createSvgElement("desc").cast();
  }

  public final JsSVGEllipseElement createSVGEllipseElement() {
    return createSvgElement("ellipse").cast();
  }

  public final JsSVGFEBlendElement createSVGFEBlendElement() {
    return createSvgElement("feblend").cast();
  }

  public final JsSVGFEColorMatrixElement createSVGFEColorMatrixElement() {
    return createSvgElement("fecolormatrix").cast();
  }

  public final JsSVGFEComponentTransferElement createSVGFEComponentTransferElement() {
    return createSvgElement("fecomponenttransfer").cast();
  }

  public final JsSVGFECompositeElement createSVGFECompositeElement() {
    return createSvgElement("fecomposite").cast();
  }

  public final JsSVGFEConvolveMatrixElement createSVGFEConvolveMatrixElement() {
    return createSvgElement("feconvolvematrix").cast();
  }

  public final JsSVGFEDiffuseLightingElement createSVGFEDiffuseLightingElement() {
    return createSvgElement("fediffuselighting").cast();
  }

  public final JsSVGFEDisplacementMapElement createSVGFEDisplacementMapElement() {
    return createSvgElement("fedisplacementmap").cast();
  }

  public final JsSVGFEDistantLightElement createSVGFEDistantLightElement() {
    return createSvgElement("fedistantlight").cast();
  }

  public final JsSVGFEDropShadowElement createSVGFEDropShadowElement() {
    return createSvgElement("fedropshadow").cast();
  }

  public final JsSVGFEFloodElement createSVGFEFloodElement() {
    return createSvgElement("feflood").cast();
  }

  public final JsSVGFEFuncAElement createSVGFEFuncAElement() {
    return createSvgElement("fefunca").cast();
  }

  public final JsSVGFEFuncBElement createSVGFEFuncBElement() {
    return createSvgElement("fefuncb").cast();
  }

  public final JsSVGFEFuncGElement createSVGFEFuncGElement() {
    return createSvgElement("fefuncg").cast();
  }

  public final JsSVGFEFuncRElement createSVGFEFuncRElement() {
    return createSvgElement("fefuncr").cast();
  }

  public final JsSVGFEGaussianBlurElement createSVGFEGaussianBlurElement() {
    return createSvgElement("fegaussianblur").cast();
  }

  public final JsSVGFEImageElement createSVGFEImageElement() {
    return createSvgElement("feimage").cast();
  }

  public final JsSVGFEMergeElement createSVGFEMergeElement() {
    return createSvgElement("femerge").cast();
  }

  public final JsSVGFEMergeNodeElement createSVGFEMergeNodeElement() {
    return createSvgElement("femergenode").cast();
  }

  public final JsSVGFEMorphologyElement createSVGFEMorphologyElement() {
    return createSvgElement("femorphology").cast();
  }

  public final JsSVGFEOffsetElement createSVGFEOffsetElement() {
    return createSvgElement("feoffset").cast();
  }

  public final JsSVGFEPointLightElement createSVGFEPointLightElement() {
    return createSvgElement("fepointlight").cast();
  }

  public final JsSVGFESpecularLightingElement createSVGFESpecularLightingElement() {
    return createSvgElement("fespecularlighting").cast();
  }

  public final JsSVGFESpotLightElement createSVGFESpotLightElement() {
    return createSvgElement("fespotlight").cast();
  }

  public final JsSVGFETileElement createSVGFETileElement() {
    return createSvgElement("fetile").cast();
  }

  public final JsSVGFETurbulenceElement createSVGFETurbulenceElement() {
    return createSvgElement("feturbulence").cast();
  }

  public final JsSVGFilterElement createSVGFilterElement() {
    return createSvgElement("filter").cast();
  }

  public final JsSVGFontElement createSVGFontElement() {
    return createSvgElement("font").cast();
  }

  public final JsSVGFontFaceElement createSVGFontFaceElement() {
    return createSvgElement("fontface").cast();
  }

  public final JsSVGFontFaceFormatElement createSVGFontFaceFormatElement() {
    return createSvgElement("fontfaceformat").cast();
  }

  public final JsSVGFontFaceNameElement createSVGFontFaceNameElement() {
    return createSvgElement("fontfacename").cast();
  }

  public final JsSVGFontFaceSrcElement createSVGFontFaceSrcElement() {
    return createSvgElement("fontfacesrc").cast();
  }

  public final JsSVGFontFaceUriElement createSVGFontFaceUriElement() {
    return createSvgElement("fontfaceuri").cast();
  }

  public final JsSVGForeignObjectElement createSVGForeignObjectElement() {
    return createSvgElement("foreignobject").cast();
  }

  public final JsSVGGElement createSVGGElement() {
    return createSvgElement("g").cast();
  }

  public final JsSVGGlyphElement createSVGGlyphElement() {
    return createSvgElement("glyph").cast();
  }

  public final JsSVGGlyphRefElement createSVGGlyphRefElement() {
    return createSvgElement("glyphref").cast();
  }

  public final JsSVGGradientElement createSVGGradientElement() {
    return createSvgElement("gradient").cast();
  }

  public final JsSVGHKernElement createSVGHKernElement() {
    return createSvgElement("hkern").cast();
  }

  public final JsSVGImageElement createSVGImageElement() {
    return createSvgElement("image").cast();
  }

  public final JsSVGLineElement createSVGLineElement() {
    return createSvgElement("line").cast();
  }

  public final JsSVGLinearGradientElement createSVGLinearGradientElement() {
    return createSvgElement("lineargradient").cast();
  }

  public final JsSVGMPathElement createSVGMPathElement() {
    return createSvgElement("mpath").cast();
  }

  public final JsSVGMarkerElement createSVGMarkerElement() {
    return createSvgElement("marker").cast();
  }

  public final JsSVGMaskElement createSVGMaskElement() {
    return createSvgElement("mask").cast();
  }

  public final JsSVGMetadataElement createSVGMetadataElement() {
    return createSvgElement("metadata").cast();
  }

  public final JsSVGMissingGlyphElement createSVGMissingGlyphElement() {
    return createSvgElement("missingglyph").cast();
  }

  public final JsSVGPathElement createSVGPathElement() {
    return createSvgElement("path").cast();
  }

  public final JsSVGPatternElement createSVGPatternElement() {
    return createSvgElement("pattern").cast();
  }

  public final JsSVGPolygonElement createSVGPolygonElement() {
    return createSvgElement("polygon").cast();
  }

  public final JsSVGPolylineElement createSVGPolylineElement() {
    return createSvgElement("polyline").cast();
  }

  public final JsSVGRadialGradientElement createSVGRadialGradientElement() {
    return createSvgElement("radialgradient").cast();
  }

  public final JsSVGRectElement createSVGRectElement() {
    return createSvgElement("rect").cast();
  }

  public final JsSVGSVGElement createSVGElement() {
    return createSvgElement("svg").cast();
  }

  public final JsSVGScriptElement createSVGScriptElement() {
    return createSvgElement("script").cast();
  }

  public final JsSVGSetElement createSVGSetElement() {
    return createSvgElement("set").cast();
  }

  public final JsSVGStopElement createSVGStopElement() {
    return createSvgElement("stop").cast();
  }

  public final JsSVGStyleElement createSVGStyleElement() {
    return createSvgElement("style").cast();
  }

  public final JsSVGSwitchElement createSVGSwitchElement() {
    return createSvgElement("switch").cast();
  }

  public final JsSVGSymbolElement createSVGSymbolElement() {
    return createSvgElement("symbol").cast();
  }

  public final JsSVGTRefElement createSVGTRefElement() {
    return createSvgElement("tref").cast();
  }

  public final JsSVGTSpanElement createSVGTSpanElement() {
    return createSvgElement("tspan").cast();
  }

  public final JsSVGTextContentElement createSVGTextContentElement() {
    return createSvgElement("textcontent").cast();
  }

  public final JsSVGTextElement createSVGTextElement() {
    return createSvgElement("text").cast();
  }

  public final JsSVGTextPathElement createSVGTextPathElement() {
    return createSvgElement("textpath").cast();
  }

  public final JsSVGTextPositioningElement createSVGTextPositioningElement() {
    return createSvgElement("textpositioning").cast();
  }

  public final JsSVGTitleElement createSVGTitleElement() {
    return createSvgElement("title").cast();
  }

  public final JsSVGUseElement createSVGUseElement() {
    return createSvgElement("use").cast();
  }

  public final JsSVGVKernElement createSVGVKernElement() {
    return createSvgElement("vkern").cast();
  }

  public final JsSVGViewElement createSVGViewElement() {
    return createSvgElement("view").cast();
  }

  public final JsScriptElement createScriptElement() {
    return createElement("script").cast();
  }

  public final JsSelectElement createSelectElement() {
    return createElement("select").cast();
  }

  public final JsShadowElement createShadowElement() {
    return createElement("shadow").cast();
  }

  public final JsSourceElement createSourceElement() {
    return createElement("source").cast();
  }

  public final JsSpanElement createSpanElement() {
    return createElement("span").cast();
  }

  public final JsStyleElement createStyleElement() {
    return createElement("style").cast();
  }

  public final JsTableCaptionElement createTableCaptionElement() {
    return createElement("caption").cast();
  }

  public final JsTableCellElement createTableCellElement() {
    return createElement("tablecell").cast();
  }

  public final JsTableColElement createTableColElement() {
    return createElement("tablecol").cast();
  }

  public final JsTableElement createTableElement() {
    return createElement("table").cast();
  }

  public final JsTableRowElement createTableRowElement() {
    return createElement("tablerow").cast();
  }

  public final JsTableSectionElement createTableSectionElement() {
    return createElement("tablesection").cast();
  }

  public final JsTextAreaElement createTextAreaElement() {
    return createElement("textarea").cast();
  }

  public final JsTitleElement createTitleElement() {
    return createElement("title").cast();
  }

  public final JsTrackElement createTrackElement() {
    return createElement("track").cast();
  }

  public final JsUListElement createUListElement() {
    return createElement("ulist").cast();
  }

  public final JsUnknownElement createUnknownElement() {
    return createElement("unknown").cast();
  }

  public final JsVideoElement createVideoElement() {
    return createElement("video").cast();
  }
}
