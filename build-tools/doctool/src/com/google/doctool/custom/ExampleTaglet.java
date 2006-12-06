package com.google.doctool.custom;

import com.google.doctool.Booklet;
import com.google.doctool.LinkResolver;
import com.google.doctool.LinkResolver.ExtraClassResolver;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.SourcePosition;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

import java.util.Map;

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
    return "<blockquote><pre>" + slurpSource + "</pre></blockquote>";
  }

  public String toString(Tag[] tags) {
    return null;
  }

}
