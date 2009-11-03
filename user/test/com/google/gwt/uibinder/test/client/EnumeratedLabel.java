package com.google.gwt.uibinder.test.client;

import com.google.gwt.user.client.ui.Label;

public class EnumeratedLabel extends Label {
  public enum Suffix { ending, suffix, tail}

  private Suffix suffix = Suffix.ending;
  private String value = "";
  
  @Override
  public void setText(String text) {
    this.value = text;
    update();
  }
  
  public void setSuffix(Suffix suffix) {
    this.suffix = suffix;
    update();
  }

  private void update() {
    super.setText(value + ": " + suffix);
  }
}
