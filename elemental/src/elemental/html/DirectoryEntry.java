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
  * <div><strong>DRAFT</strong> <div>This page is not complete.</div>
</div>
<p>The <code>DirectoryEntry</code> interface of the <a title="en/DOM/File_API/File_System_API" rel="internal" href="https://developer.mozilla.org/en/DOM/File_API/File_System_API">FileSystem API</a> represents a directory in a file system.</p>
  */
public interface DirectoryEntry extends Entry {


  /**
    * <p>Creates a new DirectoryReader to read entries from this Directory.</p>
<pre>void getMetada ();</pre> <div id="section_7"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>DirectoryReader</code></dt>
</dl> </div>
    */
  DirectoryReader createReader();


  /**
    * <p>Creates or looks up a directory.</p>
<pre>void vopyTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getDirectory must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getDirectory must create it as a zero-length file and return a corresponding getDirectory. </li> <li>If create is not true and the path doesn't exist, getDirectory must fail. </li> <li>If create is not true and the path exists, but is a directory, getDirectory must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a getDirectory corresponding to path.</li>
</ul> <dt>successCallback</dt> <dd>A callback that is called to return the DirectoryEntry selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
 </div><div id="section_13"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getDirectory(String path);


  /**
    * <p>Creates or looks up a directory.</p>
<pre>void vopyTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getDirectory must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getDirectory must create it as a zero-length file and return a corresponding getDirectory. </li> <li>If create is not true and the path doesn't exist, getDirectory must fail. </li> <li>If create is not true and the path exists, but is a directory, getDirectory must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a getDirectory corresponding to path.</li>
</ul> <dt>successCallback</dt> <dd>A callback that is called to return the DirectoryEntry selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
 </div><div id="section_13"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getDirectory(String path, Object flags);


  /**
    * <p>Creates or looks up a directory.</p>
<pre>void vopyTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getDirectory must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getDirectory must create it as a zero-length file and return a corresponding getDirectory. </li> <li>If create is not true and the path doesn't exist, getDirectory must fail. </li> <li>If create is not true and the path exists, but is a directory, getDirectory must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a getDirectory corresponding to path.</li>
</ul> <dt>successCallback</dt> <dd>A callback that is called to return the DirectoryEntry selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
 </div><div id="section_13"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getDirectory(String path, Object flags, EntryCallback successCallback);


  /**
    * <p>Creates or looks up a directory.</p>
<pre>void vopyTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_12"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getDirectory must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getDirectory must create it as a zero-length file and return a corresponding getDirectory. </li> <li>If create is not true and the path doesn't exist, getDirectory must fail. </li> <li>If create is not true and the path exists, but is a directory, getDirectory must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a getDirectory corresponding to path.</li>
</ul> <dt>successCallback</dt> <dd>A callback that is called to return the DirectoryEntry selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
 </div><div id="section_13"><span id="Returns_4"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getDirectory(String path, Object flags, EntryCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Creates or looks up a file.</p>
<pre>void moveTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getFile must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getFile must create it as a zero-length file and return a corresponding FileEntry. </li> <li>If create is not true and the path doesn't exist, getFile must fail. </li> <li>If create is not true and the path exists, but is a directory, getFile must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a FileEntry corresponding to path.</li>
</ul>
<dl> <dt>successCallback</dt> <dd>A callback that is called to return the file selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_10"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getFile(String path);


  /**
    * <p>Creates or looks up a file.</p>
<pre>void moveTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getFile must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getFile must create it as a zero-length file and return a corresponding FileEntry. </li> <li>If create is not true and the path doesn't exist, getFile must fail. </li> <li>If create is not true and the path exists, but is a directory, getFile must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a FileEntry corresponding to path.</li>
</ul>
<dl> <dt>successCallback</dt> <dd>A callback that is called to return the file selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_10"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getFile(String path, Object flags);


  /**
    * <p>Creates or looks up a file.</p>
<pre>void moveTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getFile must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getFile must create it as a zero-length file and return a corresponding FileEntry. </li> <li>If create is not true and the path doesn't exist, getFile must fail. </li> <li>If create is not true and the path exists, but is a directory, getFile must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a FileEntry corresponding to path.</li>
</ul>
<dl> <dt>successCallback</dt> <dd>A callback that is called to return the file selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_10"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getFile(String path, Object flags, EntryCallback successCallback);


  /**
    * <p>Creates or looks up a file.</p>
<pre>void moveTo (
  <em>(in DOMString path, optional Flags options, optional EntryCallback successCallback, optional ErrorCallback errorCallback);</em>
);</pre>
<div id="section_9"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>path</dt> <dd>Either an absolute path or a relative path from this DirectoryEntry to the file to be looked up or created. It is an error to attempt to create a file whose immediate parent does not yet exist.</dd> <dt>options</dt>
</dl>
<ul> <li>If create and exclusive are both true, and the path already exists, getFile must fail.</li> <li> If create is true, the path doesn't exist, and no other error occurs, getFile must create it as a zero-length file and return a corresponding FileEntry. </li> <li>If create is not true and the path doesn't exist, getFile must fail. </li> <li>If create is not true and the path exists, but is a directory, getFile must fail. </li> <li>Otherwise, if no other error occurs, getFile must return a FileEntry corresponding to path.</li>
</ul>
<dl> <dt>successCallback</dt> <dd>A callback that is called to return the file selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_10"><span id="Returns_3"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void getFile(String path, Object flags, EntryCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Deletes a directory and all of its contents, if any. If you are deleting a directory that contains a file that cannot be removed, some of the contents of the directory might be deleted. You cannot delete the root directory of a file system.</p>
<pre>DOMString toURL (
  <em>(in </em>VoidCallback successCallback, optional ErrorCallback errorCallback<em>);</em>
);</pre>
<div id="section_15"><span id="Parameter_3"></span><h5 class="editable">Parameter</h5>
<dl>
<dt>successCallback</dt> <dd>A callback that is called to return the DirectoryEntry selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl> </div><div id="section_16"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void removeRecursively(VoidCallback successCallback);


  /**
    * <p>Deletes a directory and all of its contents, if any. If you are deleting a directory that contains a file that cannot be removed, some of the contents of the directory might be deleted. You cannot delete the root directory of a file system.</p>
<pre>DOMString toURL (
  <em>(in </em>VoidCallback successCallback, optional ErrorCallback errorCallback<em>);</em>
);</pre>
<div id="section_15"><span id="Parameter_3"></span><h5 class="editable">Parameter</h5>
<dl>
<dt>successCallback</dt> <dd>A callback that is called to return the DirectoryEntry selected or created.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd>
</dl> </div><div id="section_16"><span id="Returns_5"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void removeRecursively(VoidCallback successCallback, ErrorCallback errorCallback);
}
