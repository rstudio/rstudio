package com.google.gwt.examples.i18n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

public class MyConstantsExample {

  public void useMyConstants() {
    MyConstants myConstants = (MyConstants) GWT.create(MyConstants.class);
    Window.alert(myConstants.helloWorld());
  }
}
