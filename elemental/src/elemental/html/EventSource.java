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
  * <p>The <code>EventSource</code> interface is used to manage server-sent events. You can set the onmessage attribute to a JavaScript function to receive non-typed messages (that is, messages with no <code>event</code> field). You can also call <code>addEventListener()</code> to listen for events just like any other event source.</p>
<p>See <a title="en/Server-sent events/Using server-sent events" rel="internal" href="https://developer.mozilla.org/en/Server-sent_events/Using_server-sent_events">Using server-sent events</a> for further details.</p>
  */
public interface EventSource extends EventTarget {

  /**
    * The connection is not being established, has been closed or there was a fatal error.
    */

    static final int CLOSED = 2;

  /**
    * The connection is being established.
    */

    static final int CONNECTING = 0;

  /**
    * The connection is open and dispatching events.
    */

    static final int OPEN = 1;

  String getURL();


  /**
    * A JavaScript function to call when an error occurs.
    */
  EventListener getOnerror();

  void setOnerror(EventListener arg);


  /**
    * A JavaScript function to call when an a message without an <code>event</code> field arrives.
    */
  EventListener getOnmessage();

  void setOnmessage(EventListener arg);


  /**
    * A JavaScript function to call when the connection has opened.
    */
  EventListener getOnopen();

  void setOnopen(EventListener arg);


  /**
    * The state of the connection, must be one of <code>CONNECTING</code>, <code>OPEN</code>, or <code>CLOSED</code>. <strong>Read only.</strong>
    */
  int getReadyState();


  /**
    * Read only.
    */
  String getUrl();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * Closes the connection, if any, and sets the <code>readyState</code> attribute to <code>CLOSED</code>. If the connection is already closed, the method does nothing.
    */
  void close();

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
