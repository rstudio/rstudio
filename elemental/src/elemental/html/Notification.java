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
  * <div class="geckoMinversionHeaderTemplate"><p>Mobile Only in Gecko 2.0</p><p>Available only in Firefox Mobile as of Gecko 2.0 (Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
</p></div>

<div><p>Non-standard</p></div><p></p>
<p>The notification object, which you create using the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/navigator.mozNotification">navigator.mozNotification</a></code>
&nbsp;object's <code>createNotification()</code>&nbsp;method, is used to configure and display desktop notifications to the user.</p>
  */
public interface Notification extends EventTarget {

  String getDir();

  void setDir(String arg);


  /**
    * &nbsp;A function to call when the notification is clicked.
    */
  EventListener getOnclick();

  void setOnclick(EventListener arg);


  /**
    * &nbsp;A function to call when the notification is dismissed.
    */
  EventListener getOnclose();

  void setOnclose(EventListener arg);

  EventListener getOndisplay();

  void setOndisplay(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnshow();

  void setOnshow(EventListener arg);

  String getReplaceId();

  void setReplaceId(String arg);

  String getTag();

  void setTag(String arg);

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  void cancel();

  void close();

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);


  /**
    * Displays the notification.
    */
  void show();
}
