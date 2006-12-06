// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.gwt.user.client.ui;

public class FlexTableProfile extends HTMLTableProfile {

  public HTMLTable createTable(int rows, int columns) {
    return new FlexTable();
  }

}
