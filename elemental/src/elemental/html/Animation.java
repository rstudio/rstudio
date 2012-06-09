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
public interface Animation {

    static final int DIRECTION_ALTERNATE = 1;

    static final int DIRECTION_NORMAL = 0;

    static final int FILL_BACKWARDS = 1;

    static final int FILL_BOTH = 3;

    static final int FILL_FORWARDS = 2;

    static final int FILL_NONE = 0;

  double getDelay();

  int getDirection();

  double getDuration();

  double getElapsedTime();

  void setElapsedTime(double arg);

  boolean isEnded();

  int getFillMode();

  int getIterationCount();

  String getName();

  boolean isPaused();

  void pause();

  void play();
}
