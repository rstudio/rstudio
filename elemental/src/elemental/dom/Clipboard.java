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
package elemental.dom;
import elemental.html.FileList;
import elemental.util.Indexable;
import elemental.html.ImageElement;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/widget/public/nsIClipboard.idl"><code>widget/public/nsIClipboard.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>This interface supports basic clipboard operations such as: setting, retrieving, emptying, matching and supporting clipboard data.</span><div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsISupports">nsISupports</a></code>
<span>Last changed in Gecko 1.8 (Firefox 1.5 / Thunderbird 1.5 / SeaMonkey 1.0)
</span></div>
  */
public interface Clipboard {

  String getDropEffect();

  void setDropEffect(String arg);

  String getEffectAllowed();

  void setEffectAllowed(String arg);

  FileList getFiles();

  DataTransferItemList getItems();

  Indexable getTypes();

  void clearData();

  void clearData(String type);


  /**
    * <p>This method retrieves data from the clipboard into a transferable.</p>

<div id="section_8"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>aTransferable</code></dt> <dd>The transferable to receive data from the clipboard.</dd> <dt><code>aWhichClipboard</code></dt> <dd>Specifies the clipboard to which this operation applies.</dd>
</dl>
</div>
    */
  String getData(String type);


  /**
    * <p>This method sets the data from a transferable on the native clipboard.</p>

<div id="section_13"><span id="Parameters_5"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>aTransferable</code></dt> <dd>The transferable containing the data to put on the clipboard.</dd> <dt><code>anOwner</code></dt> <dd>The owner of the transferable.</dd> <dt><code>aWhichClipboard</code></dt> <dd>Specifies the clipboard to which this operation applies.</dd>
</dl>
</div>
    */
  boolean setData(String type, String data);

  void setDragImage(ImageElement image, int x, int y);
}
