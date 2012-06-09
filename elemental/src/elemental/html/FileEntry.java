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
<p>The <code>FileEntry</code> interface of the <a title="en/DOM/File_API/File_System_API" rel="internal" href="https://developer.mozilla.org/en/DOM/File_API/File_System_API">FileSystem API</a> represents a file in a file system.</p>
  */
public interface FileEntry extends Entry {


  /**
    * <p>Creates a new <code>FileWriter</code> associated with the file that the <code>FileEntry</code> represents.</p>
<pre>void createWriter (
 in FileWriterCallback successCallback, optional ErrorCallback errorCallback
);</pre> <div id="section_4"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the new <code>FileWriter</code>.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_5"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void createWriter(FileWriterCallback successCallback);


  /**
    * <p>Creates a new <code>FileWriter</code> associated with the file that the <code>FileEntry</code> represents.</p>
<pre>void createWriter (
 in FileWriterCallback successCallback, optional ErrorCallback errorCallback
);</pre> <div id="section_4"><span id="Parameter"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the new <code>FileWriter</code>.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_5"><span id="Returns"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl> </div>
    */
  void createWriter(FileWriterCallback successCallback, ErrorCallback errorCallback);


  /**
    * <p>Returns a File that represents the current state of the file that this <code>FileEntry</code> represents.</p>
<pre>void file (
  <em>FileCallback successCallback, optional ErrorCallback errorCallback</em>
);</pre>
<div id="section_7"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the new <code>FileWriter</code>.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_8"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void file(FileCallback successCallback);


  /**
    * <p>Returns a File that represents the current state of the file that this <code>FileEntry</code> represents.</p>
<pre>void file (
  <em>FileCallback successCallback, optional ErrorCallback errorCallback</em>
);</pre>
<div id="section_7"><span id="Parameter_2"></span><h5 class="editable">Parameter</h5>
<dl> <dt>successCallback</dt> <dd>A callback that is called with the new <code>FileWriter</code>.</dd> <dt>errorCallback</dt> <dd>A callback that is called when errors happen.</dd> </dl> </div><div id="section_8"><span id="Returns_2"></span><h5 class="editable">Returns</h5>
<dl> <dt><code>void</code></dt>
</dl>
</div>
    */
  void file(FileCallback successCallback, ErrorCallback errorCallback);
}
