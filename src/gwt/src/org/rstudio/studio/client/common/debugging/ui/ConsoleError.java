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
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;

import org.rstudio.core.client.Debug;
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
import com.google.gwt.user.client.ui.Anchor;
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

   // Because we are adding interactive elements to the VirtualScroller the GWT bindings are lost.
   // We need to programmatically find the elements and manipulate them through regular JS functions.
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
      }
      
      if (errorClass != null)
         errorMessage.addStyleName(errorClass);

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

      NodeList<Node> children = this.getElement().getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         Node n = children.getItem(i);
         if (n.getNodeType() != Node.ELEMENT_NODE)
            continue;

         Element child = Element.as(children.getItem(i));
         if (child.hasClassName("show_traceback_text")) {
            if (showingTraceback_)
               child.setInnerText(constants_.hideTracebackText());
            else
               child.setInnerText(constants_.showTracebackText());
         }
         else if (child.hasClassName("stack_trace"))
         {
            if (showingTraceback_)
               child.getStyle().setDisplay(Style.Display.BLOCK);
            else
               child.getStyle().setDisplay(Style.Display.NONE);
         }
      }
   }

   @UiField Anchor showTracebackText;
   @UiField Image showTracebackImage;
   @UiField Anchor rerunText;
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
