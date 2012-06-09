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
package elemental.dom;

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
public interface SpeechRecognitionError {

    static final int ABORTED = 2;

    static final int AUDIO_CAPTURE = 3;

    static final int BAD_GRAMMAR = 7;

    static final int LANGUAGE_NOT_SUPPORTED = 8;

    static final int NETWORK = 4;

    static final int NOT_ALLOWED = 5;

    static final int NO_SPEECH = 1;

    static final int OTHER = 0;

    static final int SERVICE_NOT_ALLOWED = 6;

  int getCode();

  String getMessage();
}
