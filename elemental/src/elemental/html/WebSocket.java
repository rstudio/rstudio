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
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><p><strong>This is an experimental feature</strong><br>Because this feature is still in development in some browsers, check the <a href="#AutoCompatibilityTable">compatibility table</a> for the proper prefixes to use in various browsers.</p></div>
<p></p>
<p>The <code>WebSocket</code> object provides the API&nbsp;for creating and managing a <a title="en/WebSockets" rel="internal" href="https://developer.mozilla.org/en/WebSockets">WebSocket</a> connection to a server, as well as for sending and receiving data on the connection.</p>
  */
public interface WebSocket extends EventTarget {

  /**
    * The connection is closed or couldn't be opened.
    */

    static final int CLOSED = 3;

  /**
    * The connection is in the process of closing.
    */

    static final int CLOSING = 2;

  /**
    * The connection is not yet open.
    */

    static final int CONNECTING = 0;

  /**
    * The connection is open and ready to communicate.
    */

    static final int OPEN = 1;

  String getURL();


  /**
    * A string indicating the type of binary data being transmitted by the connection. This should be either "blob"&nbsp;if DOM&nbsp;<code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
&nbsp;objects are being used or "arraybuffer" if <a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer"><code>ArrayBuffer</code></a> objects are being used.
    */
  String getBinaryType();

  void setBinaryType(String arg);


  /**
    * The number of bytes of data that have been queued using calls to  but not yet transmitted to the network. This value does not reset to zero when the connection is closed; if you keep calling , this will continue to climb. <strong>Read only.</strong>
    */
  int getBufferedAmount();


  /**
    * The extensions selected by the server. This is currently only the empty string or a list of extensions as negotiated by the connection.
    */
  String getExtensions();


  /**
    * An event listener to be called when the WebSocket connection's <code>readyState</code> changes to <code>CLOSED</code>. The listener receives a <a title="en/WebSockets/WebSockets reference/CloseEvent" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent"><code>CloseEvent</code></a> named "close".
    */
  EventListener getOnclose();

  void setOnclose(EventListener arg);


  /**
    * An event listener to be called when an error occurs. This is a simple event named "error".
    */
  EventListener getOnerror();

  void setOnerror(EventListener arg);


  /**
    * An event listener to be called when a message is received from the server. The listener receives a <a title="en/WebSockets/WebSockets reference/MessageEvent" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/MessageEvent"><code>MessageEvent</code></a> named "message".
    */
  EventListener getOnmessage();

  void setOnmessage(EventListener arg);


  /**
    * An event listener to be called when the WebSocket connection's <code>readyState</code> changes to <code>OPEN</code>; this indicates that the connection is ready to send and receive data. The event is a simple one with the name "open".
    */
  EventListener getOnopen();

  void setOnopen(EventListener arg);


  /**
    * A string indicating the name of the sub-protocol the server selected; this will be one of the strings specified in the <code>protocols</code> parameter when creating the WebSocket object.
    */
  String getProtocol();


  /**
    * The current state of the connection; this is one of the <a rel="custom" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/WebSocket#Ready_state_constants">Ready state constants</a>. <strong>Read only.</strong>
    */
  int getReadyState();


  /**
    * The URL&nbsp;as resolved by the constructor. This is always an absolute URL. <strong>Read only.</strong>
    */
  String getUrl();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * <p>Closes the WebSocket connection or connection attempt, if any. If the connection is already <code>CLOSED</code>, this method does nothing.</p>

<div id="section_7"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>code</code> 
<span title="">Optional</span>
</dt> <dd>A numeric value indicating the status code explaining why the connection is being closed. If this parameter is not specified, a default value of 1000 (indicating a normal "transaction complete"&nbsp;closure)&nbsp;is assumed. See the <a title="en/WebSockets/WebSockets reference/CloseEvent#Status codes" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent#Status_codes">list of status codes</a> on the <a title="en/WebSockets/WebSockets reference/CloseEvent" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent"><code>CloseEvent</code></a> page for permitted values.</dd> <dt><code>reason</code> 
<span title="">Optional</span>
</dt> <dd>A human-readable string explaining why the connection is closing. This string must be no longer than 123 UTF-8 characters.</dd>
</dl>
</div><div id="section_8"><span id="Exceptions_thrown"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INVALID_ACCESS_ERR</code></dt> <dd>An invalid <code>code</code> was specified.</dd> <dt><code>SYNTAX_ERR</code></dt> <dd>The <code>reason</code> string is too long or contains unpaired surrogates.</dd>
</dl>
</div>
    */
  void close();


