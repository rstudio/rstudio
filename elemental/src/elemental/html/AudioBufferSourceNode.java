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
public interface AudioBufferSourceNode extends AudioSourceNode {

    static final int FINISHED_STATE = 3;

    static final int PLAYING_STATE = 2;

    static final int SCHEDULED_STATE = 1;

    static final int UNSCHEDULED_STATE = 0;

  AudioBuffer getBuffer();

  void setBuffer(AudioBuffer arg);

  AudioGain getGain();

  boolean isLoop();

  void setLoop(boolean arg);

  boolean isLooping();

  void setLooping(boolean arg);

  AudioParam getPlaybackRate();

  int getPlaybackState();

  void noteGrainOn(double when, double grainOffset, double grainDuration);

  void noteOff(double when);

  void noteOn(double when);
}
