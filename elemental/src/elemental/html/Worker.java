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
package elemental.html;
import elemental.util.Indexable;
import elemental.events.EventListener;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>Workers are background tasks that can be easily created and can send messages back to their creators. Creating a worker is as simple as calling the <code>Worker()</code>&nbsp;constructor, specifying a script to be run in the worker thread.</p>
<p>Of note is the fact that workers may in turn spawn new workers as long as those workers are hosted within the same origin as the parent page.&nbsp; In addition, workers may use <a title="En/XMLHttpRequest" class="internal" rel="internal" href="https://developer.mozilla.org/en/DOM/XMLHttpRequest"><code>XMLHttpRequest</code></a> for network I/O, with the exception that the <code>responseXML</code> and <code>channel</code> attributes on <code>XMLHttpRequest</code> always return <code>null</code>.</p>
<p>For a list of global functions available to workers, see <a title="En/DOM/Worker/Functions available to workers" rel="internal" href="https://developer.mozilla.org/En/DOM/Worker/Functions_available_to_workers">Functions available to workers</a>.</p>
<div class="geckoVersionNote">
<p>
</p><div class="geckoVersionHeading">Gecko 2.0 note<div>(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
</div></div>
<p></p>
<p>If you want to use workers in extensions, and would like to have access to <a title="en/js-ctypes" rel="internal" href="https://developer.mozilla.org/en/js-ctypes">js-ctypes</a>, you should use the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/ChromeWorker">ChromeWorker</a></code>
 object instead.</p>
</div>
<p>See <a class="internal" title="en/Using DOM workers" rel="internal" href="https://developer.mozilla.org/En/Using_web_workers">Using web workers</a> for examples and details.</p>
  */
public interface Worker extends AbstractWorker {


  /**
    * An event listener that is called whenever a <code>MessageEvent</code> with type <code>message</code> bubbles through the worker. The message is stored in the event's <code>data</code> member.
    */
  EventListener getOnmessage();

  void setOnmessage(EventListener arg);


  /**
    * <p>Sends a message to the worker's inner scope. This accepts a single parameter, which is the data to send to the worker. The data may be any value or JavaScript object that does not contain functions or cyclical references (since the object is converted to <a class="internal" title="En/JSON" rel="internal" href="https://developer.mozilla.org/en/JSON">JSON</a> internally).</p>

<div id="section_10"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>aMessage<br> </code></dt> <dd>The object to deliver to the worker; this will be in the data field in the event delivered to the <code>onmessage</code> handler. This may be any value or JavaScript object that does not contain functions or cyclical references (since the object is converted to <a class="internal" title="En/JSON" rel="internal" href="https://developer.mozilla.org/en/JSON">JSON</a> internally).</dd>
</dl>
</div>
    */
  void postMessage(Object message);


  /**
    * <p>Sends a message to the worker's inner scope. This accepts a single parameter, which is the data to send to the worker. The data may be any value or JavaScript object that does not contain functions or cyclical references (since the object is converted to <a class="internal" title="En/JSON" rel="internal" href="https://developer.mozilla.org/en/JSON">JSON</a> internally).</p>

<div id="section_10"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>aMessage<br> </code></dt> <dd>The object to deliver to the worker; this will be in the data field in the event delivered to the <code>onmessage</code> handler. This may be any value or JavaScript object that does not contain functions or cyclical references (since the object is converted to <a class="internal" title="En/JSON" rel="internal" href="https://developer.mozilla.org/en/JSON">JSON</a> internally).</dd>
</dl>
</div>
    */
  void postMessage(Object message, Indexable messagePorts);


  /**
    * <p>Immediately terminates the worker. This does not offer the worker an opportunity to finish its operations; it is simply stopped at once.</p>
<pre>void terminate();
</pre>
    */
  void terminate();

  void webkitPostMessage(Object message);

  void webkitPostMessage(Object message, Indexable messagePorts);
}
