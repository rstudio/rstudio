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
import com.google.gwt.junit.client.GWTTestCase;

/**
 * A series of tests to ensure that widgets with a wrap() method properly assert
 * their element types.
 */
public class WidgetSubclassingTest extends GWTTestCase {

  private static final String ASSERTION_ERROR = "Should have received an assertion error trying to use the wrong element type";

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  // Correct subclasses.
  private static class TestAnchor extends Anchor {
    public TestAnchor() {
      super(Document.get().createAnchorElement());
    }
  }

  private static class TestButton extends Button {
    public TestButton() {
      super(Document.get().createButtonElement());
    }
  }

  private static class TestFileUpload extends FileUpload {
    public TestFileUpload() {
      super(Document.get().createFileInputElement());
    }
  }

  private static class TestFormPanel extends FormPanel {
    public TestFormPanel() {
      super(Document.get().createFormElement());
    }
  }

  private static class TestFrame extends Frame {
    public TestFrame() {
      super(Document.get().createIFrameElement());
    }
  }

  private static class TestHidden extends Hidden {
    public TestHidden() {
      super(Document.get().createHiddenInputElement());
    }
  }

  private static class TestHTML extends HTML {
    public TestHTML() {
      super(Document.get().createDivElement());
    }
  }

  private static class TestImage extends Image {
    public TestImage() {
      super(Document.get().createImageElement());
    }
  }

  private static class TestInlineHTML extends InlineHTML {
    public TestInlineHTML() {
      super(Document.get().createSpanElement());
    }
  }

  private static class TestInlineLabel extends InlineLabel {
    public TestInlineLabel() {
      super(Document.get().createSpanElement());
    }
  }

  private static class TestLabel extends Label {
    public TestLabel() {
      super(Document.get().createSpanElement());
    }
  }

  private static class TestListBox extends ListBox {
    public TestListBox() {
      super(Document.get().createSelectElement());
    }
  }

  private static class TestPasswordTextBox extends PasswordTextBox {
    public TestPasswordTextBox() {
      super(Document.get().createPasswordInputElement());
    }
  }

  private static class TestSimpleCheckBox extends SimpleCheckBox {
    public TestSimpleCheckBox() {
      super(Document.get().createCheckInputElement());
    }
  }

  private static class TestSimpleRadioButton extends SimpleRadioButton {
    public TestSimpleRadioButton() {
      super(Document.get().createRadioInputElement("group"));
    }
  }

  private static class TestTextBox extends TextBox {
    public TestTextBox() {
      super(Document.get().createTextInputElement());
    }
  }

  // Broken subclasses.
  private static class BrokenAnchor extends Anchor {
    public BrokenAnchor() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenButton extends Button {
    public BrokenButton() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenFileUpload extends FileUpload {
    public BrokenFileUpload() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenFormPanel extends FormPanel {
    public BrokenFormPanel() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenFrame extends Frame {
    public BrokenFrame() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenHidden extends Hidden {
    public BrokenHidden() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenHTML extends HTML {
    public BrokenHTML() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenImage extends Image {
    public BrokenImage() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenInlineHTML extends InlineHTML {
    public BrokenInlineHTML() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenInlineLabel extends InlineLabel {
    public BrokenInlineLabel() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenLabel extends Label {
    public BrokenLabel() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenListBox extends ListBox {
    public BrokenListBox() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenPasswordTextBox extends PasswordTextBox {
    public BrokenPasswordTextBox() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenSimpleCheckBox extends SimpleCheckBox {
    public BrokenSimpleCheckBox() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenSimpleRadioButton extends SimpleRadioButton {
    public BrokenSimpleRadioButton() {
      super(Document.get().createBRElement());
    }
  }

  private static class BrokenTextBox extends TextBox {
    public BrokenTextBox() {
      super(Document.get().createBRElement());
    }
  }

  public void testAnchor() {
    // Make sure the normal case works.
    new TestAnchor();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenAnchor();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testButton() {
    // Make sure the normal case works.
    new TestButton();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenButton();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testFileUpload() {
    // Make sure the normal case works.
    new TestFileUpload();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenFileUpload();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testFormPanel() {
    // Make sure the normal case works.
    new TestFormPanel();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenFormPanel();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testFrame() {
    // Make sure the normal case works.
    new TestFrame();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenFrame();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testHidden() {
    // Make sure the normal case works.
    new TestHidden();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenHidden();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testHTML() {
    // Make sure the normal case works.
    new TestHTML();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenHTML();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testImage() {
    // Make sure the normal case works.
    new TestImage();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenImage();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testInlineHTML() {
    // Make sure the normal case works.
    new TestInlineHTML();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenInlineHTML();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testInlineLabel() {
    // Make sure the normal case works.
    new TestInlineLabel();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenInlineLabel();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testLabel() {
    // Make sure the normal case works.
    new TestLabel();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenLabel();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testListBox() {
    // Make sure the normal case works.
    new TestListBox();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenListBox();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testPasswordTextBox() {
    // Make sure the normal case works.
    new TestPasswordTextBox();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenPasswordTextBox();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testSimpleCheckBox() {
    // Make sure the normal case works.
    new TestSimpleCheckBox();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenSimpleCheckBox();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testSimpleRadioButton() {
    // Make sure the normal case works.
    new TestSimpleRadioButton();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenSimpleRadioButton();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }

  public void testTextBox() {
    // Make sure the normal case works.
    new TestTextBox();

    // And the wrong element type doesn't.
    if (!GWT.isScript()) {
      try {
        new BrokenTextBox();
        fail(ASSERTION_ERROR);
      } catch (AssertionError e) {
      }
    }
  }
}
