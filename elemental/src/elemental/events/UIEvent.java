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
package elemental.events;
import elemental.html.Window;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/events/nsIDOMUIEvent.idl"><code>dom/interfaces/events/nsIDOMUIEvent.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>A basic event interface for all user interface events</span><div><div>1.0</div><div>11.0</div><div title="Introduced in Gecko 1.0 
"></div><div title="Last changed in Gecko 9.0 
"></div></div>
<div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMEvent">nsIDOMEvent</a></code>
<span>Last changed in Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
</span></div></div>
<p></p>
<p>The DOM <code>UIEvent</code> represents simple user interface events.</p>
  */
public interface UIEvent extends Event {

  int getCharCode();


  /**
    * Detail about the event, depending on the type of event. <strong>Read only.</strong>
    */
  int getDetail();

  int getKeyCode();

  int getLayerX();

  int getLayerY();

  int getPageX();

  int getPageY();


  /**
    * A view which generated the event. <strong>Read only.</strong>
    */
  Window getView();

  int getWhich();


  /**
    * <p>Initializes the UIEvent object.</p>

<div id="section_5"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>typeArg</code></dt> <dd>The type of UI event.</dd> <dt><code>canBubbleArg</code></dt> <dd>Whether or not the event can bubble.</dd> <dt><code>cancelableArg</code></dt> <dd>Whether or not the event can be canceled.</dd> <dt><code>viewArg</code></dt> <dd>Specifies the <code>view</code> attribute value. This may be <code>null</code>.</dd> <dt><code>detailArg</code></dt> <dd>Specifies the detail attribute value.</dd>
</dl>
</div>
    */
  void initUIEvent(String type, boolean canBubble, boolean cancelable, Window view, int detail);
}
