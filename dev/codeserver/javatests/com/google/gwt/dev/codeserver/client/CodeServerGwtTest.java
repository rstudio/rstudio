/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.codeserver.client;

import com.google.gwt.core.client.ScriptInjector;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

/**
 * A GwtTestCase for the JavaScript that is part of super dev mode.
 *
 * Since inside of the GWT SDK there is not a lot of JavaScript it does not make sense to add a
 * extra testing framework for JavaScript. Rather this class bundles the JavaScript that should
 * be tested as a TextResource and injects it into the current page. This way we can write test
 * against it using JSNI.
 */
public class CodeServerGwtTest extends GWTTestCase {

  interface Resource extends ClientBundle {
    @Source("com/google/gwt/dev/codeserver/recompile_lib.js")
    TextResource libJS();
  }

  private boolean injected;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.codeserver.CodeServerTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    ensureJsInjected();
  }

  public native void testPropertyHelper_withProperInput() /*-{
    // setup property providers and values for the test
    var mocks = (function(){
      var propProviders = {};
      var propValues = {};
      propProviders['prop1'] = function(){
        return 'val1_1';
      };

      propValues['prop1'] = {'val1_1':0,'val1_2':1,'val1_3':2};

      propProviders['prop2'] = function(){
        return 'val2_2';
      };

      propValues['prop2'] = {'val2_1':0,'val2_2':1,'val2_3':2};
      return {provider: propProviders, values : propValues};
    })();

    // Actual test
    var PropertyHelper = $wnd.namespace.lib.PropertyHelper;
    var propertyHelper = new PropertyHelper('testModule', mocks.provider, mocks.values);
    var result = propertyHelper.computeBindingProperties();
    var assertStringEquals = @CodeServerGwtTest::assertEquals(Ljava/lang/String;Ljava/lang/String;);
    var assertTrue = @CodeServerGwtTest::assertTrue(Ljava/lang/String;Z);

    var length = Object.keys(result).length;
    assertTrue(length == 2, "PropertyHelper did not return two entries: " + length);
    assertStringEquals('val1_1', result['prop1']);
    assertStringEquals('val2_2', result['prop2']);
  }-*/;

  public native void testRecompiler() /*-{
    var Recompiler = $wnd.namespace.lib.Recompiler;
    var recompiler = new Recompiler('testModule', {prop1: 'val1', prop2 : 'val2'});

    var jsonpUrl = '';
    var callbackCalled = false;

    var assertStringEquals = @CodeServerGwtTest::assertEquals(Ljava/lang/String;Ljava/lang/String;);
    var assertTrue = @CodeServerGwtTest::assertTrue(Ljava/lang/String;Z);

    // patch up functions of recompiler that need the actual SDM environment
    recompiler.getCodeServerBaseUrl = function() {
      return "http://mytesthost:7812/";
    };

    recompiler.__jsonp = function(url, callback) {
      jsonpUrl = url;
      callback({status : 'ok'});
    };

    // do the test
    recompiler.compile(function(result) {
      callbackCalled = true;
      //compile is done
      assertStringEquals('ok', result.status);
      assertStringEquals('http://mytesthost:7812/recompile/testModule?prop1=val1&prop2=val2',
          jsonpUrl);
    });
    assertTrue(callbackCalled, 'callback for successful recompile was not executed');
  }-*/;

  private void ensureJsInjected() {
    if(injected) {
      return;
    }
    Resource res = GWT.create(Resource.class);
    String before = "(function(){ \n" +
    		"$wnd.namespace = {};$namespace = $wnd.namespace; $global = $wnd";
    String js = res.libJS().getText();
    ScriptInjector.fromString(before + js + "})()").inject();
  }
}
