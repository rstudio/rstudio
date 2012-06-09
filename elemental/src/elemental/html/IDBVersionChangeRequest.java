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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div class="warning"><strong>Warning: </strong> The latest specification does not include this interface anymore as the <code>IDBDatabase.setVersion()</code> method has been removed. However, it is still implemented in not up-to-date browsers. See the compatibility table for version details.<br> The new way to do it is to use the <a title="en/IndexedDB/IDBOpenDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBOpenDBRequest"><code>IDBOpenDBRequest</code></a> interface which has now the <code>onblocked</code> handler and the newly needed <code>onupgradeneeded</code> one.</div>
<p>The <code>IDBVersionChangeRequest</code> interface the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB API </a>represents a request to change the version of a database. It is used only by the <a title="en/IndexedDB/IDBDatabase#setVersion" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabase#setVersion"><code>setVersion()</code></a> method of <code><a title="en/IndexedDB/IDBDatabase" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabase">IDBDatabase</a></code>.</p>
<p>Inherits from:&nbsp;<code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></p>
  */
public interface IDBVersionChangeRequest extends IDBRequest {


  /**
    * The event handler for the blocked event.
    */
  EventListener getOnblocked();

  void setOnblocked(EventListener arg);
}
