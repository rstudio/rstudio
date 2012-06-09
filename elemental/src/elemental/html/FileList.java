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
  * <p>An object of this type is returned by the <code>files</code> property of the HTML&nbsp;input element; this lets you access the list of files selected with the <code>&lt;input type="file"&gt;</code> element. It's also used for a list of files dropped into web content when using the drag and drop API; see the <a title="En/DragDrop/DataTransfer" rel="internal" href="https://developer.mozilla.org/En/DragDrop/DataTransfer"><code>DataTransfer</code></a> object for details on this usage.</p>
<p>

</p><div><p>Gecko 1.9.2 note</p><p>Prior to Gecko 1.9.2, the input element only supported a single file being selected at a time, meaning that the FileList would contain only one file. Starting with Gecko 1.9.2, if the input element's multiple attribute is true, the FileList may contain multiple files.</p></div>
  */
public interface FileList extends Indexable {


  /**
    * A read-only value indicating the number of files in the list.
    */
  int getLength();


  /**
    * <p>Returns a <a title="en/DOM/File" rel="internal" href="https://developer.mozilla.org/en/DOM/File"><code>File</code></a> object representing the file at the specified index in the file list.</p>

<div id="section_6"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>index</code></dt> <dd>The zero-based index of the file to retrieve from the list.</dd>
</dl>
</div><div id="section_7"><span id="Return_value"></span><h6 class="editable">Return value</h6>
<p>The <a title="en/DOM/File" rel="internal" href="https://developer.mozilla.org/en/DOM/File"><code>File</code></a> representing the requested file.</p>
</div>
    */
  File item(int index);
}
