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
package com.google.gwt.dev.shell;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * Shared boilerplate for dialogs.
 */
public abstract class DialogBase extends Dialog implements DisposeListener {

  private class Buttons extends GridPanel {
    public Buttons(Composite parent) {
      super(parent, SWT.NONE, hasCancel ? 2 : 1, true);

      if (hasOk) {
        okButton = new Button(this, SWT.PUSH);
        setGridData(okButton, 1, 1, FILL, FILL, false, false);
        okButton.setText("    OK    ");
        okButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            clickOkButton();
          }
        });
      }

      if (hasCancel) {
        cancelButton = new Button(this, SWT.PUSH);
        setGridData(cancelButton, 1, 1, FILL, FILL, false, false);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            clickCancelButton();
          }
        });
      }

      shell.setDefaultButton(okButton);
    }
  }

  private class Contents extends GridPanel {

    public Contents(Composite parent) {
      super(parent, SWT.NONE, 1, false, 0, 0);

      Control contents = createContents(this);
      setGridData(contents, 1, 1, FILL, FILL, true, true);

      if (hasOk || hasCancel) {
        Buttons buttons = new Buttons(this);
        setGridData(buttons, 1, 1, RIGHT, BOTTOM, false, false);
      }
    }
  }

  /**
   * Pops up a confirm/cancel dialog.
   */
  public static boolean confirmAction(Shell shell, String msg, String msgTitle) {
    MessageBox msgBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES
        | SWT.NO);
    msgBox.setText(msgTitle);
    msgBox.setMessage(msg);
    return msgBox.open() == SWT.YES;
  }

  private Button cancelButton;

  private boolean cancelled = true;

  private boolean hasCancel;

  private boolean hasOk;

  private int minHeight;

  private int minWidth;

  private Button okButton;

  private Shell shell;

  public DialogBase(Shell parent, int minWidth, int minHeight) {
    this(parent, minWidth, minHeight, true, true);
  }

  public DialogBase(Shell parent, int minWidth, int minHeight,
      boolean hasOkButton, boolean hasCancelButton) {
    super(parent, SWT.NONE);
    this.minWidth = minWidth;
    this.minHeight = minHeight;
    hasOk = hasOkButton;
    hasCancel = hasCancelButton;
  }

  public Shell getShell() {
    return shell;
  }

  public boolean open() {
    return open(true);
  }

  public boolean open(boolean autoSize) {
    Shell parent = getParent();
    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL
        | SWT.RESIZE);
    shell.setImages(ShellMainWindow.getIcons());
    shell.setText(getText());
    shell.setLayout(new FillLayout());

    new Contents(shell);

    onOpen();

    int myWidth;
    int myHeight;
    if (autoSize) {
      // Try to make the dialog big enough to hold the packed layout or
      // the requested size, whichever is bigger.
      //
      shell.pack();

      Rectangle shellBounds = shell.getBounds();

      myWidth = Math.max(shellBounds.width, minWidth);
      myHeight = Math.max(shellBounds.height, minHeight);
    } else {
      myWidth = minWidth;
      myHeight = minHeight;
    }

    // Try to center within parent shell.
    //
    Rectangle parentBounds = parent.getBounds();
    int myLeft = parentBounds.x + (parentBounds.width / 2 - myWidth / 2);
    int myTop = parentBounds.y + (parentBounds.height / 4);

    shell.setBounds(myLeft, myTop, myWidth, myHeight);

    shell.open();

    Display display = parent.getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }

    return !cancelled;
  }

  @Override
  public void setText(String string) {
    super.setText(string);
    shell.setText(string);
  }

  public void widgetDisposed(DisposeEvent e) {
  }

  protected void clickCancelButton() {
    cancelled = true;
    onCancel();
    shell.dispose();
  }

  protected void clickOkButton() {
    cancelled = false;
    onOk();
    shell.dispose();
  }

  protected abstract Control createContents(Composite parent);

  protected void onCancel() {
  }

  protected void onOk() {
  }

  protected void onOpen() {
  }

  protected void setOkEnabled(boolean enabled) {
    okButton.setEnabled(enabled);
  }
}
