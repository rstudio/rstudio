/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasWidgetsTester.WidgetAdder;

/**
 * Tests the FormPanel.
 * 
 * @see FormPanelTestServlet
 */
public class FormPanelTest extends GWTTestCase {
  public static boolean clicked = false;

  public String getModuleName() {
    return "com.google.gwt.user.FormPanelTest";
  }

  public void testAttachDetachOrder(HasWidgets container, WidgetAdder adder) {
    HasWidgetsTester.testAttachDetachOrder(new FormPanel());
  }

  public void testCancelSubmit() {
    TextBox tb = new TextBox();
    tb.setName("q");

    FormPanel form = new FormPanel();
    form.setWidget(tb);
    form.setAction("http://www.google.com/search");

    form.addFormHandler(new FormHandler() {
      public void onSubmit(FormSubmitEvent event) {
        event.setCancelled(true);
      }

      public void onSubmitComplete(FormSubmitCompleteEvent event) {
        fail("Form was cancelled and should not have been submitted");
      }
    });

    form.submit();
  }

  /**
   * Tests uploading a file using post & multipart encoding.
   */
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

    delayTestFinish(5000);
    form.addFormHandler(new FormHandler() {
      public void onSubmit(FormSubmitEvent event) {
      }

      public void onSubmitComplete(FormSubmitCompleteEvent event) {
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
    cb1.setChecked(true);
    cb0.setName("cb0");
    cb1.setName("cb1");

    RadioButton rb0 = new RadioButton("foo");
    RadioButton rb1 = new RadioButton("foo");
    rb0.setChecked(true);
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

    delayTestFinish(5000);

    form.addFormHandler(new FormHandler() {
      public void onSubmit(FormSubmitEvent event) {
      }

      public void onSubmitComplete(FormSubmitCompleteEvent event) {
        // The server just echoes the query string. This is what it should look
        // like.
        assertTrue(event.getResults().equals(
            "tb=text&amp;ptb=password&amp;cb1=on&amp;rb0=on&amp;lb=value1&amp;h=v"));
        finishTest();
      }
    });

    form.submit();
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

    delayTestFinish(5000);
    form.addFormHandler(new FormHandler() {
      public void onSubmit(FormSubmitEvent event) {
      }

      public void onSubmitComplete(FormSubmitCompleteEvent event) {
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
   */
  public void testSubmitFrame() {
    final NamedFrame frame = new NamedFrame("myFrame");
    FormPanel form = new FormPanel(frame);
    form.setMethod(FormPanel.METHOD_POST);
    form.setAction(GWT.getModuleBaseURL() + "formHandler?sendHappyHtml");
    RootPanel.get().add(form);
    RootPanel.get().add(frame);

    delayTestFinish(10000);
    Timer t = new Timer() {
      public void run() {
        // Make sure the frame got the contents we expected.
        assertTrue(isHappyDivPresent(frame.getElement()));
        finishTest();
      }

      private native boolean isHappyDivPresent(Element iframe) /*-{
        return !!iframe.contentWindow.document.getElementById(':)');
      }-*/;
    };

    // Wait 5 seconds before checking the results.
    t.schedule(5000);
    form.submit();
  }
}
