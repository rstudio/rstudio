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
package elemental.events;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <div><div>

<a rel="custom" href="http://mxr.mozilla.org/mozilla-central/source/dom/interfaces/events/nsIDOMProgressEvent.idl"><code>dom/interfaces/events/nsIDOMProgressEvent.idl</code></a><span><a rel="internal" href="https://developer.mozilla.org/en/Interfaces/About_Scriptable_Interfaces" title="en/Interfaces/About_Scriptable_Interfaces">Scriptable</a></span></div><span>This interface represents the events sent with progress information while uploading data using the <code>XMLHttpRequest</code> object.</span><div><div>1.0</div><div>11.0</div><div></div><div>Introduced</div><div>Gecko 1.9.1</div><div title="Introduced in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
"></div><div title="Last changed in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
"></div></div>
<div>Inherits from: <code><a rel="custom" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/nsIDOMEvent">nsIDOMEvent</a></code>
<span>Last changed in Gecko 1.9.1 (Firefox 3.5 / Thunderbird 3.0 / SeaMonkey 2.0)
</span></div></div>
<p></p>
<p>The <code>nsIDOMProgressEvent</code> is used in the media elements (<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video">&lt;video&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/audio">&lt;audio&gt;</a></code>
) to inform interested code of the progress of the media download. This implementation is a placeholder until the specification is complete, and is compatible with the WebKit ProgressEvent.</p>
  */
public interface ProgressEvent extends Event {


  /**
    * Specifies whether or not the total size of the transfer is known. <strong>Read only.</strong>
    */
  boolean isLengthComputable();


  /**
    * The number of bytes transferred since the beginning of the operation. This doesn't include headers and other overhead, but only the content itself. <strong>Read only.</strong>
    */
  double getLoaded();


  /**
    * The total number of bytes of content that will be transferred during the operation. If the total size is unknown, this value is zero. <strong>Read only.</strong>
    */
  double getTotal();
}
