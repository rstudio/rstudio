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

import com.google.doctool.Booklet;
import com.google.doctool.LinkResolver;
import com.google.doctool.LinkResolver.ExtraClassResolver;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

/**
 * A taglet for slurping examples into javadoc output.
 */
public class ExampleTaglet implements Taglet {

  public static void register(Map tagletMap) {
    ExampleTaglet tag = new ExampleTaglet();
    Taglet t = (Taglet) tagletMap.get(tag.getName());
    if (t != null) {
      tagletMap.remove(tag.getName());
    }
    tagletMap.put(tag.getName(), tag);
  }

  public String getName() {
    return "example";
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
    return true;
  }

  public String toString(Tag tag) {
    SourcePosition position = LinkResolver.resolveLink(tag,
        new ExtraClassResolver() {
          public ClassDoc findClass(String className) {
            return GWTJavaDoclet.root.classNamed(className);
          }
        });

    String slurpSource = Booklet.slurpSource(position);
    // The <pre> tag still requires '<' and '>' characters to be escaped
    slurpSource = slurpSource.replace("<", "&lt;");
    slurpSource = slurpSource.replace(">", "&gt;");
    return "<blockquote><pre>" + slurpSource + "</pre></blockquote>";
  }

  public String toString(Tag[] tags) {
    if (tags == null || tags.length == 0) {
      return null;
    }
    String result = "";
    for (int i = 0; i < tags.length; i++) {
      result += toString(tags[i]);
    }
    return result;
  }

}
