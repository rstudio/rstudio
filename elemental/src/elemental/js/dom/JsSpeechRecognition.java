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
import elemental.dom.SpeechGrammarList;
import elemental.js.events.JsEvent;
import elemental.events.EventListener;
import elemental.dom.SpeechRecognition;
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

public class JsSpeechRecognition extends JsElementalMixinBase  implements SpeechRecognition {
  protected JsSpeechRecognition() {}

  public final native boolean isContinuous() /*-{
    return this.continuous;
  }-*/;

  public final native void setContinuous(boolean param_continuous) /*-{
    this.continuous = param_continuous;
  }-*/;

  public final native JsSpeechGrammarList getGrammars() /*-{
    return this.grammars;
  }-*/;

  public final native void setGrammars(SpeechGrammarList param_grammars) /*-{
    this.grammars = param_grammars;
  }-*/;

  public final native String getLang() /*-{
    return this.lang;
  }-*/;

  public final native void setLang(String param_lang) /*-{
    this.lang = param_lang;
  }-*/;

  public final native EventListener getOnaudioend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onaudioend);
  }-*/;

  public final native void setOnaudioend(EventListener listener) /*-{
    this.onaudioend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnaudiostart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onaudiostart);
  }-*/;

  public final native void setOnaudiostart(EventListener listener) /*-{
    this.onaudiostart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onend);
  }-*/;

  public final native void setOnend(EventListener listener) /*-{
    this.onend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnerror() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onerror);
  }-*/;

  public final native void setOnerror(EventListener listener) /*-{
    this.onerror = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnnomatch() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onnomatch);
  }-*/;

  public final native void setOnnomatch(EventListener listener) /*-{
    this.onnomatch = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnresult() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onresult);
  }-*/;

  public final native void setOnresult(EventListener listener) /*-{
    this.onresult = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnresultdeleted() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onresultdeleted);
  }-*/;

  public final native void setOnresultdeleted(EventListener listener) /*-{
    this.onresultdeleted = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsoundend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsoundend);
  }-*/;

  public final native void setOnsoundend(EventListener listener) /*-{
    this.onsoundend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnsoundstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onsoundstart);
  }-*/;

  public final native void setOnsoundstart(EventListener listener) /*-{
    this.onsoundstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnspeechend() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onspeechend);
  }-*/;

  public final native void setOnspeechend(EventListener listener) /*-{
    this.onspeechend = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnspeechstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onspeechstart);
  }-*/;

  public final native void setOnspeechstart(EventListener listener) /*-{
    this.onspeechstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnstart() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onstart);
  }-*/;

  public final native void setOnstart(EventListener listener) /*-{
    this.onstart = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native void abort() /*-{
    this.abort();
  }-*/;

  public final native void start() /*-{
    this.start();
  }-*/;

  public final native void stop() /*-{
    this.stop();
  }-*/;
}
