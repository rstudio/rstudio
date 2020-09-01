/*
 * ModalDialogBase.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.aria.client.DialogRole;
import com.google.gwt.aria.client.Id;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import elemental2.dom.DomGlobal;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.ShortcutManager;
import org.rstudio.core.client.command.ShortcutManager.Handle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.NativeWindow;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.Timers;

import java.util.ArrayList;
import java.util.List;

public abstract class ModalDialogBase extends DialogBox
                                      implements AriaLiveStatusReporter
{

   public interface ReturnFocusHandler
   {
      boolean returnFocus(Element el);
   }

   public static HandlerRegistration registerReturnFocusHandler(final ReturnFocusHandler handler)
   {
      FOCUS_HANDLERS.add(handler);

      return new HandlerRegistration()
      {
         @Override
         public void removeHandler()
         {
            FOCUS_HANDLERS.remove(handler);
         }
      };
   }

   protected ModalDialogBase(DialogRole role)
   {
      this(null, role);
   }

   protected ModalDialogBase(SimplePanel containerPanel, DialogRole role)
   {
      // core initialization. passing false for modal works around
      // modal PopupPanel suppressing global keyboard accelerators (like
      // Ctrl-N or Ctrl-T). modality is achieved via setGlassEnabled(true)
      super(false, false);
      setGlassEnabled(true);
      addStyleDependentName("ModalDialog");
      addStyleName(RES.styles().modalDialog());

      // a11y
      role_ = role;
      role_.set(getElement());
      focus_ = new FocusHelper(getElement());

      // main panel used to host UI
      mainPanel_ = new VerticalPanel();
      bottomPanel_ = new HorizontalPanel();
      bottomPanel_.setStyleName(ThemeStyles.INSTANCE.dialogBottomPanel());
      bottomPanel_.setWidth("100%");
      buttonPanel_ = new HorizontalPanel();
      leftButtonPanel_ = new HorizontalPanel();
      bottomPanel_.add(leftButtonPanel_);
      bottomPanel_.add(buttonPanel_);

      ariaLiveStatusWidget_ = new AriaLiveStatusWidget();
      bottomPanel_.add(ariaLiveStatusWidget_);

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

      addDomHandler(event ->
      {
         // Is this too aggressive? Alternatively we could only filter out
         // keycodes that are known to be problematic (pgup/pgdown)
         if (handleKeyDownEvent(event)) {
            event.stopPropagation();
         }
      }, KeyDownEvent.getType());
   }
   
   protected void hideButtons()
   {
     buttonPanel_.setVisible(false);
   }
   
   protected boolean handleKeyDownEvent(KeyDownEvent event) {
      return true;
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

   public void setRestoreFocusOnClose(boolean restoreFocus)
   {
      restoreFocus_ = restoreFocus;
   }

   public void showModal()
   {
      showModal(true);
   }

   public void showModal(boolean restoreFocus)
   {
      if (mainWidget_ == null)
      {
         mainWidget_ = createMainWidget();

         // get the main widget to line up with the right edge of the buttons.
         mainWidget_.getElement().getStyle().setMarginRight(2, Unit.PX);
         mainPanel_.insert(mainWidget_, 0);
      }

      restoreFocus_ = restoreFocus;

      if (restoreFocus)
      {
         originallyActiveElement_ = DomUtils.getActiveElement();
         if (originallyActiveElement_ != null)
            originallyActiveElement_.blur();
      }

      // position the dialog
      positionAndShowDialog(() ->
      {
         // defer shown notification to allow all elements to render
         // before attempting to interact w/ them programmatically (e.g. setFocus)
         Timers.singleShot(100, () -> onDialogShown());
      });
   }

   protected abstract Widget createMainWidget();

   protected void positionAndShowDialog(Command onCompleted)
   {
      super.center();
      onCompleted.execute();

      // Force the contents of the modal to be vertically scrollable
      // if the height of the modal is larger than the app window
      Element e = this.getElement();
      Element child = e.getFirstChildElement();
      if (child != null)
      {
         int windowInnerHeight = DomGlobal.window.innerHeight;
         if (windowInnerHeight <= 10) return; // degenerate property case

         // snap the top of the modal to the top bounds of the window
         int eleTop = e.getAbsoluteTop();
         if (eleTop < 0)
         {
            eleTop = 0;
            e.getStyle().setTop(0, Unit.PX);
         }
         int eleHeight = e.getOffsetHeight();

         if (eleHeight + 30 >= windowInnerHeight)
         {
            child.getStyle().setProperty("overflowY", "auto");

            // don't override overflowX if it's already set
            String overflowX = child.getStyle().getProperty("overflowX");
            if (overflowX == null || overflowX.length() < 1)
            {
               child.getStyle().setProperty("overflowX", "hidden");
            }
            child.getStyle().setPropertyPx("maxHeight", windowInnerHeight - eleTop - 30);
         }
      }
   }

   protected void onDialogShown()
   {
      refreshFocusableElements();
      focusInitialControl();
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
      return new ThemedButton("Cancel", clickEvent ->
      {
         if (cancelOperation != null)
            cancelOperation.execute();
         closeDialog();
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

      // nothing to do if we don't have an element to return focus to
      if (originallyActiveElement_ == null)
         return;

      try
      {
         if (restoreFocus_)
            restoreFocus();
      }
      catch (Exception e)
      {
         // intentionally swallow exceptions (as they can occur
         // for a multitude of reasons and generally are not actionable)
      }
      finally
      {
         originallyActiveElement_ = null;
      }
   }

   private void restoreFocus()
   {
      // iterate over focus handlers (in reverse order so
      // most recently added handlers are executed first)
      // and see if a registered handler can fire
      for (int i = 0, n = FOCUS_HANDLERS.size(); i < n; i++)
      {
         try
         {
            // first, try running a registered focus handler
            ReturnFocusHandler handler = FOCUS_HANDLERS.get(n - i - 1);
            if (handler.returnFocus(originallyActiveElement_))
               return;

         }
         catch (Exception e)
         {
            // swallow exceptions (attempts to focus an element can
            // fail for a multitude of reasons and those reasons are
            // usually not actionable by the user)
         }
      }

      try
      {
         // if no registered handler fired, then just focus element
         originallyActiveElement_.focus();
      }
      catch (Exception e)
      {
         // swallow exceptions
      }

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

            // allow Enter on textareas, buttons, or anchors (including custom links)
            Element e = DomUtils.getActiveElement();
            if (e.hasTagName("TEXTAREA") ||
                  e.hasTagName("A") ||
                  e.hasTagName("BUTTON") ||
                  e.hasClassName(ALLOW_ENTER_KEY_CLASS) ||
                  (e.hasAttribute("role") && StringUtil.equals(e.getAttribute("role"), "link")))
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
            if (nativeEvent.getShiftKey() && focus_.isFirst(DomUtils.getActiveElement()))
            {
               nativeEvent.preventDefault();
               nativeEvent.stopPropagation();
               event.cancel();
               focusLastControl();
            }
            else if (!nativeEvent.getShiftKey() && focus_.isLast(DomUtils.getActiveElement()))
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
      for (ThemedButton allButton : allButtons_)
         allButton.setEnabled(enabled);
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
    * refreshFocusableElements or setFirstFocusableElement.
    *
    * Invoked when Tabbing off the last control in the modal dialog to set focus back to
    * the first control, and by default to set initial focus when the dialog is shown.
    *
    * To set focus on a different control when the dialog is displayed, override
    * focusInitialControl, instead.
    */
   protected void focusFirstControl()
   {
      focus_.focusFirstControl();
   }

    /**
    * Set focus on last keyboard focusable element in dialog, as set by
    * <code>refreshFocusableElements</code> or <code>setLastFocusableElement</code>.
    */
   protected void focusLastControl()
   {
      focus_.focusLastControl();
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
    * Gets an ordered list of keyboard-focusable elements in the dialog.
    */
   public ArrayList<Element> getFocusableElements()
   {
      return DomUtils.getFocusableElements(getElement());
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
      if (!DomUtils.isEffectivelyVisible(getElement()))
         return;

      ArrayList<Element> focusable = getFocusableElements();
      if (focusable.size() == 0)
      {
         Debug.logWarning("No potentially focusable controls found in modal dialog");
         return;
      }
      focus_.setFirst(focusable.get(0));
      focus_.setLast(focusable.get(focusable.size() - 1));
   }

   /**
    * Perform a deferred update of focusable elements, then set focus on the initial control.
    */
   public void deferRefreshFocusableElements()
   {
      Scheduler.get().scheduleDeferred(() ->
      {
         refreshFocusableElements();
         focusInitialControl();
      });
   }

   public interface Styles extends CssResource
   {
      String modalDialog();
   }

   public interface Resources extends ClientBundle
   {
      @Source("ModalDialogBase.css")
      Styles styles();
   }

   @Override
   public void reportStatus(String status, int delayMs, Severity severity)
   {
      ariaLiveStatusWidget_.reportStatus(status, delayMs, severity);
   }

   private static final Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

   private Handle shortcutDisableHandle_;

   private boolean escapeDisabled_ = false;
   private boolean enterDisabled_ = false;
   private final SimplePanel containerPanel_;
   private final VerticalPanel mainPanel_;
   private final HorizontalPanel bottomPanel_;
   private final HorizontalPanel buttonPanel_;
   private final HorizontalPanel leftButtonPanel_;
   private ThemedButton okButton_;
   private ThemedButton cancelButton_;
   private ThemedButton defaultOverrideButton_;
   private final ArrayList<ThemedButton> allButtons_ = new ArrayList<>();
   private Widget mainWidget_;
   private boolean restoreFocus_;
   private Element originallyActiveElement_;
   private Animation currentAnimation_ = null;
   private final DialogRole role_;
   private final AriaLiveStatusWidget ariaLiveStatusWidget_;
   private final FocusHelper focus_;
   private static final List<ReturnFocusHandler> FOCUS_HANDLERS = new ArrayList<>();
   
   public static final String ALLOW_ENTER_KEY_CLASS = "__rstudio_modal_allow_enter_key";
}
