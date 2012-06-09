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
import elemental.html.Window;

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
public interface WheelEvent extends UIEvent {

  boolean isAltKey();

  int getClientX();

  int getClientY();

  boolean isCtrlKey();

  boolean isMetaKey();

  int getOffsetX();

  int getOffsetY();

  int getScreenX();

  int getScreenY();

  boolean isShiftKey();

  boolean isWebkitDirectionInvertedFromDevice();

  int getWheelDelta();

  int getWheelDeltaX();

  int getWheelDeltaY();

  int getX();

  int getY();

  void initWebKitWheelEvent(int wheelDeltaX, int wheelDeltaY, Window view, int screenX, int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey);
}
