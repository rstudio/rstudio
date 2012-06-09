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
  * <p>The <code>FileReader</code> object lets web applications asynchronously read the contents of files (or raw data buffers) stored on the user's computer, using <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 objects to specify the file or data to read. File objects may be obtained in one of two ways: from a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/FileList">FileList</a></code>
 object returned as a result of a user selecting files using the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/input">&lt;input&gt;</a></code>
 element, or from a drag and drop operation's <a title="En/DragDrop/DataTransfer" rel="internal" href="https://developer.mozilla.org/En/DragDrop/DataTransfer"><code>DataTransfer</code></a> object.</p>
<p>To create a <code>FileReader</code>, simply do the following:</p>
<pre>var reader = new FileReader();
</pre>
<p>See <a title="en/Using files from web applications" rel="internal" href="https://developer.mozilla.org/en/Using_files_from_web_applications">Using files from web applications</a> for details and examples.</p>
  */
public interface FileReader extends EventTarget {

  /**
    * The entire read request has been completed.
    */

    static final int DONE = 2;

  /**
    * No data has been loaded yet.
    */

    static final int EMPTY = 0;

  /**
    * Data is currently being loaded.
    */

    static final int LOADING = 1;


  /**
    * The error that occurred while reading the file. <strong>Read only.</strong>
    */
  FileError getError();


  /**
    * Called when the read operation is aborted.
    */
  EventListener getOnabort();

  void setOnabort(EventListener arg);


  /**
    * Called when an error occurs.
    */
  EventListener getOnerror();

  void setOnerror(EventListener arg);


  /**
    * Called when the read operation is successfully completed.
    */
  EventListener getOnload();

  void setOnload(EventListener arg);


  /**
    * Called when the read is completed, whether successful or not. This is called after either <code>onload</code> or <code>onerror</code>.
    */
  EventListener getOnloadend();

  void setOnloadend(EventListener arg);


  /**
    * Called when reading the data is about to begin.
    */
  EventListener getOnloadstart();

  void setOnloadstart(EventListener arg);


  /**
    * Called periodically while the data is being read.
    */
  EventListener getOnprogress();

  void setOnprogress(EventListener arg);


  /**
    * Indicates the state of the <code>FileReader</code>. This will be one of the <a rel="custom" href="https://developer.mozilla.org/en/DOM/FileReader#State_constants">State constants</a>. <strong>Read only.</strong>
    */
  int getReadyState();


  /**
    * The file's contents. This property is only valid after the read operation is complete, and the format of the data depends on which of the methods was used to initiate the read operation. <strong>Read only.</strong>
    */
  Object getResult();


