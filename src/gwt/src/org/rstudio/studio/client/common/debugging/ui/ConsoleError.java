/*
 * ConsoleError.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.common.debugging.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.debugging.model.ErrorFrame;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ConsoleError extends Composite
{
   private static ConsoleErrorUiBinder uiBinder = GWT.create(ConsoleErrorUiBinder.class);

   interface ConsoleErrorUiBinder extends UiBinder<Widget, ConsoleError>
   {
   }
   
   public interface Observer
   {
      void onErrorBoxResize();
      void runCommandWithDebug(String command);
   }

   public ConsoleError(UnhandledError error,
                       String errorClass, 
                       String command,
                       Observer observer,
                       Element errorEl)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      observer_ = observer;
      command_ = command;
      
      initWidget(uiBinder.createAndBindUi(this));
      
      // If we're using extended condition highlighting for console errors,
      // we need to add those classes back here
      String highlightConditions = prefs_.consoleHighlightConditions().getValue();
      if (highlightConditions != UserPrefsAccessor.CONSOLE_HIGHLIGHT_CONDITIONS_NONE)
      {
         getElement().addClassName(VirtualConsole.RES.styles().group());
         getElement().addClassName(VirtualConsole.RES.styles().groupError());
         errorMessage.getElement().getStyle().setPaddingLeft(6, Unit.PX);
      }
      
      // If errorEl is provided, then take ownership of it -- replace this widget's
      // 'errorMessage' element with the provided 'errorEl'. If it's not provided,
      // then generate it using a virtual console attached to this widget's 'errorMessage'.
      if (errorEl != null)
      {
         errorMessage.getElement().getParentElement().replaceChild(errorEl, errorMessage.getElement());
      }
      else
      {
         VirtualConsole console = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(errorMessage.getElement());
         String message = error.getErrorMessage();
         console.submit(message);

         // the message is complete, so show any text a stray escape held back
         console.flushPartialAnsiCode();
      }
      
      if (errorClass != null)
         errorMessage.addStyleName(errorClass);

      // The Console inserts only this widget's element, never attaching the widget,
      // so the links' own GWT click handlers would not fire; listen on the root instead.
      EventListener onConsoleErrorClick = event ->
      {
         if (DOM.eventGetType(event) == Event.ONCLICK)
         {
            Element target = Element.as(event.getEventTarget());
            if (target == null)
               return;

            if (target.hasClassName("show_traceback_text") || target.hasClassName("show_traceback_image"))
            {
               setTracebackVisible(!showingTraceback_);
               observer_.onErrorBoxResize();
            }
            else if (target.hasClassName("rerun_text") || target.hasClassName("rerun_image"))
            {
               observer_.onErrorBoxResize();
               observer_.runCommandWithDebug(command_);
            }
         }
      };

      // Sets the localized label, so it doesn't change language on first toggle
      setTracebackVisible(false);
      rerunText.setText(constants_.rerunWithDebugText());
      rerunText.setVisible(command_ != null);
      rerunImage.setVisible(command_ != null);

      DOM.sinkEvents(this.getElement(), Event.ONCLICK);
      DOM.setEventListener(this.getElement(), onConsoleErrorClick);

      JsArray<ErrorFrame> frames = error.getErrorFrames();
      for (int i = frames.length() - 1; i >= 0; i--)
      {
         ConsoleErrorFrame frame = new ConsoleErrorFrame(i + 1, frames.get(i));
         framePanel.add(frame);
      }
   }
   
   @Inject
   private void initialize(UserPrefs prefs)
   {
      prefs_ = prefs;
   }
   
   public void setTracebackVisible(boolean visible)
   {
      showingTraceback_ = visible;
      showTracebackText.setText(showingTraceback_
            ? constants_.hideTracebackText()
            : constants_.showTracebackText());
      showTracebackText.getElement().setAttribute("aria-expanded",
            showingTraceback_ ? "true" : "false");
      framePanel.setVisible(showingTraceback_);
   }

   public void focus()
   {
      showTracebackText.setFocus(true);
   }

   /**
    * Shows, as a tooltip on the error's buttons, the shortcut that brings focus
    * back to the most recent error. Browsers also expose the tooltip to screen
    * readers as the buttons' description.
    *
    * @param shortcut The shortcut as displayed text, e.g. "Ctrl+Alt+X".
    */
   public void setFocusShortcutHint(String shortcut)
   {
      String hint = constants_.focusLastConsoleErrorHint(shortcut);
      showTracebackText.setTitle(hint);
      rerunText.setTitle(hint);
   }

   @UiField Button showTracebackText;
   @UiField Image showTracebackImage;
   @UiField Button rerunText;
   @UiField Image rerunImage;
   @UiField HTMLPanel framePanel;
   @UiField HTML errorMessage;
   
   private Observer observer_;
   private boolean showingTraceback_ = false;
   private String command_;
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
   
   // Injected
   private UserPrefs prefs_;
   
}
