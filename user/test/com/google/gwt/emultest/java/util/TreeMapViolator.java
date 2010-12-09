/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.emultest.java.util;

import java.util.Map;

/**
 * Used to delay referencing methods only present in the emulated JRE until they
 * are actually used.
 */
public class TreeMapViolator {
  // Use JSNI to call a special method on our implementation of TreeMap.
  @SuppressWarnings("unchecked") // raw Map
  public static native void callAssertCorrectness(Map map) /*-{
    map.@java.util.TreeMap::assertCorrectness()();
  }-*/;
}
