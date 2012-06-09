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
import elemental.util.Indexable;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><strong>DRAFT</strong>
<div>This page is not complete.</div>
</div>

<p></p>
<p>A <code>MessageEvent</code> is sent to clients using WebSockets when data is received from the server. This is delivered to the listener indicated by the <code>WebSocket</code> object's <code>onmessage</code> attribute.</p>
  */
public interface MessageEvent extends Event {


  /**
    * The data from the server.
    */
  Object getData();

  String getLastEventId();

  String getOrigin();

  Indexable getPorts();

  Window getSource();

  void initMessageEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Object dataArg, String originArg, String lastEventIdArg, Window sourceArg, Indexable messagePorts);

  void webkitInitMessageEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Object dataArg, String originArg, String lastEventIdArg, Window sourceArg, Indexable transferables);
}
