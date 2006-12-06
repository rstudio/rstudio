package com.google.doctool.custom;

import com.sun.tools.doclets.Taglet;
import com.sun.tools.doclets.standard.tags.SimpleTaglet;

import java.util.Map;

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
