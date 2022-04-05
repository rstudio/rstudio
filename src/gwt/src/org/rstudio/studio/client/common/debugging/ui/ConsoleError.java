/*
 * ConsoleError.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

package org.rstudio.studio.client.common.debugging.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.resources.client.CssResource;
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.VirtualConsole;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.StudioClientCommonConstants;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;

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

   interface ConsoleErrorStyle extends CssResource {
      String tracebackLinkSelected();
   }

   // Because we are adding interactive elements to the VirtualScroller the GWT bindings are lost.
   // We need to programmatically find the elements and manipulate them through regular JS functions.
   public ConsoleError(UnhandledError err, 
                       String errorClass, 
                       Observer observer, 
                       String command)
   {
      observer_ = observer;
      command_ = command;
      
      initWidget(uiBinder.createAndBindUi(this));
      
      VirtualConsole vc = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(errorMessage.getElement());
      vc.submit(err.getErrorMessage());
      
      errorMessage.addStyleName(errorClass);

      rerunText.setVisible(command_ != null);
      rerunImage.setVisible(command_ != null);

      EventListener onTracebackLinkClick = event ->
      {
         if (DOM.eventGetType(event) == Event.ONCLICK)
         {
            Element target = Element.as(event.getEventTarget());
            if (target == null)
               return;
            
            if (target.hasClassName("rerun_text") || target.hasClassName("rerun_image"))
            {
               observer_.onErrorBoxResize();
               observer_.runCommandWithDebug(command_);
            }
            else if (target.hasClassName("traceback_full"))
            {
               showTracebackFull();
            }
            else if (target.hasClassName("traceback_branch"))
            {
               showTracebackBranch();
            }
            else if (target.hasClassName("traceback_hide"))
            {
               hideTraceback();
            }
         }
      };
      DOM.sinkEvents(tracebackLinks.getElement(), Event.ONCLICK);
      DOM.setEventListener(tracebackLinks.getElement(), onTracebackLinkClick);

      // use rlang back traces if possible
      JsArrayString trace = err.getTrace();
      if (trace.length() > 0) {
         hasFullTraceback_ = true;
         VirtualConsole vcTracebackFull = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(tracebackFull.getElement());
         vcTracebackFull.submit(trace.get(0));

         VirtualConsole vcTracebackBranch = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(tracebackBranch.getElement());
         vcTracebackBranch.submit(trace.get(1));
      } else 
      {
         tracebackLinkFull.getElement().getStyle().setDisplay(Style.Display.NONE);
         for (int i = err.getErrorFrames().length() - 1; i >= 0; i--)
         {
            ConsoleErrorFrame frame = new ConsoleErrorFrame(i + 1,
                  err.getErrorFrames().get(i));
            framePanel.add(frame);
         }
      }

   }
   
   public void setTracebackVisible(boolean visible)
   {
      if (visible)
      {
         if (hasFullTraceback_)
            showTracebackFull();
         else 
            showTracebackBranch();
      }
      else 
      {
         hideTraceback();
      }
   }

   public void showTracebackFull() 
   {
      showingTraceback_ = true;
      framePanel.getElement().getStyle().setDisplay(Style.Display.BLOCK);
      tracebackFull.getElement().getStyle().setDisplay(Style.Display.BLOCK);
      tracebackBranch.getElement().getStyle().setDisplay(Style.Display.NONE);
      
      tracebackLinkFull.getElement().addClassName(style.tracebackLinkSelected());
      tracebackLinkBranch.getElement().removeClassName(style.tracebackLinkSelected());
      tracebackLinkHide.getElement().removeClassName(style.tracebackLinkSelected());
   }

   public void showTracebackBranch() 
   {
      showingTraceback_ = true;
      framePanel.getElement().getStyle().setDisplay(Style.Display.BLOCK);
      tracebackFull.getElement().getStyle().setDisplay(Style.Display.NONE);
      tracebackBranch.getElement().getStyle().setDisplay(Style.Display.BLOCK);

      tracebackLinkFull.getElement().removeClassName(style.tracebackLinkSelected());
      tracebackLinkBranch.getElement().addClassName(style.tracebackLinkSelected());
      tracebackLinkHide.getElement().removeClassName(style.tracebackLinkSelected());
   }

   public void hideTraceback() 
   {
      showingTraceback_ = false;
      framePanel.getElement().getStyle().setDisplay(Style.Display.NONE);
      tracebackFull.getElement().getStyle().setDisplay(Style.Display.NONE);
      tracebackBranch.getElement().getStyle().setDisplay(Style.Display.NONE);

      tracebackLinkFull.getElement().removeClassName(style.tracebackLinkSelected());
      tracebackLinkBranch.getElement().removeClassName(style.tracebackLinkSelected());
      tracebackLinkHide.getElement().addClassName(style.tracebackLinkSelected());
   }

   @UiField Anchor rerunText;
   @UiField Image rerunImage;
   @UiField HTMLPanel framePanel;
   @UiField HTML errorMessage;

   @UiField HTMLPanel tracebackLinks;
   @UiField HTMLPanel tracebackFull;
   @UiField HTMLPanel tracebackBranch;

   @UiField Anchor tracebackLinkFull;
   @UiField Anchor tracebackLinkBranch;
   @UiField Anchor tracebackLinkHide;
   
   @UiField ConsoleErrorStyle style;

   private Observer observer_;
   private boolean showingTraceback_ = false;
   private boolean hasFullTraceback_ = false;
   private String command_;
   private static final StudioClientCommonConstants constants_ = GWT.create(StudioClientCommonConstants.class);
}
