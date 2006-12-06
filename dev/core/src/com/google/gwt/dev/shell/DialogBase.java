// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.dev.GWTShell;

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

public abstract class DialogBase extends Dialog implements DisposeListener {

/** 
 * Pops up a confirm/cancel dialog. 
 */
	public static boolean confirmAction(Shell shell, String msg, String msgTitle) {
		MessageBox msgBox = new MessageBox(shell, SWT.ICON_WARNING
				| SWT.YES | SWT.NO);
		msgBox.setText(msgTitle);
		msgBox.setMessage(msg);
		return msgBox.open()==SWT.YES;
	}
	

private class Buttons extends GridPanel {
    public Buttons(Composite parent) {
      super(parent, SWT.NONE, fHasCancel ? 2 : 1, true);

      if (fHasOk) {
        fOKButton = new Button(this, SWT.PUSH);
        setGridData(fOKButton, 1, 1, FILL, FILL, false, false);
        fOKButton.setText("    OK    ");
        fOKButton.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            clickOkButton();
          }
        });
      }

      if (fHasCancel) {
        fCancelButton = new Button(this, SWT.PUSH);
        setGridData(fCancelButton, 1, 1, FILL, FILL, false, false);
        fCancelButton.setText("Cancel");
        fCancelButton.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
            clickCancelButton();
          }
        });
      }

      fShell.setDefaultButton(fOKButton);
    }
  }

  private class Contents extends GridPanel {

    public Contents(Composite parent) {
      super(parent, SWT.NONE, 1, false, 0, 0);

      Control contents = createContents(this);
      setGridData(contents, 1, 1, FILL, FILL, true, true);

      if (fHasOk || fHasCancel) {
        Buttons buttons = new Buttons(this);
        setGridData(buttons, 1, 1, RIGHT, BOTTOM, false, false);
      }
    }
  }

  public DialogBase(Shell parent, int minWidth, int minHeight) {
    this(parent, minWidth, minHeight, true, true);
  }

  public DialogBase(Shell parent, int minWidth, int minHeight,
      boolean hasOkButton, boolean hasCancelButton) {
    super(parent, SWT.NONE);
    fMinWidth = minWidth;
    fMinHeight = minHeight;
    fHasOk = hasOkButton;
    fHasCancel = hasCancelButton;
  }

  public Shell getShell() {
    return fShell;
  }

  public boolean open() {
    return open(true);
  }

  public boolean open(boolean autoSize) {
    Shell parent = getParent();
    fShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL
      | SWT.RESIZE);
    fShell.setImages(GWTShell.getIcons());
    fShell.setText(getText());
    fShell.setLayout(new FillLayout());

    new Contents(fShell);

    onOpen();

    int myWidth;
    int myHeight;
    if (autoSize) {
      // Try to make the dialog big enough to hold the packed layout or
      // the requested size, whichever is bigger.
      //
      fShell.pack();

      Rectangle shellBounds = fShell.getBounds();

      myWidth = Math.max(shellBounds.width, fMinWidth);
      myHeight = Math.max(shellBounds.height, fMinHeight);
    } else {
      myWidth = fMinWidth;
      myHeight = fMinHeight;
    }

    // Try to center within parent shell.
    //
    Rectangle parentBounds = parent.getBounds();
    int myLeft = parentBounds.x + (parentBounds.width / 2 - myWidth / 2);
    int myTop = parentBounds.y + (parentBounds.height / 4);

    fShell.setBounds(myLeft, myTop, myWidth, myHeight);

    fShell.open();

    Display display = parent.getDisplay();
    while (!fShell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }

    return !fCancelled;
  }

  public void setText(String string) {
    super.setText(string);
    fShell.setText(string);
  }

  public void widgetDisposed(DisposeEvent e) {
  }

  protected void clickCancelButton() {
    fCancelled = true;
    onCancel();
    fShell.dispose();
  }

  protected void clickOkButton() {
    fCancelled = false;
    onOk();
    fShell.dispose();
  }

  protected abstract Control createContents(Composite parent);

  protected void onCancel() {
  }

  protected void onOk() {
  }

  protected void onOpen() {
  }

  protected void setOkEnabled(boolean enabled) {
    fOKButton.setEnabled(enabled);
  }

  private Button fCancelButton;
  private boolean fCancelled = true;
  private boolean fHasCancel;
  private boolean fHasOk;
  private int fMinHeight;
  private int fMinWidth;
  private Button fOKButton;
  private Shell fShell;
}
