package com.google.doctool.custom;

import com.google.doctool.ResourceIncluder;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

public class IncludeTaglet implements Taglet {

  public static void register(Map tagletMap) {
    IncludeTaglet tag = new IncludeTaglet();
    Taglet t = (Taglet) tagletMap.get(tag.getName());
    if (t != null) {
      tagletMap.remove(tag.getName());
    }
    tagletMap.put(tag.getName(), tag);
  }

  public String getName() {
    return "gwt.include";
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
    String contents = ResourceIncluder.getResourceFromClasspathScrubbedForHTML(tag);
    return "<blockquote><pre>" + contents + "</pre></blockquote>";
  }

  public String toString(Tag[] tags) {
    return null;
  }

}
