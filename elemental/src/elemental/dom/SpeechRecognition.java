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
public interface SpeechRecognition extends EventTarget {

  boolean isContinuous();

  void setContinuous(boolean arg);

  SpeechGrammarList getGrammars();

  void setGrammars(SpeechGrammarList arg);

  String getLang();

  void setLang(String arg);

  EventListener getOnaudioend();

  void setOnaudioend(EventListener arg);

  EventListener getOnaudiostart();

  void setOnaudiostart(EventListener arg);

  EventListener getOnend();

  void setOnend(EventListener arg);

  EventListener getOnerror();

  void setOnerror(EventListener arg);

  EventListener getOnnomatch();

  void setOnnomatch(EventListener arg);

  EventListener getOnresult();

  void setOnresult(EventListener arg);

  EventListener getOnresultdeleted();

  void setOnresultdeleted(EventListener arg);

  EventListener getOnsoundend();

  void setOnsoundend(EventListener arg);

  EventListener getOnsoundstart();

  void setOnsoundstart(EventListener arg);

  EventListener getOnspeechend();

  void setOnspeechend(EventListener arg);

  EventListener getOnspeechstart();

  void setOnspeechstart(EventListener arg);

  EventListener getOnstart();

  void setOnstart(EventListener arg);

  void abort();

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  boolean dispatchEvent(Event evt);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  void start();

  void stop();
}
