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
  * A <code>CloseEvent</code> is sent to clients using WebSockets when the connection is closed. This is delivered to the listener indicated by the <code>WebSocket</code> object's <code>onclose</code> attribute.
  */
public interface CloseEvent extends Event {


  /**
    * The WebSocket connection close code provided by the server. See <a title="en/XPCOM_Interface_Reference/nsIWebSocketChannel#Status_codes" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIWebSocketChannel#Status_codes">Status codes</a> for possible values.
    */
  int getCode();


  /**
    * A string indicating the reason the server closed the connection. This is specific to the particular server and sub-protocol.
    */
  String getReason();


  /**
    * Indicates whether or not the connection was cleanly closed.
    */
  boolean isWasClean();
}
