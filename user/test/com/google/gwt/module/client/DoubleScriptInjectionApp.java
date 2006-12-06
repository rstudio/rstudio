package com.google.gwt.module.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;

public class DoubleScriptInjectionApp implements EntryPoint {

  public void onModuleLoad() {
    Window.alert(SingleScriptInjectionTest.isScriptOneReady());
    Window.alert(DoubleScriptInjectionTest.isScriptTwoReady());
  }

}
