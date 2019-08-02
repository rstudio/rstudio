/*
 * ModalDialogBase.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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


import com.google.gwt.animation.client.Animation;
import com.google.gwt.aria.client.DialogRole;
import com.google.gwt.aria.client.Id;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.ShortcutManager.Handle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ui.RStudioThemes;

import java.util.ArrayList;

public abstract class ModalDialogBase extends DialogBox
{
   private static final String firstFocusClass = "__rstudio_modal_first_focus";
   private static final String lastFocusClass = "__rstudio_modal_last_focus";

   protected ModalDialogBase(DialogRole role)
   {
      this(null, role);
   }

   protected ModalDialogBase(SimplePanel containerPanel, DialogRole role)
   {
      // core initialization. passing false for modal works around
      // modal PopupPanel supressing global keyboard accelerators (like
      // Ctrl-N or Ctrl-T). modality is achieved via setGlassEnabled(true)
      super(false, false);
      setGlassEnabled(true);
      addStyleDependentName("ModalDialog");

      // a11y
      role_ = role;
      role_.set(getElement());
      A11y.setARIADialogModal(getElement());

      // main panel used to host UI
      mainPanel_ = new VerticalPanel();
      bottomPanel_ = new HorizontalPanel();
      bottomPanel_.setStyleName(ThemeStyles.INSTANCE.dialogBottomPanel());
      bottomPanel_.setWidth("100%");
      buttonPanel_ = new HorizontalPanel();
      leftButtonPanel_ = new HorizontalPanel();
      bottomPanel_.add(leftButtonPanel_);
      bottomPanel_.add(buttonPanel_);
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      mainPanel_.add(bottomPanel_);

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

      addDomHandler(new KeyDownHandler()
      {
         public void onKeyDown(KeyDownEvent event)
         {
            // Is this too aggressive? Alternatively we could only filter out
            // keycodes that are known to be problematic (pgup/pgdown)
            event.stopPropagation();
         }
      }, KeyDownEvent.getType());
   }

   @Override
   protected void beginDragging(MouseDownEvent event)
   {
      // Prevent text selection from occurring when moving the dialog box
      event.preventDefault();
      super.beginDragging(event);
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      ModalDialogTracker.onShow(this);
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

      RStudioThemes.disableDarkMenus();
   }

   @Override
   protected void onUnload()
   {
      if (shortcutDisableHandle_ != null)
         shortcutDisableHandle_.close();
      shortcutDisableHandle_ = null;

      ModalDialogTracker.onHide(this);

      RStudioThemes.enableDarkMenus();

      super.onUnload();
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

      // position the dialog
      positionAndShowDialog(new Command() {
         @Override
         public void execute()
         {
            // defer shown notification to allow all elements to render
            // before attempting to interact w/ them programmatically (e.g. setFocus)
            Timer timer = new Timer() {
               public void run() {
                  onDialogShown();
               }
            };
            timer.schedule(100);
         }
      });
   }
   

   protected abstract Widget createMainWidget();

   protected void positionAndShowDialog(Command onCompleted)
   {
      super.center();
      onCompleted.execute();
   }

   protected void onDialogShown()
   {
      refreshFocusableElements();
      focusInitialControl();
      
      // try hard to make sure focus ends up in dialog
      Element focused = DomUtils.getActiveElement();
      if (focused == null || !DomUtils.contains(getElement(), focused))
      {
        focusFirstControl();
      }
   }

   protected void addOkButton(ThemedButton okButton, String elementId)
   {
      okButton_ = okButton;
      okButton_.addStyleDependentName("DialogAction");
      okButton_.setDefault(defaultOverrideButton_ == null);
      addButton(okButton_, elementId);
   }

   protected void addOkButton(ThemedButton okButton)
   {
      addOkButton(okButton, ElementIds.DIALOG_OK_BUTTON);
   }

   protected void setOkButtonCaption(String caption)
   {
      okButton_.setText(caption);
   }
   
   protected void setOkButtonId(String id)
   {
      okButton_.getElement().setId(id);
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

   /**
    * Set focus on the OK button
    * @return true if button can receive focus, false if button doesn't exist or was disabled
    */
   protected boolean focusOkButton()
   {
      if (okButton_ == null || !okButton_.isEnabled() || !okButton_.isVisible())
         return false;

      FocusHelper.setFocusDeferred(okButton_);
      return true;
   }

   protected void enableCancelButton(boolean enabled)
   {
      cancelButton_.setEnabled(enabled);
   }

    /**
    * Set focus on the cancel button
    * @return true if button received focus, false if button doesn't exist or was disabled
    */
    protected boolean focusCancelButton()
   {
      if (cancelButton_ == null || !cancelButton_.isEnabled() || !cancelButton_.isVisible())
         return false;
      FocusHelper.setFocusDeferred(cancelButton_);
      return true;
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

   protected ThemedButton addCancelButton(String elementId)
   {
      ThemedButton cancelButton = createCancelButton(null);
      addCancelButton(cancelButton);
      ElementIds.assignElementId(cancelButton.getElement(), elementId);
      return cancelButton;
   }

   protected ThemedButton addCancelButton()
   {
      return addCancelButton(ElementIds.DIALOG_CANCEL_BUTTON);
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

   protected void addCancelButton(ThemedButton cancelButton, String elementId)
   {
      cancelButton_ = cancelButton;
      cancelButton_.addStyleDependentName("DialogAction");
      addButton(cancelButton_, elementId);
   }

   protected void addCancelButton(ThemedButton cancelButton)
   {
      addCancelButton(cancelButton, ElementIds.DIALOG_CANCEL_BUTTON);
   }

   protected void addLeftButton(ThemedButton button, String elementId)
   {
      button.addStyleDependentName("DialogAction");
      button.addStyleDependentName("DialogActionLeft");
      ElementIds.assignElementId(button.getElement(), elementId);
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

   protected void addButton(ThemedButton button, String elementId)
   {
      button.addStyleDependentName("DialogAction");
      buttonPanel_.add(button);
      ElementIds.assignElementId(button.getElement(), elementId);
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

   protected ProgressIndicator addProgressIndicator(final boolean closeOnCompleted)
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
      hide();
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

   @Override
   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
      if (!ModalDialogTracker.isTopMost(this))
         return;

      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         NativeEvent nativeEvent = event.getNativeEvent();
         switch (nativeEvent.getKeyCode())
         {
         case KeyCodes.KEY_ENTER:

            if (enterDisabled_)
               break;

            // allow Enter on textareas or anchors (including custom links)
            Element e = DomUtils.getActiveElement();
            if (e.hasTagName("TEXTAREA") || e.hasTagName("A") || 
                  (e.hasAttribute("role") && e.getAttribute("role") == "link"))
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
            
         case KeyCodes.KEY_TAB:
            if (nativeEvent.getShiftKey() && DomUtils.getActiveElement().hasClassName(firstFocusClass))
            {
               nativeEvent.preventDefault();
               nativeEvent.stopPropagation();
               event.cancel();
               focusLastControl();
            }
            else if (!nativeEvent.getShiftKey() && DomUtils.getActiveElement().hasClassName(lastFocusClass))
            {
               nativeEvent.preventDefault();
               nativeEvent.stopPropagation();
               event.cancel();
               focusFirstControl();
            }
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
      if (!isShowing() || !allowAnimation)
      {
         // Don't animate if not showing
         setPopupPosition(p.getX(), p.getY());
         return;
      }

      if (currentAnimation_ != null)
      {
         currentAnimation_.cancel();
         currentAnimation_ = null;
      }

      final int origLeft = getPopupLeft();
      final int origTop = getPopupTop();
      final int deltaX = p.getX() - origLeft;
      final int deltaY = p.getY() - origTop;

      currentAnimation_ = new Animation()
      {
         @Override
         protected void onUpdate(double progress)
         {
            if (!isShowing())
               cancel();
            else
            {
               setPopupPosition((int) (origLeft + deltaX * progress),
                     (int) (origTop + deltaY * progress));
            }
         }
      };
      currentAnimation_.run(200);
   }

   @Override
   public void setText(String text)
   {
      super.setText(text);
      role_.setAriaLabelProperty(getElement(), text);
   }

   /**
    * Optional description of dialog for accessibility tools 
    * @param element element containing the description
    */
   protected void setARIADescribedBy(Element element)
   {
      String id = element.getId();
      if (StringUtil.isNullOrEmpty(id))
      {
         id = DOM.createUniqueId();
         element.setId(id);
      }
      role_.setAriaDescribedbyProperty(getElement(), Id.of(element));
   }

   /**
    * Set focus on first keyboard focusable element in dialog, as set by 
    * <code>refreshFocusableElements</code> or <code>setFirstFocusableElement</code>.
    */
   protected void focusFirstControl()
   {
      Element first = getByClass(firstFocusClass);
      if (first != null)
         first.focus();
   }

    /**
    * Set focus on last keyboard focusable element in dialog, as set by 
    * <code>refreshFocusableElements</code> or <code>setLastFocusableElement</code>.
    */
   protected void focusLastControl()
   {
      Element last = getByClass(lastFocusClass);
      if (last != null)
         last.focus();
   }

   /**
    * Invoked when dialog first loads to set initial focus. By default sets focus on the 
    * first control in the dialog; override to set initial focus elsewhere.
    */
   protected void focusInitialControl()
   {
      focusFirstControl();
   }

   /**
    * @param element first keyboard focusable element in the dialog
    */
   private void setFirstFocusableElement(Element element)
   {
      removeExisting(firstFocusClass);
      element.addClassName(firstFocusClass);
   }

   /**
    * @param element last keyboard focusable element in the dialog
    */
   private void setLastFocusableElement(Element element)
   {
      removeExisting(lastFocusClass);
      element.addClassName(lastFocusClass);
   }

   /**
    * Gets an ordered list of keyboard-focusable elements in the dialog.
    */
   public ArrayList<Element> getFocusableElements()
   {
      // css selector from https://github.com/scottaohara/accessible_modal_window
      String focusableElements = 
            "button:not([hidden]):not([disabled]), [href]:not([hidden]), " +
            "input:not([hidden]):not([type=\"hidden\"]):not([disabled]), " +
            "select:not([hidden]):not([disabled]), textarea:not([hidden]):not([disabled]), " +
            "[tabindex=\"0\"]:not([hidden]):not([disabled]), summary:not([hidden]), " +
            "[contenteditable]:not([hidden]), audio[controls]:not([hidden]), " +
            "video[controls]:not([hidden])";
      NodeList<Element> potentiallyFocusable = DomUtils.querySelectorAll(getElement(), focusableElements);

      ArrayList<Element> focusable = new ArrayList<>();
      for (int i = 0; i < potentiallyFocusable.getLength(); i++)
      {
         // only include items taking up space
         if (potentiallyFocusable.getItem(i).getOffsetWidth() > 0 && 
               potentiallyFocusable.getItem(i).getOffsetHeight() > 0)
         {
            focusable.add(potentiallyFocusable.getItem(i));
         }
      }
      return focusable;
   }

   /**
    * Gets a list of keyboard focusable elements in the dialog, and tracks which ones are
    * first and last. This is used to keep keyboard focus in the dialog when Tabbing and
    * Shift+Tabbing off end or beginning of dialog.
    * 
    * If the dialog is dynamic, and the first and/or last focusable elements change over time,
    * call this function again to update the information; or, if the auto-detection
    * is not suitable, override focusFirstControl and/or focusLastControl.
    */
   public void refreshFocusableElements()
   {
      ArrayList<Element> focusable = getFocusableElements(); 
      if (focusable.size() == 0)
      {
         Debug.logWarning("No potentially focusable controls found in modal dialog");
         return;
      }
      setFirstFocusableElement(focusable.get(0));
      setLastFocusableElement(focusable.get(focusable.size() - 1));
   }

   /**
    * Perform a deferred update of focusable elements, then set focus on the initial control.
    */
   public void deferRefreshFocusableElements()
   {
      Scheduler.get().scheduleDeferred(() -> {
         refreshFocusableElements();
         focusInitialControl();
      });
   }

   private void removeExisting(String classname)
   {
      Element current = getByClass(classname);
      if (current != null)
         current.removeClassName(classname);
   }

   private Element getByClass(String classname)
   {
      NodeList<Element> current = DomUtils.querySelectorAll(getElement(), "." + classname);
      if (current.getLength() > 1)
      {
         Debug.logWarning("Multiple controls found with class: " + classname);
         return null;
      }
      if (current.getLength() == 1)
      {
         return current.getItem(0);
      }
      return null;
   }

   private Handle shortcutDisableHandle_;

   private boolean escapeDisabled_ = false;
   private boolean enterDisabled_ = false;
   private SimplePanel containerPanel_;
   private VerticalPanel mainPanel_;
   private HorizontalPanel bottomPanel_;
   private HorizontalPanel buttonPanel_;
   private HorizontalPanel leftButtonPanel_;
   private ThemedButton okButton_;
   private ThemedButton cancelButton_;
   private ThemedButton defaultOverrideButton_;
   private ArrayList<ThemedButton> allButtons_ = new ArrayList<ThemedButton>();
   private Widget mainWidget_;
   private com.google.gwt.dom.client.Element originallyActiveElement_;
   private Animation currentAnimation_ = null;
   private DialogRole role_;
}
