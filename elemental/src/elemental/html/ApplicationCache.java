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
  * 
  */
public interface ApplicationCache extends EventTarget {

    static final int CHECKING = 2;

    static final int DOWNLOADING = 3;

    static final int IDLE = 1;

    static final int OBSOLETE = 5;

    static final int UNCACHED = 0;

    static final int UPDATEREADY = 4;

  EventListener getOncached();

  void setOncached(EventListener arg);

  EventListener getOnchecking();

  void setOnchecking(EventListener arg);

  EventListener getOndownloading();

  void setOndownloading(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnnoupdate();

  void setOnnoupdate(EventListener arg);

  EventListener getOnobsolete();

  void setOnobsolete(EventListener arg);

  EventListener getOnprogress();

  void setOnprogress(EventListener arg);

  EventListener getOnupdateready();

  void setOnupdateready(EventListener arg);

  int getStatus();

  void abort();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  void swapCache();

  void update();
}
