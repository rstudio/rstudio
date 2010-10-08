/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;

/**
 * Tests the FormPanel.
 *
 * @see com.google.gwt.user.server.ui.FormPanelTestServlet
 */
public class FormPanelTest extends SimplePanelTestBase<FormPanel> {

  /**
   * The maximum amount of time to wait for a test to finish.
   */
  private static final int TEST_DELAY = 15000;

  public static boolean clicked = false;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.FormPanelTest";
  }

  public void testCancelSubmit() {
    TextBox tb = new TextBox();
    tb.setName("q");

    FormPanel form = new FormPanel();
    form.setWidget(tb);
    form.setAction("http://www.google.com/search");

    form.addSubmitHandler(new SubmitHandler() {
      public void onSubmit(SubmitEvent event) {
        event.cancel();
      }
    });
    form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
      public void onSubmitComplete(SubmitCompleteEvent event) {
        fail("Form was canceled and should not have been submitted");
      }
    });

    form.submit();
  }

  public void testFileUpload() {
    final FormPanel form = new FormPanel();
    form.setMethod(FormPanel.METHOD_POST);
    form.setEncoding(FormPanel.ENCODING_MULTIPART);
    assertEquals(FormPanel.ENCODING_MULTIPART, form.getEncoding());
    form.setAction(GWT.getModuleBaseURL() + "formHandler");

    FileUpload file = new FileUpload();
    file.setName("file0");
    form.setWidget(file);

    RootPanel.get().add(form);

    delayTestFinish(TEST_DELAY);
    form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
      public void onSubmitComplete(SubmitCompleteEvent event) {
        // The server just echoes the contents of the request. The following
        // string should have been present in it.
        assertTrue(event.getResults().indexOf(
            "Content-Disposition: form-data; name=\"file0\";") != -1);
        finishTest();
      }
    });
    form.submit();
  }

  /**
   * Tests submitting using url-encoded get, with all form widgets (other than
   * FileUpload, which requires post/multipart.
   */
  public void testMethodGet() {
    final FormPanel form = new FormPanel();
    form.setMethod(FormPanel.METHOD_GET);
    form.setEncoding(FormPanel.ENCODING_URLENCODED);
    form.setAction(GWT.getModuleBaseURL() + "formHandler");

    TextBox tb = new TextBox();
    tb.setText("text");
    tb.setName("tb");

    PasswordTextBox ptb = new PasswordTextBox();
    ptb.setText("password");
    ptb.setName("ptb");

    CheckBox cb0 = new CheckBox(), cb1 = new CheckBox();
    cb1.setValue(true);
    cb0.setName("cb0");
    cb1.setName("cb1");

    RadioButton rb0 = new RadioButton("foo");
    RadioButton rb1 = new RadioButton("foo");
    rb0.setValue(true);
    rb0.setName("rb0");
    rb1.setName("rb1");

    ListBox lb = new ListBox();
    lb.addItem("option0");
    lb.addItem("option1");
    lb.addItem("option2");
    lb.setValue(0, "value0");
    lb.setValue(1, "value1");
    lb.setValue(2, "value2");
    lb.setSelectedIndex(1);
    lb.setName("lb");

    Hidden h = new Hidden("h", "v");

    FlowPanel panel = new FlowPanel();
    panel.add(tb);
    panel.add(ptb);
    panel.add(cb0);
    panel.add(cb1);
    panel.add(rb0);
    panel.add(rb1);
    panel.add(lb);
    panel.add(h);
    form.setWidget(panel);
    RootPanel.get().add(form);

    delayTestFinish(TEST_DELAY);

    form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
      public void onSubmitComplete(SubmitCompleteEvent event) {
        // The server just echoes the query string. This is what it should look
        // like.
        assertTrue(event.getResults().equals(
            "tb=text&amp;ptb=password&amp;cb1=on&amp;rb0=on&amp;lb=value1&amp;h=v"));
        finishTest();
      }
    });

    form.submit();
  }

  public void testNamedTargetSubmitEvent() {
    // Create a form and frame in the document we can wrap.
    String iframeId = Document.get().createUniqueId();
    String iframeName = Document.get().createUniqueId();
    final Element container = Document.get().createDivElement();
    container.setInnerHTML(
        "<form method='post' target='" + iframeName + "' action='"
            + GWT.getModuleBaseURL()
            + "formHandler?sendHappyHtml'>"
            + "<input type='submit' id='submitBtn'></input></form>"
            + "<iframe src=\"javascript:\'\'\" id='" + iframeId + "' "
            + "name='" + iframeName + "'></iframe>");
    Document.get().getBody().appendChild(container);

    // Wrap the form and make sure its target frame is intact.
    FormPanel form = FormPanel.wrap(container.getFirstChildElement());
    assertEquals(iframeName, form.getTarget());

    // Ensure that no synthesized iframe was created.
    assertNull(form.getSynthesizedIFrame());

    // Submit the form using the submit button and make sure the submit event fires.
    delayTestFinish(TEST_DELAY);
    form.addSubmitHandler(new SubmitHandler() {
      public void onSubmit(SubmitEvent event) {
        finishTest();
      }
    });

    Document.get().getElementById("submitBtn").<InputElement>cast().click();
  }

  public void testReset() {
    FormPanel form = new FormPanel();
    RootPanel.get().add(form);
    TextBox textBox = new TextBox();
    textBox.setText("Hello World");
    form.setWidget(textBox);
    assertEquals("Hello World", textBox.getText());
    form.reset();
    assertEquals("", textBox.getText());
    RootPanel.get().remove(form);
  }

  public void testSubmitAndHideDialog() {
    final FormPanel form = new FormPanel();
    form.setMethod(FormPanel.METHOD_GET);
    form.setEncoding(FormPanel.ENCODING_URLENCODED);
    form.setAction(GWT.getModuleBaseURL() + "formHandler");

    TextBox tb = new TextBox();
    form.add(tb);
    tb.setText("text");
    tb.setName("tb");

    final DialogBox dlg = new DialogBox();
    dlg.setWidget(form);
    dlg.show();

    delayTestFinish(TEST_DELAY);
    form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
      public void onSubmitComplete(SubmitCompleteEvent event) {
        // Make sure we get our results back.
        assertTrue(event.getResults().equals("tb=text"));
        finishTest();

        // Hide the dialog on submit complete. This was causing problems at one
        // point because hiding the dialog detached the form and iframe.
        dlg.hide();
      }
    });

    form.submit();
  }

  /**
   * Tests submitting an alternate frame.
   * TODO: Investigate intermittent failures with HtmlUnit.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  public void testSubmitFrame() {
    final NamedFrame frame = new NamedFrame("myFrame");
    FormPanel form = new FormPanel(frame);
    form.setMethod(FormPanel.METHOD_POST);
    form.setAction(GWT.getModuleBaseURL() + "formHandler?sendHappyHtml");
    RootPanel.get().add(form);
    RootPanel.get().add(frame);

    delayTestFinish(TEST_DELAY);
    form.submit();
    Timer t = new Timer() {
      @Override
      public void run() {
        // Make sure the frame got the contents we expected.
        assertTrue(isHappyDivPresent(frame.getElement()));
        finishTest();
      }
    };

    // Wait 5 seconds before checking the results.
    t.schedule(TEST_DELAY - 2000);
  }

  public void testWrappedForm() {
    // Create a form and frame in the document we can wrap.
    final String iframeId = Document.get().createUniqueId();
    final String iframeName = Document.get().createUniqueId();
    final Element container = Document.get().createDivElement();
    container.setInnerHTML(
        "<form method='post' target='" + iframeName + "' action='"
            + GWT.getModuleBaseURL()
            + "formHandler?sendHappyHtml'>"
            + "<input type='hidden' name='foo' value='bar'></input></form>"
            + "<iframe src=\"javascript:\'\'\" id='" + iframeId + "' "
            + "name='" + iframeName + "'></iframe>");
    Document.get().getBody().appendChild(container);

    // Wrap the form and make sure its target frame is intact.
    FormPanel form = FormPanel.wrap(container.getFirstChildElement());
    assertEquals(iframeName, form.getTarget());

    // Ensure that no synthesized iframe was created.
    assertNull(form.getSynthesizedIFrame());

    // Submit the form and make sure the target frame comes back with the right
    // stuff (give it 2.5 s to complete).
    delayTestFinish(TEST_DELAY);
    form.submit();
    new Timer() {
      @Override
      public void run() {
        // Get the targeted frame and make sure it contains the requested
        // 'happyElem'.
        Frame frame = Frame.wrap(Document.get().getElementById(iframeId));
        assertTrue(isHappyDivPresent(frame.getElement()));
        finishTest();
      }
    }.schedule(TEST_DELAY - 2000);
  }

  public void testWrappedFormTargetAssertion() {
    // Testing a dev-mode-only assertion.
    if (!GWT.isScript()) {
      // Create a form element with the target attribute already set.
      final Element container = Document.get().createDivElement();
      container.setInnerHTML("<form target='foo'></form>");
      Document.get().getBody().appendChild(container);

      try {
        // Attempt to wrap it, requesting that an iframe be created.
        FormPanel.wrap(container.getFirstChildElement(), true);
        fail("Assertion expected wrapping a form with the target set");
      } catch (Throwable e) {
        // ok.
      }
    }
  }

  public void testWrappedFormWithIFrame() {
    // Create a form and frame in the document we can wrap.
    final Element container = Document.get().createDivElement();
    container.setInnerHTML(
        "<form method='get' "
        + "encoding='application/x-www-form-urlencoded' action='"
        + GWT.getModuleBaseURL() + "formHandler'>"
        + "<input type='text' name='tb' value='text'></input></form>");
    Document.get().getBody().appendChild(container);

    // Wrap the form, asking for an iframe to be created.
    FormPanel form = FormPanel.wrap(container.getFirstChildElement(), true);

    // Give the submit 5s to complete.
    delayTestFinish(TEST_DELAY);
    form.addSubmitCompleteHandler(new SubmitCompleteHandler() {
      public void onSubmitComplete(SubmitCompleteEvent event) {
        // Make sure we get our results back.
        assertTrue(event.getResults().equals("tb=text"));
        finishTest();
      }
    });

    form.submit();
  }

  @Override
  protected FormPanel createPanel() {
    return new FormPanel();
  }

  private native boolean isHappyDivPresent(Element iframe) /*-{
    return !!iframe.contentWindow.document.getElementById(':)');
  }-*/;
}
