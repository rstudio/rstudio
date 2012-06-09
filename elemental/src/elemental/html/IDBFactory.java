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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>The <code>IDBFactory</code> interface of the <a title="en/IndexedDB" rel="internal" href="https://developer.mozilla.org/en/IndexedDB">IndexedDB&nbsp;API</a> lets applications asynchronously access the indexed databases. The object that implements the interface is&nbsp; <code>window.indexedDB</code>. You open—that is, create and access—and delete a database with the object and not directly with <code>IDBFactory</code>.</p>
<p>This interface still has vendor prefixes, that is to say, you have to make calls with <code>mozIndexedDB.open()</code> for Firefox and <code>webkitIndexedDB.open()</code> for Chrome.</p>
  */
public interface IDBFactory {


  /**
    * <p>Compares two values as keys to determine equality and ordering for IndexedDB operations, such as storing and iterating. Do not use this method for comparing arbitrary JavaScript values, because many JavaScript values are either not valid IndexedDB keys (booleans and objects, for example) or are treated as equivalent IndexedDB keys (for example, since IndexedDB ignores arrays with non-numeric properties and treats them as empty arrays, so any non-numeric array are treated as equivalent).</p>
<p>This throws an exception if either of the values is not a valid key. &nbsp;</p>

<div id="section_11"><span id="Parameters_3"></span><h5 class="editable">Parameters</h5>
<dl> <dt>first</dt> <dd>The first key to compare.</dd> <dt>second</dt> <dd>The second key to compare.</dd>
</dl>
</div><div id="section_12"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt>Integer</dt> <dd> <table class="standard-table" width="434"> <thead> <tr> <th scope="col" width="216">Returned value</th> <th scope="col" width="206">Description</th> </tr> </thead> <tbody> <tr> <td>-1</td> <td>1st key &lt; 2nd</td> </tr> <tr> <td>0</td> <td>1st key = 2nd</td> </tr> <tr> <td>1</td> <td>1st key &gt; 2nd</td> </tr> </tbody> </table> </dd>
</dl>
</div><div id="section_13"><span id="Exceptions"></span><h5 class="editable">Exceptions</h5>
<p>This method can raise an <a title="en/IndexedDB/IDBDatabaseException" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException">IDBDatabaseException</a> with the following code:</p>
<br>
<br>
<br><table class="standard-table"> <thead> <tr> <th scope="col" width="131">Attribute</th> <th scope="col" width="698">Description</th> </tr> <tr> <td><code><a title="en/IndexedDB/IDBDatabaseException#NON_TRANSIENT_ERR" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabaseException#NON_TRANSIENT_ERR">NON_TRANSIENT_ERR</a></code></td> <td>One of the supplied keys was not a valid key.</td> </tr> </thead> <tbody> </tbody>
</table>
</div>
    */
  short cmp(Object first, Object second);


