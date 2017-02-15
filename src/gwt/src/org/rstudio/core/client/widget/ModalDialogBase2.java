/*
 * ModalDialogBase2.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

import org.rstudio.core.client.Point;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.ShortcutManager.Handle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.studio.client.RStudioGinjector;

import java.util.ArrayList;

// TODOs for parity with ModalDialogBase:
//  - ModalDialogTracker
//  - setGlassEnabled()
//  - Animations
//  - DialogBox.setText()
//  - Styles

public abstract class ModalDialogBase2 extends PopupPanel
{
   protected ModalDialogBase2()
   {
      this(null);
   }

   private class MouseHandler implements MouseDownHandler, MouseUpHandler,
      MouseOutHandler, MouseOverHandler, MouseMoveHandler {

      @Override
      public void onMouseDown(MouseDownEvent event) {
         if (isResizeEvent(event.getNativeEvent())) {
            // TODO: Start Dragging
         }
      }

      @Override
      public void onMouseMove(MouseMoveEvent event) {
      }

      @Override
      public void onMouseUp(MouseUpEvent event) {
      }

      @Override
      public void onMouseOver(MouseOverEvent arg0)
      {
      }

      @Override
      public void onMouseOut(MouseOutEvent arg0)
      {
      }
   }

   protected ModalDialogBase2(SimplePanel containerPanel)
   {
      super(false);
      
      GWT.<Binder>create(Binder.class).createAndBindUi(this);
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

      // embed main panel in a custom container if specified
      containerPanel_ = containerPanel;
      if (containerPanel_ != null)
      {
         containerPanel_.setWidget(mainPanel_);
         setWidget(containerPanel_);
      }
      else
      {
         setWidget(mainPanel_);
      }

      MouseHandler mouseHandler = new MouseHandler();

      addDomHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            // Is this too aggressive? Alternatively we could only filter out
            // keycodes that are known to be problematic (pgup/pgdown)
            event.stopPropagation();
         }
      }, KeyDownEvent.getType());

      addDomHandler(mouseHandler, MouseDownEvent.getType());
      addDomHandler(mouseHandler, MouseUpEvent.getType());
      addDomHandler(mouseHandler, MouseMoveEvent.getType());
      addDomHandler(mouseHandler, MouseOverEvent.getType());
      addDomHandler(mouseHandler, MouseOutEvent.getType());
   }

   protected void beginDragging(MouseDownEvent event)
   {
      // Prevent text selection from occurring when moving the dialog box
      event.preventDefault();
   }

   protected void onLoad()
   {
      if (shortcutDisableHandle_ != null)
         shortcutDisableHandle_.close();
      shortcutDisableHandle_ = ShortcutManager.INSTANCE.disable();

      try
      {
         // 728: Focus remains in Source view when message dialog pops up over it
         NativeWindow.get().focus();
      }
      catch (Throwable e)
      {
      }
   }

   protected void onUnload()
   {
      if (shortcutDisableHandle_ != null)
         shortcutDisableHandle_.close();
      shortcutDisableHandle_ = null;
   }

   public boolean isEscapeDisabled()
   {
      return escapeDisabled_;
   }

   public void setEscapeDisabled(boolean escapeDisabled)
   {
      escapeDisabled_ = escapeDisabled;
   }

   public boolean isEnterDisabled()
   {
      return enterDisabled_;
   }

   public void setEnterDisabled(boolean enterDisabled)
   {
      enterDisabled_ = enterDisabled;
   }

   @Override
   public void show()
   {
      super.show();
   }

   public void showModal()
   {
      if (mainWidget_ == null)
      {
         mainWidget_ = createMainWidget();

         // get the main widget to line up with the right edge of the buttons.
         mainWidget_.getElement().getStyle().setMarginRight(2, Unit.PX);

         mainPanel_.insert(mainWidget_, 0);
      }

      originallyActiveElement_ = DomUtils.getActiveElement();
      if (originallyActiveElement_ != null)
         originallyActiveElement_.blur();

      setPopupPositionAndShow(new PopupPanel.PositionCallback(){
         public void setPosition(int offsetWidth, int offsetHeight) {
            int left = (Window.getClientWidth() - offsetWidth) / 2;
            int top = (Window.getClientHeight() - offsetHeight) / 2;
            setPopupPosition(left, top);
         }
      });
   }
   

   protected abstract Widget createMainWidget();

   protected void positionAndShowDialog(Command onCompleted)
   {
      onCompleted.execute();
   }

   protected void onDialogShown()
   {
   }

   protected void addOkButton(ThemedButton okButton)
   {
      okButton_ = okButton;
      okButton_.addStyleDependentName("DialogAction");
      okButton_.setDefault(defaultOverrideButton_ == null);
      addButton(okButton_);
   }

   protected void setOkButtonCaption(String caption)
   {
      okButton_.setText(caption);
   }

   protected void enableOkButton(boolean enabled)
   {
      okButton_.setEnabled(enabled);
   }

   protected void clickOkButton()
   {
      okButton_.click();
   }

   protected void setOkButtonVisible(boolean visible)
   {
      okButton_.setVisible(visible);
   }

   protected void focusOkButton()
   {
      if (okButton_ != null)
         FocusHelper.setFocusDeferred(okButton_);
   }

   protected void enableCancelButton(boolean enabled)
   {
      cancelButton_.setEnabled(enabled);
   }

   protected void setDefaultOverrideButton(ThemedButton button)
   {
      if (button != defaultOverrideButton_)
      {
         if (defaultOverrideButton_ != null)
            defaultOverrideButton_.setDefault(false);

         defaultOverrideButton_ = button;
         if (okButton_ != null)
            okButton_.setDefault(defaultOverrideButton_ == null);

         if (defaultOverrideButton_ != null)
            defaultOverrideButton_.setDefault(true);
      }
   }

   protected ThemedButton addCancelButton()
   {
      ThemedButton cancelButton = createCancelButton(null);
      addCancelButton(cancelButton);
      return cancelButton;
   }

   protected ThemedButton createCancelButton(final Operation cancelOperation)
   {
      return new ThemedButton("Cancel", new ClickHandler() {
         public void onClick(ClickEvent event) {
            if (cancelOperation != null)
               cancelOperation.execute();
            closeDialog();
         }
      });
   }

   protected void addCancelButton(ThemedButton cancelButton)
   {
      cancelButton_ = cancelButton;
      cancelButton_.addStyleDependentName("DialogAction");
      addButton(cancelButton_);
   }

   protected void addLeftButton(ThemedButton button)
   {
      button.addStyleDependentName("DialogAction");
      button.addStyleDependentName("DialogActionLeft");
      leftButtonPanel_.add(button);
      allButtons_.add(button);
   }

   protected void addLeftWidget(Widget widget)
   {
      leftButtonPanel_.add(widget);
   }

   protected void removeLeftWidget(Widget widget)
   {
      leftButtonPanel_.remove(widget);
   }

   protected void addButton(ThemedButton button)
   {
      button.addStyleDependentName("DialogAction");
      buttonPanel_.add(button);
      allButtons_.add(button);
   }

   // inserts an action button--in the same panel as OK/cancel, but preceding
   // them (presuming they're already present)
   protected void addActionButton(ThemedButton button)
   {
      button.addStyleDependentName("DialogAction");
      buttonPanel_.insert(button, 0);
      allButtons_.add(button);
   }

   protected void setButtonAlignment(HorizontalAlignmentConstant alignment)
   {
      bottomPanel_.setCellHorizontalAlignment(buttonPanel_, alignment);
   }

   protected ProgressIndicator addProgressIndicator()
   {
      return addProgressIndicator(true);
   }

   protected ProgressIndicator addProgressIndicator(
                                                    final boolean closeOnCompleted)
   {
      final SlideLabel label = new SlideLabel(true);
      Element labelEl = label.getElement();
      Style labelStyle = labelEl.getStyle();
      labelStyle.setPosition(Style.Position.ABSOLUTE);
      labelStyle.setLeft(0, Style.Unit.PX);
      labelStyle.setRight(0, Style.Unit.PX);
      labelStyle.setTop(-12, Style.Unit.PX);
      mainPanel_.add(label);
      
      return new ProgressIndicator()
      {
         public void onProgress(String message)
         {
            onProgress(message, null);
         }
         
         public void onProgress(String message, Operation onCancel)
         {
            if (message == null)
            {
               label.setText("", true);
               if (showing_)
                  clearProgress();
            }
            else
            {
               label.setText(message, false);
               if (!showing_)
               {
                  enableControls(false);
                  label.show();
                  showing_ = true;
               }
            }
            
            label.onCancel(onCancel);
         }

         public void onCompleted()
         {
            clearProgress();
            if (closeOnCompleted)
               closeDialog();
         }

         public void onError(String message)
         {
            clearProgress();
            RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
                  "Error", message);
         }

         @Override
         public void clearProgress()
         {
            if (showing_)
            {
               enableControls(true);
               label.hide();
               showing_ = false;
            }
            
         }
         
         private boolean showing_;
      };
   }

   public void closeDialog()
   {
      removeFromParent();

      try
      {
         if (originallyActiveElement_ != null
               && !originallyActiveElement_.getTagName().equalsIgnoreCase("body"))
         {
            Document doc = originallyActiveElement_.getOwnerDocument();
            if (doc != null)
            {
               originallyActiveElement_.focus();
            }
         }
      }
      catch (Exception e)
      {
         // focus() fail if the element is no longer visible. It's
         // easier to just catch this than try to detect it.

         // Also originallyActiveElement_.getTagName() can fail with:
         // "Permission denied to access property 'tagName' from a non-chrome context"
         // possibly due to Firefox "anonymous div" issue.
      }
      originallyActiveElement_ = null;
   }

   protected SimplePanel getContainerPanel()
   {
      return containerPanel_;
   }

   protected void enableControls(boolean enabled)
   {
      enableButtons(enabled);
      onEnableControls(enabled);
   }

   protected void onEnableControls(boolean enabled)
   {

   }

   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         NativeEvent nativeEvent = event.getNativeEvent();
         switch (nativeEvent.getKeyCode())
         {
         case KeyCodes.KEY_ENTER:

            if (enterDisabled_)
               break;

            // allow Enter on textareas
            Element e = DomUtils.getActiveElement();
            if (e.hasTagName("TEXTAREA"))
               return;

            ThemedButton defaultButton = defaultOverrideButton_ == null
                  ? okButton_
                  : defaultOverrideButton_;
            if ((defaultButton != null) && defaultButton.isEnabled())
            {
               nativeEvent.preventDefault();
               nativeEvent.stopPropagation();
               event.cancel();
               defaultButton.click();
            }
            break;
         case KeyCodes.KEY_ESCAPE:
            if (escapeDisabled_)
               break;
            onEscapeKeyDown(event);
            break;
         }
      }
   }

   protected void onEscapeKeyDown(Event.NativePreviewEvent event)
   {
      NativeEvent nativeEvent = event.getNativeEvent();
      if (cancelButton_ == null)
      {
         if ((okButton_ != null) && okButton_.isEnabled())
         {
            nativeEvent.preventDefault();
            nativeEvent.stopPropagation();
            event.cancel();
            okButton_.click();
         }
      }
      else if (cancelButton_.isEnabled())
      {
         nativeEvent.preventDefault();
         nativeEvent.stopPropagation();
         event.cancel();
         cancelButton_.click();
      }
   }

   private void enableButtons(boolean enabled)
   {
      for (int i = 0; i < allButtons_.size(); i++)
         allButtons_.get(i).setEnabled(enabled);
   }

   public void move(Point p, boolean allowAnimation)
   {
   }
   
   public void setText(String text)
   {
   }

   private boolean isResizeEvent(NativeEvent event) {
      EventTarget target = event.getEventTarget();
      if (Element.is(target)) {
         return Element.as(target) == resizeLeft_.getElement();
      }
      return false;
   }

   @UiTemplate("ModalDialogBase2.ui.xml")
   interface Binder extends UiBinder<Widget, ModalDialogBase2>
   {}
   
   public interface Styles extends CssResource
   {
      String mainPanel();
      String resizeLeft();
      String resizeRight();
      String resizeTop();
      String resizeBottom();

      String resizeTopLeft();
      String resizeTopRight();
      String resizeBottomRight();
      String resizeBottomLeft();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("ModalDialogBase2.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

   private Handle shortcutDisableHandle_;

   private boolean escapeDisabled_ = false;
   private boolean enterDisabled_ = false;
   private SimplePanel containerPanel_;
   
   
   @UiField
   VerticalPanel mainPanel_;
   
   @UiField
   HorizontalPanel bottomPanel_;
   
   @UiField
   HorizontalPanel buttonPanel_;
   
   @UiField
   HorizontalPanel leftButtonPanel_;

   @UiField
   HTMLPanel resizeLeft_;
   
   private ThemedButton okButton_;
   private ThemedButton cancelButton_;
   private ThemedButton defaultOverrideButton_;
   private ArrayList<ThemedButton> allButtons_ = new ArrayList<ThemedButton>();
   private Widget mainWidget_;
   private com.google.gwt.dom.client.Element originallyActiveElement_;
}
