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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface Geolocation {


  /**
    * When the <code>clearWatch()</code> method is called, the <code>watch()</code> process stops calling for new position identifiers and cease invoking callbacks.
    */
  void clearWatch(int watchId);


  /**
    * <p>Acquires the user's current position via a new position object. If this fails, <code>errorCallback</code> is invoked with an <code>nsIDOMGeoPositionError</code> argument.</p>

<div id="section_8"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>successCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionCallback">nsIDOMGeoPositionCallback</a></code> to be called when the current position is available.</dd>
</dl>
<dl> <dt><code>errorCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionErrorCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionErrorCallback"> nsIDOMGeoPositionErrorCallback</a></code> that is called if an error occurs while retrieving the position; this parameter is optional.</dd>
</dl>
<dl> <dt><code>options</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionOptions" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionOptions">nsIDOMGeoPositionOptions</a></code> object specifying options; this parameter is optional.</dd>
</dl>
</div>
    */
  void getCurrentPosition(PositionCallback successCallback);


  /**
    * <p>Acquires the user's current position via a new position object. If this fails, <code>errorCallback</code> is invoked with an <code>nsIDOMGeoPositionError</code> argument.</p>

<div id="section_8"><span id="Parameters_2"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>successCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionCallback">nsIDOMGeoPositionCallback</a></code> to be called when the current position is available.</dd>
</dl>
<dl> <dt><code>errorCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionErrorCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionErrorCallback"> nsIDOMGeoPositionErrorCallback</a></code> that is called if an error occurs while retrieving the position; this parameter is optional.</dd>
</dl>
<dl> <dt><code>options</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionOptions" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionOptions">nsIDOMGeoPositionOptions</a></code> object specifying options; this parameter is optional.</dd>
</dl>
</div>
    */
  void getCurrentPosition(PositionCallback successCallback, PositionErrorCallback errorCallback);


  /**
    * <p>Similar to <a title="En/NsIDOMGeoGeolocation#getCurrentPosition()" class="internal" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoGeolocation#getCurrentPosition()"><code>getCurrentPosition()</code></a>, except it continues to call the callback with updated position information periodically until <a title="En/NsIDOMGeoGeolocation#clearWatch()" class="internal" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoGeolocation#clearWatch()"><code>clearWatch()</code></a>&nbsp;is called.</p>

<div id="section_10"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>successCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionCallback">nsIDOMGeoPositionCallback</a></code> that is to be called whenever new position information is available.</dd>
</dl>
<dl> <dt><code>errorCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionErrorCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionErrorCallback"> nsIDOMGeoPositionErrorCallback</a></code> to call when an error occurs; this is an optional parameter.</dd>
</dl>
<dl> <dt><code>options</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionOptions" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionOptions">nsIDOMGeoPositionOptions</a></code> object specifying options; this parameter is optional.</dd>
</dl>
</div><div id="section_11"><span id="Return_value"></span><h6 class="editable">Return value</h6>
<p>An ID&nbsp;number that can be used to reference the watcher in the future when calling <code><a title="en/nsIDOMGeolocation#clearWatch()" class="internal" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoGeolocation#clearWatch()">clearWatch()</a></code>.</p>
</div>
    */
  int watchPosition(PositionCallback successCallback);


  /**
    * <p>Similar to <a title="En/NsIDOMGeoGeolocation#getCurrentPosition()" class="internal" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoGeolocation#getCurrentPosition()"><code>getCurrentPosition()</code></a>, except it continues to call the callback with updated position information periodically until <a title="En/NsIDOMGeoGeolocation#clearWatch()" class="internal" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoGeolocation#clearWatch()"><code>clearWatch()</code></a>&nbsp;is called.</p>

<div id="section_10"><span id="Parameters_3"></span><h6 class="editable">Parameters</h6>
<dl> <dt><code>successCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionCallback">nsIDOMGeoPositionCallback</a></code> that is to be called whenever new position information is available.</dd>
</dl>
<dl> <dt><code>errorCallback</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionErrorCallback" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionErrorCallback"> nsIDOMGeoPositionErrorCallback</a></code> to call when an error occurs; this is an optional parameter.</dd>
</dl>
<dl> <dt><code>options</code></dt> <dd>An <code><a class="internal" title="En/NsIDOMGeoPositionOptions" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoPositionOptions">nsIDOMGeoPositionOptions</a></code> object specifying options; this parameter is optional.</dd>
</dl>
</div><div id="section_11"><span id="Return_value"></span><h6 class="editable">Return value</h6>
<p>An ID&nbsp;number that can be used to reference the watcher in the future when calling <code><a title="en/nsIDOMGeolocation#clearWatch()" class="internal" rel="internal" href="https://developer.mozilla.org/en/XPCOM_Interface_Reference/NsIDOMGeoGeolocation#clearWatch()">clearWatch()</a></code>.</p>
</div>
    */
  int watchPosition(PositionCallback successCallback, PositionErrorCallback errorCallback);
}
