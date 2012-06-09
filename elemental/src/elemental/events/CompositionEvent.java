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

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/events/nsIDOMCompositionEvent.idl"><code>dom/interfaces/events/nsIDOMCompositionEvent.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>An event interface for composition events</span><div><div>1.0</div><div>11.0</div><div></div><div>Introduced</div><div>Gecko 9.0</div><div title="Introduced in Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
"></div><div title="Last changed in Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
"></div></div>
<div>Inherits from: <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIDOMUIEvent&amp;ident=nsIDOMUIEvent" class="new">nsIDOMUIEvent</a></code>
<span>Last changed in Gecko 9.0 (Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
</span></div></div>
<p></p>
<p>The DOM <code>CompositionEvent</code> represents events that occur due to the user indirectly entering text.</p>
  */
public interface CompositionEvent extends UIEvent {


  /**
    * <p>For <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOM_event_reference/compositionstart">compositionstart</a></code>
 events, this is the currently selected text that will be replaced by the string being composed. This value doesn't change even if content changes the selection range; rather, it indicates the string that was selected when composition started.</p> <p>For <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOM_event_reference/compositionupdate">compositionupdate</a></code>
, this is the string as it stands currently as editing is ongoing.</p> <p>For <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOM_event_reference/compositionend">compositionend</a></code>
 events, this is the string as committed to the editor.</p> <p><strong>Read only</strong>.</p>
    */
  String getData();


  /**
    * <p>Initializes the attributes of a composition event.</p>

<div id="section_5"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>typeArg</code></dt> <dd>The type of composition event; this will be one of <code>compositionstart</code>, <code>compositionupdate</code>, or <code>compositionend</code>.</dd> <dt><code>canBubbleArg</code></dt> <dd>Whether or not the event can bubble.</dd> <dt><code>cancelableArg</code></dt> <dd>Whether or not the event can be canceled.</dd> <dt><code>viewArg</code></dt> <dd>?</dd> <dt><code>dataArg</code></dt> <dd>The value of the <code>data</code> attribute.</dd> <dt><code>localeArg</code></dt> <dd>The value of the <code>locale</code> attribute.</dd>
</dl>
</div>
    */
  void initCompositionEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Window viewArg, String dataArg);
}
