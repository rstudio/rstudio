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
package com.google.web.bindery.requestfactory.gwt.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDelegate;
import com.google.gwt.editor.client.EditorError;
import com.google.gwt.editor.client.HasEditorDelegate;
import com.google.gwt.editor.client.HasEditorErrors;
import com.google.gwt.editor.client.adapters.EditorSource;
import com.google.gwt.editor.client.adapters.ListEditor;
import com.google.gwt.editor.client.adapters.SimpleEditor;
import com.google.web.bindery.requestfactory.gwt.client.HasRequestContext;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryTestBase;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.SimpleBarProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooProxy;
import com.google.web.bindery.requestfactory.shared.SimpleFooRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Integration test of the Editor framework. Only tests for
 * RequestFactory-specific features belong here; all other tests should use the
 * SimpleBeanEditorDriver to make the tests simpler.
 */
public class EditorTest extends RequestFactoryTestBase {
  /*
   * DO NOT USE finishTest(). Instead, call finishTestAndReset();
   */

  static class SimpleBarEditor implements Editor<SimpleBarProxy>, HasRequestContext<SimpleBarProxy> {
    protected final SimpleEditor<String> userName = SimpleEditor.of();
    RequestContext ctx;

    public void setRequestContext(RequestContext ctx) {
      this.ctx = ctx;
    }
  }

  static class SimpleFooBarNameOnlyEditor implements HasRequestContext<SimpleFooProxy> {
    RequestContext ctx;

    /**
     * Test nested path access.
     */
    @Path("barField.userName")
    final SimpleEditor<String> barName = SimpleEditor.of();

    public void setRequestContext(RequestContext ctx) {
      this.ctx = ctx;
    }
  }

  interface SimpleFooDriver extends RequestFactoryEditorDriver<SimpleFooProxy, SimpleFooEditor> {
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

  static class SimpleFooEditorWithList implements Editor<SimpleFooProxy> {

    final SimpleEditor<String> userName = SimpleEditor.of();

    final ListEditor<SimpleFooProxy, SimpleFooBarNameOnlyEditor> selfOneToManyField = ListEditor
        .of(new EditorSource<SimpleFooBarNameOnlyEditor>() {
          @Override
          public SimpleFooBarNameOnlyEditor create(int index) {
            return new SimpleFooBarNameOnlyEditor();
          }
        });
  }

  interface SimpleFooEditorWithListDriver extends
      RequestFactoryEditorDriver<SimpleFooProxy, SimpleFooEditorWithList> {
  }

  private static final int TEST_TIMEOUT = 5000;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.requestfactory.gwt.RequestFactorySuite";
  }

  public void test() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditor editor = new SimpleFooEditor();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(req, editor);
    final String[] paths = driver.getPaths();
    assertEquals(Arrays.asList("barField"), Arrays.asList(paths));

