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
public interface MediaController extends EventTarget {

  TimeRanges getBuffered();

  double getCurrentTime();

  void setCurrentTime(double arg);

  double getDefaultPlaybackRate();

  void setDefaultPlaybackRate(double arg);

  double getDuration();

  boolean isMuted();

  void setMuted(boolean arg);

  boolean isPaused();

  double getPlaybackRate();

  void setPlaybackRate(double arg);

  TimeRanges getPlayed();

  TimeRanges getSeekable();

  double getVolume();

  void setVolume(double arg);

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);

  void pause();

  void play();

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