  /**
    * <p>Aborts the read operation. Upon return, the <code>readyState</code> will be <code>DONE</code>.</p>

<div id="section_7"><span id="Parameters"></span><h6 class="editable">Parameters</h6>
<p>None.</p>
</div><div id="section_8"><span id="Exceptions_thrown"></span><h6 class="editable">Exceptions thrown</h6>
<dl> <dt><code>DOM_FILE_ABORT_ERR</code></dt> <dd><code>abort()</code> was called while no read operation was in progress (that is, the state wasn't <code>LOADING</code>). <div class="note"><strong>Note:</strong>&nbsp;This exception was not thrown by Gecko until Gecko 6.0 (Firefox 6.0 / Thunderbird 6.0 / SeaMonkey 2.3)
.</div>
</dd>
</dl>
<p>
</p><div>
<span id="readAsArrayBuffer()"></span></div></div>
    */
  void abort();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);


  /**
    * <div id="section_8"><p>Starts reading the contents of the specified <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
. When the read operation is finished, the <code>readyState</code> will become <code>DONE</code>, and the <code>onloadend</code> callback, if any, will be called. At that time, the <code>result</code> attribute contains an <code><a title="/en/JavaScript_typed_arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code> representing the file's data.</p>

</div><div id="section_9"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 to read into the <code><a title="/en/JavaScript_typed_arrays/ArrayBuffer" rel="internal" href="https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer">ArrayBuffer</a></code>.</dd>
</dl>
</div>
    */
  void readAsArrayBuffer(Blob blob);


  /**
    * <p>Starts reading the contents of the specified <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
, which may be a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
. When the read operation is finished, the <code>readyState</code> will become <code>DONE</code>, and the <code>onloadend</code> callback, if any, will be called. At that time, the <code>result</code> attribute contains the raw binary data from the file.</p>

<div id="section_11"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 from which to read.</dd>
</dl>
</div>
    */
  void readAsBinaryString(Blob blob);


  /**
    * <p>Starts reading the contents of the specified <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
. When the read operation is finished, the <code>readyState</code> will become <code>DONE</code>, and the <code>onloadend</code> callback, if any, will be called. At that time, the <code>result</code> attribute contains a <code>data:</code> URL representing the file's data.</p>

<p>This method is useful, for example, to get a preview of an image before uploading it:</p>

          <pre name="code" class="xml">&lt;!doctype html&gt;
&lt;html&gt;
&lt;head&gt;
&lt;meta content="text/html; charset=UTF-8" http-equiv="Content-Type" /&gt;
&lt;title&gt;Image preview example&lt;/title&gt;
&lt;script type="text/javascript"&gt;
oFReader = new FileReader(), rFilter = /^(image\/bmp|image\/cis-cod|image\/gif|image\/ief|image\/jpeg|image\/jpeg|image\/jpeg|image\/pipeg|image\/png|image\/svg\+xml|image\/tiff|image\/x-cmu-raster|image\/x-cmx|image\/x-icon|image\/x-portable-anymap|image\/x-portable-bitmap|image\/x-portable-graymap|image\/x-portable-pixmap|image\/x-rgb|image\/x-xbitmap|image\/x-xpixmap|image\/x-xwindowdump)$/i;

oFReader.onload = function (oFREvent) {
  document.getElementById("uploadPreview").src = oFREvent.target.result;
};

function loadImageFile() {
  if (document.getElementById("uploadImage").files.length === 0) { return; }
  var oFile = document.getElementById("uploadImage").files[0];
  if (!rFilter.test(oFile.type)) { alert("You must select a valid image file!"); return; }
  oFReader.readAsDataURL(oFile);
}
&lt;/script&gt;
&lt;/head&gt;

&lt;body onload="loadImageFile();"&gt;
&lt;form name="uploadForm"&gt;
&lt;table&gt;
&lt;tbody&gt;
&lt;tr&gt;
&lt;td&gt;&lt;img id="uploadPreview" style="width: 100px; height: 100px;" src="data:image/svg+xml,%3C%3Fxml%20version%3D%221.0%22%3F%3E%0A%3Csvg%20width%3D%22153%22%20height%3D%22153%22%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%3E%0A%20%3Cg%3E%0A%20%20%3Ctitle%3ENo%20image%3C/title%3E%0A%20%20%3Crect%20id%3D%22externRect%22%20height%3D%22150%22%20width%3D%22150%22%20y%3D%221.5%22%20x%3D%221.500024%22%20stroke-width%3D%223%22%20stroke%3D%22%23666666%22%20fill%3D%22%23e1e1e1%22/%3E%0A%20%20%3Ctext%20transform%3D%22matrix%286.66667%2C%200%2C%200%2C%206.66667%2C%20-960.5%2C%20-1099.33%29%22%20xml%3Aspace%3D%22preserve%22%20text-anchor%3D%22middle%22%20font-family%3D%22Fantasy%22%20font-size%3D%2214%22%20id%3D%22questionMark%22%20y%3D%22181.249569%22%20x%3D%22155.549819%22%20stroke-width%3D%220%22%20stroke%3D%22%23666666%22%20fill%3D%22%23000000%22%3E%3F%3C/text%3E%0A%20%3C/g%3E%0A%3C/svg%3E" alt="Image preview" /&gt;&lt;/td&gt;
&lt;td&gt;&lt;input id="uploadImage" type="file" name="myPhoto" onchange="loadImageFile();" /&gt;&lt;/td&gt;
&lt;/tr&gt;
&lt;/tbody&gt;
&lt;/table&gt;
&lt;p&gt;&lt;input type="submit" value="Send" /&gt;&lt;/p&gt;
&lt;/form&gt;
&lt;/body&gt;
&lt;/html&gt;</pre>
        
<div id="section_13"><span id="Parameters_4"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>file</code></dt> <dd>The DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 from which to read.</dd>
</dl>
</div>
    */
  void readAsDataURL(Blob blob);


  /**
    * <p>Starts reading the specified blob's contents. When the read operation is finished, the <code>readyState</code> will become <code>DONE</code>, and the <code>onloadend</code> callback, if any, will be called. At that time, the <code>result</code> attribute contains the contents of the file as a text string.</p>

<div id="section_15"><span id="Parameters_5"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 from which to read.</dd> <dt><code>encoding</code> 
<span title="">Optional</span>
</dt> <dd>A string indicating the encoding to use for the returned data. By default, UTF-8 is assumed if this parameter is not specified.</dd>
</dl>
</div>
    */
  void readAsText(Blob blob);


  /**
    * <p>Starts reading the specified blob's contents. When the read operation is finished, the <code>readyState</code> will become <code>DONE</code>, and the <code>onloadend</code> callback, if any, will be called. At that time, the <code>result</code> attribute contains the contents of the file as a text string.</p>

<div id="section_15"><span id="Parameters_5"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>blob</code></dt> <dd>The DOM <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Blob">Blob</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/File">File</a></code>
 from which to read.</dd> <dt><code>encoding</code> 
<span title="">Optional</span>
</dt> <dd>A string indicating the encoding to use for the returned data. By default, UTF-8 is assumed if this parameter is not specified.</dd>
</dl>
</div>
    */
  void readAsText(Blob blob, String encoding);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
