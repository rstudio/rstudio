package com.google.gwt.museum.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * A repository for demonstrating bugs we once faced.
 * 
 * TODO(bruce): make this more like the original bug sink idea. For now, it's
 * just a hacked together set of examples based on past issues.
 */
public class Museum implements EntryPoint {

  public void onModuleLoad() {
    Panel p = RootPanel.get();
    new Issue2290(p);
    new Issue2307(p);
  }
}
