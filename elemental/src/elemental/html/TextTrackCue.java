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
import elemental.dom.DocumentFragment;
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
public interface TextTrackCue extends EventTarget {

  String getAlign();

  void setAlign(String arg);

  double getEndTime();

  void setEndTime(double arg);

  String getId();

  void setId(String arg);

  int getLine();

  void setLine(int arg);

  EventListener getOnenter();

  void setOnenter(EventListener arg);

  EventListener getOnexit();

  void setOnexit(EventListener arg);

  boolean isPauseOnExit();

  void setPauseOnExit(boolean arg);

  int getPosition();

  void setPosition(int arg);

  int getSize();

  void setSize(int arg);

  boolean isSnapToLines();

  void setSnapToLines(boolean arg);

  double getStartTime();

  void setStartTime(double arg);

  String getText();

  void setText(String arg);

  TextTrack getTrack();

  String getVertical();

  void setVertical(String arg);

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);

  DocumentFragment getCueAsHTML();

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);
}
