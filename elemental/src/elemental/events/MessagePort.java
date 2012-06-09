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
import elemental.util.Indexable;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/threads/nsIDOMWorkers.idl"><code>dom/interfaces/threads/nsIDOMWorkers.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>This interface represents a worker thread's message port, which is used to allow the worker to post messages back to its creator.</span><div><div>1.0</div><div>11.0</div><div></div><div>Introduced</div><div>Gecko 1.9.1</div><div title="Introduced in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
"></div><div title="Last changed in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
"></div></div>
<div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsISupports">nsISupports</a></code>
<span>Last changed in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
</span></div>
  */
public interface MessagePort extends EventTarget {

  EventListener getOnmessage();

  void setOnmessage(EventListener arg);

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  void close();

  boolean dispatchEvent(Event evt);


  /**
    * <p>Posts a message into the event queue.</p>

<div id="section_4"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>aMessage</code></dt> <dd>The message to post.</dd>
</dl>
</div>
    */
  void postMessage(String message);


  /**
    * <p>Posts a message into the event queue.</p>

<div id="section_4"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>aMessage</code></dt> <dd>The message to post.</dd>
</dl>
</div>
    */
  void postMessage(String message, Indexable messagePorts);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  void start();

  void webkitPostMessage(String message);

  void webkitPostMessage(String message, Indexable transfer);
}
