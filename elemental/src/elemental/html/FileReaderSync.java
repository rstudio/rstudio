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
  * <p>The <code>FileReaderSync</code> interface allows to read <code>File</code> or <code>Blob</code> objects in a synchronous way.</p>
<p>This interface is <a title="https://developer.mozilla.org/En/DOM/Worker/Functions_available_to_workers" rel="internal" href="https://developer.mozilla.org/En/DOM/Worker/Functions_available_to_workers">only available</a> in <a title="Worker" rel="internal" href="https://developer.mozilla.org/En/DOM/Worker">workers</a> as it enables synchronous I/O that could potentially block.</p>
  */
public interface FileReaderSync {


  /**
    * <p>This method reads the contents of the specified <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code>. When the read operation is finished, it returns an <code><a href="../JavaScript_typed_arrays/ArrayBuffer" rel="internal" title="/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code> representing the file's data. If an error happened during the read, the adequate exception is sent.</p>

<div id="section_5"><span id="Parameters"></span><h4 class="editable">Parameters</h4>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> to read into the <code><a href="../JavaScript_typed_arrays/ArrayBuffer" rel="internal" title="/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code>.</dd>
</dl>
</div><div id="section_6"><span id="Return_value"></span><h4 class="editable">Return value</h4>
<p>An <code><a href="../JavaScript_typed_arrays/ArrayBuffer" rel="internal" title="/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code> representing the file's data.</p>
</div><div id="section_7"><span id="Exceptions"></span><h4 class="editable">Exceptions</h4>
<p>The following exceptions can be raised by this method:</p>
<dl> <dt><code>NotFoundError</code></dt> <dd>is raised when the resource represented by the DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> cannot be found, e. g. because it has been erased.</dd> <dt><code>SecurityError</code></dt> <dd>is raised when one of the following problematic situation is detected: <ul> <li>the resource has been modified by a third party;</li> <li>two many read are performed simultaneously;</li> <li>the file pointed by the resource is unsafe for a use from the Web (like it is a system file).</li> </ul> </dd> <dt><code>NotReadableError</code></dt> <dd>is raised when the resource cannot be read due to a permission problem, like a concurrent lock.</dd> <dt><code>EncodingError</code></dt> <dd>is raised when the resource is a data URL and exceed the limit length defined by each browser.</dd>
</dl>
</div>
    */
  ArrayBuffer readAsArrayBuffer(Blob blob);


  /**
    * <p>This method reads the contents of the specified <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code>, which may be a <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code>. When the read operation is finished, it returns a <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a> containing the raw binary data from the file. If an error happened during the read, the adequate exception is sent.</p>
<div class="note"><strong>Note</strong> <strong>: </strong>This method is deprecated and <code>readAsArrayBuffer()</code> should be used instead.</div>

<div id="section_9"><span id="Parameters_2"></span><h4 class="editable">Parameters</h4>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> to read into the <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a>.</dd>
</dl>
</div><div id="section_10"><span id="Return_value_2"></span><h4 class="editable">Return value</h4>
<p><code>A </code><a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a> containing the raw binary data from the resource</p>
</div><div id="section_11"><span id="Exceptions_2"></span><h4 class="editable">Exceptions</h4>
<p>The following exceptions can be raised by this method:</p>
<dl> <dt><code>NotFoundError</code></dt> <dd>is raised when the resource represented by the DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> cannot be found, e. g. because it has been erased.</dd> <dt><code>SecurityError</code></dt> <dd>is raised when one of the following problematic situation is detected: <ul> <li>the resource has been modified by a third party;</li> <li>two many read are performed simultaneously;</li> <li>the file pointed by the resource is unsafe for a use from the Web (like it is a system file).</li> </ul> </dd> <dt><code>NotReadableError</code></dt> <dd>is raised when the resource cannot be read due to a permission problem, like a concurrent lock.</dd> <dt><code>EncodingError</code></dt> <dd>is raised when the resource is a data URL and exceed the limit length defined by each browser.</dd>
</dl>
</div>
    */
  String readAsBinaryString(Blob blob);


