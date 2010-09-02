/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.adapters.StringEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SimpleBarRecord;
import com.google.gwt.requestfactory.shared.SimpleFooRecord;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;

import java.util.Arrays;
import java.util.Set;

/**
 * Integration test of the Editor framework.
 */
public class EditorTest extends GWTTestCase {
  interface SimpleFooDriver extends
      RequestFactoryEditorDriver<SimpleFooRecord, SimpleFooEditor> {
  }

  static class SimpleBarEditor implements Editor<SimpleBarRecord> {
    protected final StringEditor userName = StringEditor.of();
  }

  static class SimpleFooEditor implements Editor<SimpleFooRecord> {
    /**
     * Test field-based access.
     */
    final StringEditor userName = StringEditor.of();

    /**
     * Test nested path access.
     */
    @Path("barField.userName")
    final StringEditor barName = StringEditor.of();

    private final SimpleBarEditor barEditor = new SimpleBarEditor();

    /**
     * Test method-based access with path override.
     */
    @Path("barField")
    SimpleBarEditor barEditor() {
      return barEditor;
    }
  }

  private EventBus eventBus;
  private SimpleRequestFactory factory;

  @Override
  public void gwtSetUp() {
    factory = GWT.create(SimpleRequestFactory.class);
    eventBus = new HandlerManager(null);
    factory.init(eventBus);
  }

  @Override
  public void gwtTearDown() {
    factory.simpleFooRequest().reset().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
    factory.simpleBarRequest().reset().fire(new Receiver<Void>() {
      public void onSuccess(Void response, Set<SyncResult> syncResults) {
      }
    });
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  private static final int TEST_TIMEOUT = 5000;

  public void test() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditor editor = new SimpleFooEditor();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(eventBus, factory, editor);

    assertEquals(Arrays.asList("barField.userName", "barField"),
        Arrays.asList(driver.getPaths()));

    factory.simpleFooRequest().findSimpleFooById(0L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooRecord>() {
          public void onSuccess(SimpleFooRecord response,
              Set<SyncResult> syncResults) {
            driver.edit(response,
                factory.simpleFooRequest().persistAndReturnSelf(response).with(
                    driver.getPaths()));
            assertEquals("GWT", editor.userName.getValue());
            assertEquals("FOO", editor.barEditor().userName.getValue());
            assertEquals("FOO", editor.barName.getValue());
            editor.userName.setValue("EditorFooTest");
            // When there are duplicate paths, outermost editor wins
            editor.barEditor().userName.setValue("ignored");
            editor.barName.setValue("EditorBarTest");
            driver.<SimpleFooRecord> flush().fire(
                new Receiver<SimpleFooRecord>() {
                  public void onSuccess(SimpleFooRecord response,
                      Set<SyncResult> syncResults) {
                    assertEquals("EditorFooTest", response.getUserName());
                    assertEquals("EditorBarTest",
                        response.getBarField().getUserName());
                    finishTest();
                  }
                });
          }
        });
  }
}
