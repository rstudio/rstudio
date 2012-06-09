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
  * <p>The <code>File</code> object provides information about -- and access to the contents of -- files. These are generally retrieved from a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/FileList">FileList</a></code>
 object returned as a result of a user selecting files using the <code>input</code> element, or from a drag and drop operation's <a title="En/DragDrop/DataTransfer" rel="internal" href="https://developer.mozilla.org/En/DragDrop/DataTransfer"><code>DataTransfer</code></a> object.</p>
<div class="geckoVersionNote">
<p>
</p><div class="geckoVersionHeading">Gecko 2.0 note<div>(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
</div></div>
<p></p>
<p>Starting in Gecko 2.0&nbsp;(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
, the File object inherits from the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
&nbsp;interface, which provides methods and properties providing further information about the file.</p>
</div>
<p>The file reference can be saved when the form is submitted while the user is offline, so that the data can be retrieved and uploaded when the Internet connection is restored.</p>
<div class="note"><strong>Note:</strong> The <code>File</code> object as implemented by Gecko offers several non-standard methods for reading the contents of the file. These should <em>not</em> be used, as they will prevent your web application from being used in other browsers, as well as in future versions of Gecko, which will likely remove these methods.</div>
  */
public interface File extends Blob {

  Date getLastModifiedDate();


  /**
    * The name of the file referenced by the <code>File</code> object. <strong>Read only.</strong> 
<span title="(Firefox 3.6 / Thunderbird 3.1 / Fennec 1.0)
">Requires Gecko 1.9.2</span>
    */
  String getName();

  String getWebkitRelativePath();
}
