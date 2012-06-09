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
import elemental.dom.MediaStream;
import elemental.events.EventListener;
import elemental.dom.MediaStreamList;
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
public interface DeprecatedPeerConnection extends EventTarget {

    static final int ACTIVE = 2;

    static final int CLOSED = 3;

    static final int NEGOTIATING = 1;

    static final int NEW = 0;

  MediaStreamList getLocalStreams();

  EventListener getOnaddstream();

  void setOnaddstream(EventListener arg);

  EventListener getOnconnecting();

  void setOnconnecting(EventListener arg);

  EventListener getOnmessage();

  void setOnmessage(EventListener arg);

  EventListener getOnopen();

  void setOnopen(EventListener arg);

  EventListener getOnremovestream();

  void setOnremovestream(EventListener arg);

  EventListener getOnstatechange();

  void setOnstatechange(EventListener arg);

  int getReadyState();

  MediaStreamList getRemoteStreams();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  void addStream(MediaStream stream);

  void close();

  boolean dispatchEvent(Event event);

  void processSignalingMessage(String message);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  void removeStream(MediaStream stream);

  void send(String text);
}