  /**
    * <p>Request deleting a database. The method returns&nbsp;&nbsp;an IDBRequest object&nbsp;immediately, and performs the deletion operation&nbsp;asynchronously.</p>
<p>The deletion operation (performed in a different thread) consists of the following steps:</p>
<ol> <li>If there is no database with the given name, exit successfully.</li> <li>Fire an <a title="en/IndexedDB/IDBVersionChangeEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeEvent">IDBVersionChangeEvent</a> at all connection objects connected to the named database, with <code><a title="en/IndexedDB/IDBVersionChangeEvent#attr version" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeEvent#attr_version">version</a></code> set to <code>null</code>.</li> <li>If any connection objects connected to the database are still open, fire a <code>blocked</code> event at the request object returned by the <code>deleteDatabase</code> method, using <a title="en/IndexedDB/IDBVersionChangeEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeEvent">IDBVersionChangeEvent</a> with <code><a title="en/IndexedDB/IDBVersionChangeEvent#attr version" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBVersionChangeEvent#attr_version">version</a></code> set to <code>null</code>.</li> <li>Wait until all open connections to the database are closed.</li> <li>Delete the database.</li>
</ol>
<p>If the database is successfully deleted, then an <a title="en/IndexedDB/IDBSuccessEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent">IDBSuccessEvent</a> is fired on the request object returned from this method, with its <code><a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a></code> set to <code>null</code>.</p>
<p>If an error occurs while the database is being deleted, then an error event is fired on the request object that is returned from this method, with its <code><a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code">code</a></code> and <code><a title="en/IndexedDB/IDBErrorEvent#attr message" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_message">message</a></code> set to appropriate values.</p>
<p><strong>Tip:</strong> If the browser you are using hasn't implemented this yet, you can delete the object stores one by one, thus effectively removing the database.</p>

<div id="section_8"><span id="Parameters_2"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the database.</dd>
</dl>
</div><div id="section_9"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this request are fired. In the latest draft of the specification, which has not yet been implemented by browsers, the returned object is <code>IDBOpenRequest</code>.</dd>
</dl>
</div>
    */
  IDBVersionChangeRequest deleteDatabase(String name);

  IDBRequest getDatabaseNames();


  /**
    * <div class="warning"><strong>Warning:</strong> The description documents the old specification. Some browsers still implement this method. The specifications have changed, but the changes have not yet been implemented by all browser. See the compatibility table for more information.</div>
<p>Request opening a <a title="en/IndexedDB#gloss database connection" rel="internal" href="https://developer.mozilla.org/en/IndexedDB#gloss_database_connection">connection to a database</a>. The method returns &nbsp;an IDBRequest object&nbsp;immediately, and performs the opening operation asynchronously.&nbsp;</p>
<p>The opening operation—which is performed in a separate thread—consists of the following steps:</p>
<ol> <li>If a database named <code>myAwesomeDatabase</code> already exists: <ul> <li>Wait until any existing <code><a title="en/IndexedDB/IDBTransaction#VERSION CHANGE" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBTransaction#VERSION_CHANGE">VERSION_CHANGE</a></code> transactions have finished.</li> <li>If the database has a deletion pending, wait until it has been deleted.</li> </ul> </li> <li>If no database with that name exists, create a database with the provided name, with the empty string as its version, and no object stores.</li> <li>Create a connection to the database.</li>
</ol>
<p>If the operation is successful, an <a title="en/IndexedDB/IDBSuccessEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent">IDBSuccessEvent</a> is fired on the request object that is returned from this method, with its <a title="en/IndexedDB/IDBSuccessEvent#attr result" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBSuccessEvent#attr_result">result</a> attribute set to the new <a title="en/IndexedDB/IDBDatabase" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBDatabase">IDBDatabase</a> object for the connection.</p>
<p>If an error occurs while the database connection is being opened, then an <a title="en/IndexedDB/IDBErrorEvent" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent">error event</a> is fired on the request object returned from this method, with its <a title="en/IndexedDB/IDBErrorEvent#attr code" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_code"><code>code</code></a> and&nbsp; <code><a title="en/IndexedDB/IDBErrorEvent#attr message" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBErrorEvent#attr_message">message</a></code> set to appropriate values.</p>

<div id="section_5"><span id="Parameters"></span><h5 class="editable">Parameters</h5>
<dl> <dt>name</dt> <dd>The name of the database.</dd> <dt>version</dt> <dd>The version of the database.</dd>
</dl>
</div><div id="section_6"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code><a title="en/IndexedDB/IDBRequest" rel="internal" href="https://developer.mozilla.org/en/IndexedDB/IDBRequest">IDBRequest</a></code></dt> <dd>A request object on which subsequent events related to this request are fired. In the latest draft of the specification, which has not yet been implemented by browsers, the returned object is <code>IDBOpenRequest</code>.</dd>
</dl>
</div>
    */
  IDBRequest open(String name);
}
