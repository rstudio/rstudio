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
import elemental.events.EventTarget;
import elemental.dom.MediaStreamList;
import elemental.util.Mappable;
import elemental.events.EventListener;
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
public interface PeerConnection00 extends EventTarget {

    static final int ACTIVE = 2;

    static final int CLOSED = 3;

    static final int ICE_CHECKING = 0x300;

    static final int ICE_CLOSED = 0x700;

    static final int ICE_COMPLETED = 0x500;

    static final int ICE_CONNECTED = 0x400;

    static final int ICE_FAILED = 0x600;

    static final int ICE_GATHERING = 0x100;

    static final int ICE_WAITING = 0x200;

    static final int NEW = 0;

    static final int OPENING = 1;

    static final int SDP_ANSWER = 0x300;

    static final int SDP_OFFER = 0x100;

    static final int SDP_PRANSWER = 0x200;

  int getIceState();

  SessionDescription getLocalDescription();

  MediaStreamList getLocalStreams();

  EventListener getOnaddstream();

  void setOnaddstream(EventListener arg);

  EventListener getOnconnecting();

  void setOnconnecting(EventListener arg);

  EventListener getOnopen();

  void setOnopen(EventListener arg);

  EventListener getOnremovestream();

  void setOnremovestream(EventListener arg);

  EventListener getOnstatechange();

  void setOnstatechange(EventListener arg);

  int getReadyState();

  SessionDescription getRemoteDescription();

  MediaStreamList getRemoteStreams();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  void addStream(MediaStream stream);

  void addStream(MediaStream stream, Mappable mediaStreamHints);

  void close();

  SessionDescription createAnswer(String offer);

  SessionDescription createAnswer(String offer, Mappable mediaHints);

  SessionDescription createOffer();

  SessionDescription createOffer(Mappable mediaHints);

  boolean dispatchEvent(Event event);

  void processIceMessage(IceCandidate candidate);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  void removeStream(MediaStream stream);

  void startIce();

  void startIce(Mappable iceOptions);
}
