/*
 * PanimrroHTMLDialog.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.panmirror.dialogs;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;

import org.rstudio.core.client.jsinterop.JsStringConsumer;
import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

import jsinterop.annotations.JsFunction;

public class PanmirrorHTMLDialog extends ModalDialog<Boolean>
{
   @JsFunction
   public interface CreateFn
   {
      Element create(int conatinerWidth, int containerHeight, 
                     JsVoidFunction confirm,
                     JsVoidFunction cancel,
                     JsStringConsumer showProgress,
                     JsVoidFunction hideProgress);
   }

   @JsFunction
   public interface ValidateFn
   {
      String validate();
   }

   public PanmirrorHTMLDialog(String title, String okText, CreateFn create, JsVoidFunction focus,
         ValidateFn validate, OperationWithInput<Boolean> operation)
   {
      super(title, Roles.getDialogRole(), operation, () -> {
         operation.execute(false);
      });

      if (okText != null)
         setOkButtonCaption(okText);

      focus_ = focus;
      validate_ = validate;

      // create main widget
      ProgressIndicator indicator = addProgressIndicator(false);
      Element mainWidgetEl = create.create(Window.getClientWidth(), Window.getClientHeight(),
            () -> {
               clickOkButton();
            }, () -> {
               operation.execute(false);
               closeDialog();
            }, (message) -> {
               indicator.onProgress(message);
            }, () -> {
               indicator.onCompleted();
            }
         );
      mainWidget_ = new DialogWidget(mainWidgetEl);
      
      // prevent default action handling
      hideButtons();
      setEnterDisabled(true);
      setEscapeDisabled(true);
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   protected void focusInitialControl()
   {
      focus_.call();
   }

   @Override
   protected Boolean collectInput()
   {
      return true;
   }

   @Override
   protected boolean validate(Boolean result)
   {
      String error = validate_.validate();
      if (error != null)
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage("Error", error,
               new CanFocus()
               {
                  @Override
                  public void focus()
                  {
                     focus_.call();
                  }
               });
         return false;
      }
      else
      {
         return true;
      }
   }

   @Override
   protected boolean handleKeyDownEvent(KeyDownEvent event) {
      
      // Arrow keys are allowed
      if (KeyCodeEvent.isArrow(event.getNativeKeyCode())) {
         return false;
      }
      
      // Page Up and Down allowed
      switch (event.getNativeKeyCode()) {
      case KeyCodes.KEY_PAGEDOWN:   
      case KeyCodes.KEY_PAGEUP:
      case KeyCodes.KEY_SPACE:
      case KeyCodes.KEY_ESCAPE:
      case KeyCodes.KEY_ENTER:
            return false;
      }
      return true;
   }

   private class DialogWidget extends Widget
   {
      public DialogWidget(Element el)
      {
         setElement(el);
      }
   }

   private DialogWidget mainWidget_;

   private JsVoidFunction focus_;
   private ValidateFn validate_;

}
