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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.adapters.SimpleEditor;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.requestfactory.shared.ProxyRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SimpleBarProxy;
import com.google.gwt.requestfactory.shared.SimpleFooProxy;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.Violation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Integration test of the Editor framework. Only tests for
 * RequestFactory-specific features belong here; all other tests should use the
 * SimpleBeanEditorDriver to make the tests simpler.
 */
public class EditorTest extends GWTTestCase {
  static class SimpleBarEditor implements Editor<SimpleBarProxy> {
    protected final SimpleEditor<String> userName = SimpleEditor.of();
  }

  interface SimpleFooDriver extends
      RequestFactoryEditorDriver<SimpleFooProxy, SimpleFooEditor> {
  }

  static class SimpleFooEditor implements HasEditorErrors<SimpleFooProxy> {
    /**
     * Test field-based access.
     */
    final SimpleEditor<String> userName = SimpleEditor.of();

    /**
     * Test nested path access.
     */
    @Path("barField.userName")
    final SimpleEditor<String> barName = SimpleEditor.of();

    private final SimpleBarEditor barEditor = new SimpleBarEditor();

    List<EditorError> errors;

    public void showErrors(List<EditorError> errors) {
      this.errors = errors;
    }

    /**
     * Test method-based access with path override.
     */
    @Path("barField")
    SimpleBarEditor barEditor() {
      return barEditor;
    }
  }

  static class SimpleFooEditorWithDelegate extends SimpleFooEditor implements
      HasEditorDelegate<SimpleFooProxy> {
    EditorDelegate<SimpleFooProxy> delegate;

    public void setDelegate(EditorDelegate<SimpleFooProxy> delegate) {
      this.delegate = delegate;
    }
  }

  private EventBus eventBus;
  private SimpleRequestFactory factory;

  private static final int TEST_TIMEOUT = 5000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.requestfactory.RequestFactorySuite";
  }

  @Override
  public void gwtSetUp() {
    factory = GWT.create(SimpleRequestFactory.class);
    eventBus = new SimpleEventBus();
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

  public void test() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditor editor = new SimpleFooEditor();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(eventBus, factory, editor);

    factory.simpleFooRequest().findSimpleFooById(0L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResults) {
            driver.edit(response,
                factory.simpleFooRequest().persistAndReturnSelf(response).with(
                    driver.getPaths()));

            assertEquals("GWT", editor.userName.getValue());
            assertEquals("FOO", editor.barEditor().userName.getValue());
            assertEquals("FOO", editor.barName.getValue());
            editor.userName.setValue("EditorFooTest");
            // When there are duplicate paths, last declared editor wins
            editor.barEditor().userName.setValue("EditorBarTest");
            editor.barName.setValue("ignored");
            driver.<SimpleFooProxy> flush().fire(
                new Receiver<SimpleFooProxy>() {
                  public void onSuccess(SimpleFooProxy response,
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

  public void testSubscription() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditorWithDelegate editor = new SimpleFooEditorWithDelegate();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(eventBus, factory, editor);

    assertEquals(Arrays.asList("barField.userName", "barField"),
        Arrays.asList(driver.getPaths()));

    factory.simpleFooRequest().findSimpleFooById(0L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResults) {
            // Set up driver in read-only mode
            driver.edit(response, null);
            assertNotNull(editor.delegate.subscribe());

            // Simulate edits occurring elsewhere in the module
            ProxyRequest<SimpleFooProxy> request = factory.simpleFooRequest().persistAndReturnSelf(
                response);
            SimpleBarProxy newBar = factory.create(SimpleBarProxy.class);
            newBar = request.edit(newBar);
            newBar.setUserName("newBar");
            response = request.edit(response);
            response.setBarField(newBar);
            response.setUserName("updated");

            request.fire(new Receiver<SimpleFooProxy>() {
              public void onSuccess(SimpleFooProxy response,
                  Set<SyncResult> syncResults) {
                // EventBus notifications occurr after the onSuccess()
                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                  public void execute() {
                    assertEquals("updated", editor.userName.getValue());
                    assertEquals("newBar",
                        editor.barEditor().userName.getValue());
                    finishTest();
                  }
                });
              }
            });
          }
        });
  }

  public void testViolations() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditor editor = new SimpleFooEditor();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(eventBus, factory, editor);

    factory.simpleFooRequest().findSimpleFooById(0L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooProxy>() {
          public void onSuccess(SimpleFooProxy response,
              Set<SyncResult> syncResults) {
            driver.edit(response,
                factory.simpleFooRequest().persistAndReturnSelf(response).with(
                    driver.getPaths()));
            // Set to an illegal value
            editor.userName.setValue("");

            driver.<SimpleFooProxy> flush().fire(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response,
                      Set<SyncResult> syncResults) {
                    fail("Expected errors");
                  }

                  @Override
                  public void onViolation(Set<Violation> errors) {
                    assertEquals(1, errors.size());
                    Violation v = errors.iterator().next();

                    driver.setViolations(errors);
                    assertEquals(1, editor.errors.size());
                    EditorError error = editor.errors.get(0);
                    assertEquals("userName", error.getAbsolutePath());
                    assertSame(editor.userName, error.getEditor());
                    assertTrue(error.getMessage().length() > 0);
                    assertEquals("userName", error.getPath());
                    assertSame(v, error.getUserData());
                    assertNull(error.getValue());
                    finishTest();
                  }
                });
          }
        });
  }
}
