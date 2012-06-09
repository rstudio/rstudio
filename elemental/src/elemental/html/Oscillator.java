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
public interface Oscillator extends AudioSourceNode {

    static final int CUSTOM = 4;

    static final int FINISHED_STATE = 3;

    static final int PLAYING_STATE = 2;

    static final int SAWTOOTH = 2;

    static final int SCHEDULED_STATE = 1;

    static final int SINE = 0;

    static final int SQUARE = 1;

    static final int TRIANGLE = 3;

    static final int UNSCHEDULED_STATE = 0;

  AudioParam getDetune();

  AudioParam getFrequency();

  int getPlaybackState();

  int getType();

  void setType(int arg);

  void noteOff(double when);

  void noteOn(double when);

  void setWaveTable(WaveTable waveTable);
}
