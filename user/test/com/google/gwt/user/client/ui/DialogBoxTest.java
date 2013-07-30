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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Unit test for {@link DialogBox}.
 */
public class DialogBoxTest extends PopupTest {

  /**
   * An implementation of Caption which is used for testing.
   */
  private static class CaptionForTesting extends Composite implements
      DialogBox.Caption, HasHTML {

    private FocusPanel panel = new FocusPanel();
    private HTML htmlWidget = new HTML();

    public CaptionForTesting() {
      panel.add(htmlWidget);
      initWidget(panel);
    }

    public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
      return panel.addMouseDownHandler(handler);
    }

    public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
      return panel.addMouseMoveHandler(handler);
    }

    public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
      return panel.addMouseOutHandler(handler);
    }

    public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
      return panel.addMouseOverHandler(handler);
    }

    public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
      return panel.addMouseUpHandler(handler);
    }

    public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
      return panel.addMouseWheelHandler(handler);
    }

    public String getHTML() {
      return htmlWidget.getHTML();
    }

    public String getText() {
      return htmlWidget.getText();
    }

    public void setHTML(SafeHtml html) {
      htmlWidget.setHTML(html);
    }

    public void setHTML(String html) {
      this.htmlWidget.setHTML(html);
    }

    public void setText(String text) {
      htmlWidget.setText(text);
    }
  }

  private static final String html = "<b>hello</b><i>world</i>";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test the accessors.
   */
  @Override
  public void testAccessors() {
    super.testAccessors();

    // Set the widget
    DialogBox box1 = new DialogBox();
    assertNull(box1.getWidget());
    HTML contents1 = new HTML("Contents");
    box1.setWidget(contents1);
    assertEquals(contents1, box1.getWidget());

    // Set widget to null
    box1.setWidget(null);
    assertNull(box1.getWidget());
  }

  /**
   * Test getters and setters for the caption.
   */
  public void testCaption() {
    DialogBox dialogBox = new DialogBox();

    // Set the caption as text
    dialogBox.setText("text");
    assertEquals("text", dialogBox.getText());
    dialogBox.setText("<b>text</b>");
    assertEquals("<b>text</b>", dialogBox.getText());

    // Set the caption as html
    dialogBox.setHTML("text");
    assertEquals("text", dialogBox.getText());
    assertEquals("text", dialogBox.getHTML());
    dialogBox.setHTML("<b>text</b>");
    assertEquals("text", dialogBox.getText());
    assertTrue(dialogBox.getHTML().equalsIgnoreCase("<b>text</b>"));
  }

  public void testDebugId() {
    DialogBox dBox = new DialogBox();
    dBox.setAnimationEnabled(false);
    dBox.ensureDebugId("myDialogBox");
    dBox.setText("test caption");
    Label content = new Label("content");
    dBox.setWidget(content);
    dBox.show();

    // Check the body ids
    UIObjectTest.assertDebugId("myDialogBox", dBox.getElement());
    UIObjectTest.assertDebugId("myDialogBox-content",
        DOM.getParent(content.getElement()));

    delayTestFinish(5000);
    // Check the header IDs
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        UIObjectTest.assertDebugIdContents("myDialogBox-caption",
        "test caption");
        finishTest();
      }
    });
  }

  /**
   * HtmlUnit test failed intermittently in nometa mode.
   */
  @DoNotRunWith(Platform.HtmlUnitUnknown)
  @Override
  public void testDependantPopupPanel() {
    // Create the dependent popup
    final PopupPanel dependantPopup = createPopupPanel();
    dependantPopup.setAnimationEnabled(true);

    // Create the primary popup
    final DialogBox primaryPopup = new DialogBox(false, false) {
      @Override
      protected void onAttach() {
        dependantPopup.show();
        super.onAttach();
      }

      @Override
      protected void onDetach() {
        dependantPopup.hide();
        super.onDetach();
      }
    };
    primaryPopup.setAnimationEnabled(true);

    testDependantPopupPanel(primaryPopup);
  }

  public void testSafeHtmlConstructor() {
    DialogBox box = new DialogBox();
    box.setHTML(SafeHtmlUtils.fromSafeConstant(html));

    assertEquals(html, box.getHTML().toLowerCase());
  }

  /**
   * Test setting the caption.
   */
  public void testSetCaption() {
    CaptionForTesting caption = new CaptionForTesting();
    DialogBox dialogBox = new DialogBox(caption);
    caption.setText("text");
    Element td = dialogBox.getCellElement(0, 1);
    assertEquals(dialogBox.getText(), "text");
    caption.setHTML("<b>text</b>");
    assertEquals("<b>text</b>", dialogBox.getHTML().toLowerCase());
    dialogBox.show();
    assertTrue(dialogBox.getCaption() == caption);
    assertTrue(caption.asWidget().getElement() == DOM.getChild(td, 0));
    dialogBox.hide();
  }

  public void testSimpleCloseButtonOnModalDialog() {
    final DialogBox dialogBox = new DialogBox(false, true);
    Button button = new Button();
    button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    dialogBox.add(button);
    dialogBox.show();
    button.click();
    assertFalse(dialogBox.isShowing());
  }

  @Override
  protected PopupPanel createPopupPanel() {
    return new DialogBox();
  }
}
