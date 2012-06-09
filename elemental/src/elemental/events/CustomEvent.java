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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The DOM <code>CustomEvent</code> are events initialized by an application for any purpose. It's represented by the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIDOMCustomEvent&amp;ident=nsIDOMCustomEvent" class="new">nsIDOMCustomEvent</a></code>
&nbsp;interface, which extends the <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMEvent">nsIDOMEvent</a></code>
 interface.
  */
public interface CustomEvent extends Event {


  /**
    * The data passed when initializing the event.
    */
  Object getDetail();


  /**
    * <p>Initializes the event in a manner analogous to the similarly-named method in the DOM Events interfaces.</p>

<div id="section_5"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>type</code></dt> <dd>The name of the event.</dd> <dt><code>canBubble</code></dt> <dd>A boolean indicating whether the event bubbles up through the DOM or not.</dd> <dt><code>cancelable</code></dt> <dd>A boolean indicating whether the event is cancelable.</dd> <dt><code>detail</code></dt> <dd>The data passed when initializing the event.</dd>
</dl>
</div>
    */
  void initCustomEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Object detailArg);
}