  /**
    * <p>This method reads the contents of the specified <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code>. When the read operation is finished, it returns a data URL representing the file's data. If an error happened during the read, the adequate exception is sent.</p>

<div id="section_5"> <div id="section_17"><span id="Parameters_4"></span><h4 class="editable"><span>Parameters</span></h4> <dl> <dt><code>blob</code></dt> <dd>The DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> to read.</dd> </dl>
</div></div>
<div id="section_6"> <div id="section_18"><span id="Return_value_4"></span><h4 class="editable"><span>Return value</span></h4> <p>An <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a> representing the file's data as a data URL.</p>
</div></div>
<div id="section_19"><span id="Exceptions_4"></span><h4 class="editable"><span>Exceptions</span></h4>
<p>The following exceptions can be raised by this method:</p>
<dl> <dt><code>NotFoundError</code></dt> <dd>is raised when the resource represented by the DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> cannot be found, e. g. because it has been erased.</dd> <dt><code>SecurityError</code></dt> <dd>is raised when one of the following problematic situation is detected: <ul> <li>the resource has been modified by a third party;</li> <li>too many read are performed simultaneously;</li> <li>the file pointed by the resource is unsafe for a use from the Web (like it is a system file).</li> </ul> </dd> <dt><code>NotReadableError</code></dt> <dd>is raised when the resource cannot be read due to a permission problem, like a concurrent lock.</dd> <dt><code>EncodingError</code></dt> <dd>is raised when the resource is a data URL and exceed the limit length defined by each browser.</dd>
</dl>
<dl> <dt></dt>
</dl></div>
    */
  String readAsDataURL(Blob blob);


  /**
    * <p>This methods reads the specified blob's contents. When the read operation is finished, it returns a <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a> containing the file represented as a text string. The optional <strong><code>encoding</code></strong> parameter indicates the encoding to be used. If not present, the method will apply a detection algorithm for it. If an error happened during the read, the adequate exception is sent.</p>

<div id="section_13"><span id="Parameters_3"></span><h4 class="editable">Parameters</h4>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> to read into the <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a>.</dd> <dt><code>encoding</code></dt> <dd>Optional. A string representing the encoding to be used, like <strong>iso-8859-1</strong> or <strong>UTF-8</strong>.</dd>
</dl>
</div><div id="section_14"><span id="Return_value_3"></span><h4 class="editable">Return value</h4>
<p>A <a href="https://developer.mozilla.org/en/DOM/DOMString" rel="internal" title="DOMString"><code>DOMString</code></a> containing the raw binary data from the resource</p>
</div><div id="section_15"><span id="Exceptions_3"></span><h4 class="editable">Exceptions</h4>
<p>The following exceptions can be raised by this method:</p>
<dl> <dt><code>NotFoundError</code></dt> <dd>is raised when the resource represented by the DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> cannot be found, e. g. because it has been erased.</dd> <dt><code>SecurityError</code></dt> <dd>is raised when one of the following problematic situation is detected: <ul> <li>the resource has been modified by a third party;</li> <li>two many read are performed simultaneously;</li> <li>the file pointed by the resource is unsafe for a use from the Web (like it is a system file).</li> </ul> </dd> <dt><code>NotReadableError</code></dt> <dd>is raised when the resource cannot be read due to a permission problem, like a concurrent lock.</dd>
</dl>
</div>
    */
  String readAsText(Blob blob);


  /**
    * <p>This methods reads the specified blob's contents. When the read operation is finished, it returns a <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a> containing the file represented as a text string. The optional <strong><code>encoding</code></strong> parameter indicates the encoding to be used. If not present, the method will apply a detection algorithm for it. If an error happened during the read, the adequate exception is sent.</p>

<div id="section_13"><span id="Parameters_3"></span><h4 class="editable">Parameters</h4>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> to read into the <a title="DOMString" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMString"><code>DOMString</code></a>.</dd> <dt><code>encoding</code></dt> <dd>Optional. A string representing the encoding to be used, like <strong>iso-8859-1</strong> or <strong>UTF-8</strong>.</dd>
</dl>
</div><div id="section_14"><span id="Return_value_3"></span><h4 class="editable">Return value</h4>
<p>A <a href="https://developer.mozilla.org/en/DOM/DOMString" rel="internal" title="DOMString"><code>DOMString</code></a> containing the raw binary data from the resource</p>
</div><div id="section_15"><span id="Exceptions_3"></span><h4 class="editable">Exceptions</h4>
<p>The following exceptions can be raised by this method:</p>
<dl> <dt><code>NotFoundError</code></dt> <dd>is raised when the resource represented by the DOM <code><a href="https://developer.mozilla.org/en/DOM/Blob" rel="custom">Blob</a></code> or <code><a href="https://developer.mozilla.org/en/DOM/File" rel="custom">File</a></code> cannot be found, e. g. because it has been erased.</dd> <dt><code>SecurityError</code></dt> <dd>is raised when one of the following problematic situation is detected: <ul> <li>the resource has been modified by a third party;</li> <li>two many read are performed simultaneously;</li> <li>the file pointed by the resource is unsafe for a use from the Web (like it is a system file).</li> </ul> </dd> <dt><code>NotReadableError</code></dt> <dd>is raised when the resource cannot be read due to a permission problem, like a concurrent lock.</dd>
</dl>
</div>
    */
  String readAsText(Blob blob, String encoding);
}
