/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.i18n.client;

import java.util.Map;

/**
 * Testing class to represent Moods.
 */
public interface Moods extends Constants {
  /**
   * The word for 'Happy'.
   * 
   * @return 'happy'
   */
  @Key("123")
  String getHappy();

  /**
   * Convenience method to get all key/value pairs associated with the mood
   * array.
   * 
   * @return returnType of moods
   */
  @Key("moods")
  Map moodMap();

  /**
   * Gets the keys associated with moods. However note that this will not
   * display well as the values are "Sad", "123".
   * 
   * @return array of moods
   */
  @Key("moods")
  String[] moodArray();
}
