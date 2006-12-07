/*
 * Copyright 2006 Google Inc.
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
package com.google.doctool.custom;

import com.sun.tools.doclets.Taglet;
import com.sun.tools.doclets.standard.tags.SimpleTaglet;

import java.util.Map;

/**
 * A taglet for including GWT tip tags in javadoc output.
 */
public class TipTaglet extends SimpleTaglet {

  public static void register(Map tagletMap) {
    TipTaglet tag = new TipTaglet();
    Taglet t = (Taglet) tagletMap.get(tag.getName());
    if (t != null) {
      tagletMap.remove(tag.getName());
    }
    tagletMap.put(tag.getName(), tag);
  }

  public TipTaglet() {
    super("tip", "Tip:", "a");
  }

}
