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
import elemental.html.TextTrack;
import elemental.html.TextTrackCueList;
import elemental.js.events.JsEvent;
import elemental.events.EventListener;
import elemental.html.TextTrackCue;
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

public class JsTextTrack extends JsElementalMixinBase  implements TextTrack {
  protected JsTextTrack() {}

  public final native JsTextTrackCueList getActiveCues() /*-{
    return this.activeCues;
  }-*/;

  public final native JsTextTrackCueList getCues() /*-{
    return this.cues;
  }-*/;

  public final native String getKind() /*-{
    return this.kind;
  }-*/;

  public final native String getLabel() /*-{
    return this.label;
  }-*/;

  public final native String getLanguage() /*-{
    return this.language;
  }-*/;

  public final native int getMode() /*-{
    return this.mode;
  }-*/;

  public final native void setMode(int param_mode) /*-{
    this.mode = param_mode;
  }-*/;

  public final native EventListener getOncuechange() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.oncuechange);
  }-*/;

  public final native void setOncuechange(EventListener listener) /*-{
    this.oncuechange = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native void addCue(TextTrackCue cue) /*-{
    this.addCue(cue);
  }-*/;

  public final native void removeCue(TextTrackCue cue) /*-{
    this.removeCue(cue);
  }-*/;
}
