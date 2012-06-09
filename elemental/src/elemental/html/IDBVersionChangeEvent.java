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
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The <code>IDBVersionChangeEvent</code> interface of the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB&nbsp;API</a> indicates that the version of the database has changed.</p>
<p>The specification has changed and some not up-to-date browsers only support the deprecated unique attribute, <code>version</code>, from an early draft version.</p>
  */
public interface IDBVersionChangeEvent extends Event {


  /**
    * <div class="warning"><strong>Warning:</strong> While this property is still implemented by not up-to-date browsers, the latest specification does replace it by the <code>oldVersion</code> and <code>newVersion</code> attributes. See compatibility table to know what browsers support them.</div> The new version of the database in a <a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a> transaction.
    */
  String getVersion();
}
