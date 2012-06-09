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
import elemental.dom.DOMError;
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
  * <p>The <code>IDBRequest</code> interface of the IndexedDB&nbsp;API provides access to results of asynchronous requests to databases and database objects using event handler attributes. Each reading and writing operation on a database is done using a request.</p>
<p>The request object does not initially contain any information about the result of the operation, but once information becomes available, an event is fired on the request, and the information becomes available through the properties of the <code>IDBRequest</code> instance.</p>
<p>Inherits from: <a title="en/DOM/EventTarget" rel="internal" href="https://developer.mozilla.org/en/DOM/EventTarget">EventTarget</a></p>
  */
public interface IDBRequest extends EventTarget {

  DOMError getError();

  int getErrorCode();

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnsuccess();

  void setOnsuccess(EventListener arg);

  String getReadyState();

  Object getResult();

  Object getSource();

  IDBTransaction getTransaction();

  String getWebkitErrorMessage();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