  /**
    * <p>Closes the WebSocket connection or connection attempt, if any. If the connection is already <code>CLOSED</code>, this method does nothing.</p>

<div id="section_7"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>code</code> 
<span title="">Optional</span>
</dt> <dd>A numeric value indicating the status code explaining why the connection is being closed. If this parameter is not specified, a default value of 1000 (indicating a normal "transaction complete"&nbsp;closure)&nbsp;is assumed. See the <a title="en/WebSockets/WebSockets reference/CloseEvent#Status codes" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent#Status_codes">list of status codes</a> on the <a title="en/WebSockets/WebSockets reference/CloseEvent" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent"><code>CloseEvent</code></a> page for permitted values.</dd> <dt><code>reason</code> 
<span title="">Optional</span>
</dt> <dd>A human-readable string explaining why the connection is closing. This string must be no longer than 123 UTF-8 characters.</dd>
</dl>
</div><div id="section_8"><span id="Exceptions_thrown"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INVALID_ACCESS_ERR</code></dt> <dd>An invalid <code>code</code> was specified.</dd> <dt><code>SYNTAX_ERR</code></dt> <dd>The <code>reason</code> string is too long or contains unpaired surrogates.</dd>
</dl>
</div>
    */
  void close(int code);


  /**
    * <p>Closes the WebSocket connection or connection attempt, if any. If the connection is already <code>CLOSED</code>, this method does nothing.</p>

<div id="section_7"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>code</code> 
<span title="">Optional</span>
</dt> <dd>A numeric value indicating the status code explaining why the connection is being closed. If this parameter is not specified, a default value of 1000 (indicating a normal "transaction complete"&nbsp;closure)&nbsp;is assumed. See the <a title="en/WebSockets/WebSockets reference/CloseEvent#Status codes" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent#Status_codes">list of status codes</a> on the <a title="en/WebSockets/WebSockets reference/CloseEvent" rel="internal" href="https://developer.mozilla.org/en/WebSockets/WebSockets_reference/CloseEvent"><code>CloseEvent</code></a> page for permitted values.</dd> <dt><code>reason</code> 
<span title="">Optional</span>
</dt> <dd>A human-readable string explaining why the connection is closing. This string must be no longer than 123 UTF-8 characters.</dd>
</dl>
</div><div id="section_8"><span id="Exceptions_thrown"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INVALID_ACCESS_ERR</code></dt> <dd>An invalid <code>code</code> was specified.</dd> <dt><code>SYNTAX_ERR</code></dt> <dd>The <code>reason</code> string is too long or contains unpaired surrogates.</dd>
</dl>
</div>
    */
  void close(int code, String reason);

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * <p>Transmits data to the server over the WebSocket connection.</p>

<div id="section_11"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>data</code></dt> <dd>A text string to send to the server.</dd>
</dl>
</div><div id="section_12"><span id="Exceptions_thrown_2"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>INVALID_STATE_ERR</code></dt> <dd>The connection is not currently <code>OPEN</code>.</dd> <dt><code>SYNTAX_ERR</code></dt> <dd>The data is a string that has unpaired surrogates.</dd>
</dl>
</div><div id="section_13"><span id="Remarks"></span><h6 class="editable">Remarks</h6>
<div class="geckoVersionNote"> <p>
</p><div class="geckoVersionHeading">Gecko 6.0 note<div>(Firefox 6.0 / Thunderbird 6.0 / SeaMonkey 2.3)
</div></div>
<p></p> <p>Gecko's implementation of the <code>send()</code>&nbsp;method differs somewhat from the specification in Gecko 6.0; Gecko returns a <code>boolean</code> indicating whether or not the connection is still open (and, by extension, that the data was successfully queued or transmitted); this is corrected in Gecko 8.0 (Firefox 8.0 / Thunderbird 8.0 / SeaMonkey 2.5)
. In addition, at this time, Gecko does not support <code><a title="en/JavaScript typed arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code> or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 data types.</p>
</div>
</div>
    */
  boolean send(String data);
}
