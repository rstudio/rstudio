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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Unit test for {@link DialogBox}.
 */
public class DialogBoxTest extends PopupTest {

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

  @DoNotRunWith({Platform.Htmlunit})
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

    delayTestFinish(250);
    // Check the header IDs
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        UIObjectTest.assertDebugIdContents("myDialogBox-caption",
            "test caption");
        finishTest();
      }
    });
  }

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

  @Override
  protected PopupPanel createPopupPanel() {
    return new DialogBox();
  }
}