    req.simpleFooRequest().findSimpleFooById(1L).with(paths).fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(SimpleFooProxy response) {

        SimpleFooRequest context = req.simpleFooRequest();
        driver.edit(response, context);
        assertSame(context, editor.barEditor().ctx);
        context.persistAndReturnSelf().using(response).with(paths).to(
            new Receiver<SimpleFooProxy>() {
              @Override
              public void onSuccess(SimpleFooProxy response) {
                assertEquals("EditorFooTest", response.getUserName());
                assertEquals("EditorBarTest", response.getBarField().getUserName());
                finishTestAndReset();
              }
            });
        assertEquals("GWT", editor.userName.getValue());
        assertEquals("FOO", editor.barEditor().userName.getValue());
        assertEquals("FOO", editor.barName.getValue());
        editor.userName.setValue("EditorFooTest");
        // When there are duplicate paths, last declared editor wins
        editor.barEditor().userName.setValue("EditorBarTest");
        editor.barName.setValue("ignored");
        driver.flush().fire();
      }
    });
  }

  public void testConstraintViolations() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditor editor = new SimpleFooEditor();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(req, editor);

    req.simpleFooRequest().findSimpleFooById(1L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {

            SimpleFooRequest context = req.simpleFooRequest();
            driver.edit(response, context);
            context.persistAndReturnSelf().using(response).with(driver.getPaths()).to(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onConstraintViolation(Set<ConstraintViolation<?>> errors) {
                    assertEquals(1, errors.size());
                    ConstraintViolation<?> v = errors.iterator().next();

                    driver.setConstraintViolations(errors);
                    assertEquals(1, editor.errors.size());
                    EditorError error = editor.errors.get(0);
                    assertEquals("userName", error.getAbsolutePath());
                    assertSame(editor.userName, error.getEditor());
                    assertTrue(error.getMessage().length() > 0);
                    assertEquals("userName", error.getPath());
                    assertSame(v, error.getUserData());
                    assertNull(error.getValue());
                    finishTestAndReset();
                  }

                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    fail("Expected errors. You may be missing jars, see "
                        + "the comment in RequestFactoryTest.ShouldNotSucceedReceiver.onSuccess");
                  }
                });
            // Set to an illegal value
            editor.userName.setValue("");

            driver.flush().fire();
          }
        });
  }

  /**
   * Tests issues with {@code CompositeEditor}s when subeditors are dynamically
   * created, such as with a {@link ListEditor}.
   * 
   * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=6081
   */
  public void testList() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditorWithList editor = new SimpleFooEditorWithList();

    final SimpleFooEditorWithListDriver driver = GWT.create(SimpleFooEditorWithListDriver.class);
    driver.initialize(req, editor);

    final String[] paths = driver.getPaths();
    assertEquals(Arrays.asList("selfOneToManyField", "selfOneToManyField.barField"), Arrays
        .asList(paths));

    req.simpleFooRequest().getSimpleFooWithSubPropertyCollection().with(paths).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {

            SimpleFooRequest context = req.simpleFooRequest();
            driver.edit(response, context);

            SimpleFooBarNameOnlyEditor subeditor = editor.selfOneToManyField.getEditors().get(0);
            // test context is correctly set in CompositeEditor subeditors
            assertSame(context, subeditor.ctx);

            context.persistAndReturnSelf().using(response).with(paths).to(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    assertEquals("EditorBarTest", response.getSelfOneToManyField().get(0)
                        .getBarField().getUserName());
                    finishTestAndReset();
                  }
                });
            assertEquals("FOO", subeditor.barName.getValue());

            subeditor.barName.setValue("EditorBarTest");

            driver.flush().fire();
          }
        });
  }

  public void testNoSubscription() {
    final SimpleFooEditorWithDelegate editor = new SimpleFooEditorWithDelegate();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(req, editor);

    /*
     * Confirm that it's always safe to call subscribe. The editor's delegate
     * isn't set until edit is called, so edit nothing.
     */
    driver.edit(null, null);
    assertNull(editor.delegate.subscribe());
  }

  /**
   * Tests the editor can be re-used while the initial context is locked and
   * therefore its attached proxies are frozen..
   * 
   * @see http://code.google.com/p/google-web-toolkit/issues/detail?id=5752
   */
  public void testReuse() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditorWithList editor = new SimpleFooEditorWithList();

    final SimpleFooEditorWithListDriver driver = GWT.create(SimpleFooEditorWithListDriver.class);
    driver.initialize(req, editor);

    req.simpleFooRequest().findSimpleFooById(1L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {

            SimpleFooRequest context = req.simpleFooRequest();
            driver.edit(response, context);
            editor.userName.setValue("One");
            context.persistAndReturnSelf().using(response).with(driver.getPaths()).to(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    assertEquals("One", response.getUserName());
                    // just testing that it doesn't throw (see issue 5752)
                    driver.edit(response, req.simpleFooRequest());
                    editor.userName.setValue("Two");
                    driver.flush();
                    finishTestAndReset();
                  }
                });
            // The fire() will freeze the proxies and lock the context
            driver.flush().fire();
          }
        });
  }

  public void testSubscription() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditorWithDelegate editor = new SimpleFooEditorWithDelegate();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(req, editor);

    String[] paths = driver.getPaths();
    assertEquals(Arrays.asList("barField"), Arrays.asList(paths));

    req.simpleFooRequest().findSimpleFooById(1L).with(paths).fire(new Receiver<SimpleFooProxy>() {
      @Override
      public void onSuccess(SimpleFooProxy response) {
        // Set up driver in read-only mode
        driver.edit(response, null);
        assertNotNull(editor.delegate.subscribe());

        // Simulate edits occurring elsewhere in the module
        SimpleFooRequest context = req.simpleFooRequest();
        Request<SimpleFooProxy> request = context.persistAndReturnSelf().using(response);
        SimpleBarProxy newBar = context.create(SimpleBarProxy.class);
        newBar = context.edit(newBar);
        newBar.setUserName("newBar");
        response = context.edit(response);
        response.setBarField(newBar);
        response.setUserName("updated");

        request.fire(new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {
            // EventBus notifications occur after the onSuccess()
            Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
              public boolean execute() {
                if ("updated".equals(editor.userName.getValue())) {
                  assertEquals("updated", editor.userName.getValue());
                  assertEquals("newBar", editor.barEditor().userName.getValue());
                  finishTestAndReset();
                  return false;
                }
                return true;
              }
            }, 50);
          }
        });
      }
    });
  }

  public void testViolations() {
    delayTestFinish(TEST_TIMEOUT);
    final SimpleFooEditor editor = new SimpleFooEditor();

    final SimpleFooDriver driver = GWT.create(SimpleFooDriver.class);
    driver.initialize(req, editor);

    req.simpleFooRequest().findSimpleFooById(1L).with(driver.getPaths()).fire(
        new Receiver<SimpleFooProxy>() {
          @Override
          public void onSuccess(SimpleFooProxy response) {

            SimpleFooRequest context = req.simpleFooRequest();
            driver.edit(response, context);
            context.persistAndReturnSelf().using(response).with(driver.getPaths()).to(
                new Receiver<SimpleFooProxy>() {
                  @Override
                  public void onSuccess(SimpleFooProxy response) {
                    fail("Expected errors. You may be missing jars, see "
                        + "the comment in RequestFactoryTest.ShouldNotSucceedReceiver.onSuccess");
                  }

                  @SuppressWarnings("deprecation")
                  @Override
                  public void onViolation(
                      Set<com.google.web.bindery.requestfactory.shared.Violation> errors) {
                    assertEquals(1, errors.size());
                    com.google.web.bindery.requestfactory.shared.Violation v =
                        errors.iterator().next();

                    driver.setViolations(errors);
                    assertEquals(1, editor.errors.size());
                    EditorError error = editor.errors.get(0);
                    assertEquals("userName", error.getAbsolutePath());
                    assertSame(editor.userName, error.getEditor());
                    assertTrue(error.getMessage().length() > 0);
                    assertEquals("userName", error.getPath());
                    assertSame(v, error.getUserData());
                    assertNull(error.getValue());
                    finishTestAndReset();
                  }
                });
            // Set to an illegal value
            editor.userName.setValue("");

            driver.flush().fire();
          }
        });
  }
}
