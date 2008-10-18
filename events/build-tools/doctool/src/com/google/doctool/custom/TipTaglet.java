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

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

/**
 * A taglet for including GWT tip tags in javadoc output.
 */
public class TipTaglet implements Taglet {

  public static void register(Map tagletMap) {
    TipTaglet tag = new TipTaglet();
    Taglet t = (Taglet) tagletMap.get(tag.getName());
    if (t != null) {
      tagletMap.remove(tag.getName());
    }
    tagletMap.put(tag.getName(), tag);
  }

  public TipTaglet() {
  }

  public String getName() {
    return "tip";
  }

  public boolean inConstructor() {
    return true;
  }

  public boolean inField() {
    return true;
  }

  public boolean inMethod() {
    return true;
  }

  public boolean inOverview() {
    return true;
  }

  public boolean inPackage() {
    return true;
  }

  public boolean inType() {
    return true;
  }

  public boolean isInlineTag() {
    return false;
  }

  public String toString(Tag tag) {
    return null;
  }

  public String toString(Tag[] tags) {
    if (tags == null || tags.length == 0) {
      return null;
    }
    String result = "<DT><B>Tip:</B></DT><DD>";
    if (tags.length == 1) {
      result += tags[0].text();
    } else {
      result += "<UL>";
      for (int i = 0; i < tags.length; i++) {
        result += "<LI>" + tags[i].text() + "</LI>";
      }
      result += "</UL>";
    }
    result += "</DD>";
    return result;
  }

}
