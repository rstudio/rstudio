/*
 * CallFramePanel.java
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;

import java.util.ArrayList;
import java.util.Collections;

public class CallFramePanel extends ResizeComposite
{
   public interface Binder extends UiBinder<Widget, CallFramePanel>
   {
   }
   
   public interface CallFramePanelHost
   {
      void minimizeCallFramePanel();
      void restoreCallFramePanel();
      boolean getShowInternalFunctions();
      void setShowInternalFunctions(boolean hide);
   }
   
   public CallFramePanel(EnvironmentObjectsObserver observer, CallFramePanelHost panelHost)
   {
      final ThemeStyles globalStyles = ThemeResources.INSTANCE.themeStyles();
      panelHost_ = panelHost;
      
      // import the minimize button from the global theme resources
      HTML minimize = new HTML();
      minimize.setStylePrimaryName(globalStyles.minimize());
      minimize.addStyleName(ThemeStyles.INSTANCE.handCursor());
      minimize.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            if (isMinimized_)
            {
               callFramePanelHeader.removeStyleName(globalStyles.minimizedWindow());
               panelHost_.restoreCallFramePanel();
               isMinimized_ = false;
            }
            else
            {
               callFramePanelHeader.addStyleName(globalStyles.minimizedWindow());
               panelHost_.minimizeCallFramePanel();
               isMinimized_ = true;
            }
         }
      });
      
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      addStyleName("ace_editor_theme");

      Label tracebackTitle = new Label("Traceback");
      tracebackTitle.addStyleName(style.tracebackHeader());
      
      callFramePanelHeader.addStyleName(globalStyles.windowframe());
      callFramePanelHeader.add(tracebackTitle);
      CheckBox showInternals = new CheckBox("Show internals");
      showInternals.addStyleName(style.showInternalsCheckbox());
      showInternals.setValue(panelHost_.getShowInternalFunctions());
      showInternals.addValueChangeHandler(
            new ValueChangeHandler<Boolean>()
            {
               @Override
               public void onValueChange(ValueChangeEvent<Boolean> event)
               {
                  panelHost_.setShowInternalFunctions(event.getValue());
                  // Ignore the function on the top of the stack; we always
                  // want to show it since it's the execution point
                  for (int i = 1; i < callFrameItems_.size(); i++) 
                  {
                     CallFrameItem item = callFrameItems_.get(i);
                     if (!item.isNavigable() && !item.isHidden())
                     {
                        item.setVisible(event.getValue());
                     }
                  }
               }
            }
      );
      showInternals.setStylePrimaryName(style.toggleHide());
            
      callFramePanelHeader.add(showInternals);
      callFramePanelHeader.setWidgetRightWidth(
                     showInternals, 28, Style.Unit.PX, 
                                    30, Style.Unit.PCT);
      callFramePanelHeader.add(minimize);
      callFramePanelHeader.setWidgetRightWidth(minimize, 14, Style.Unit.PX, 
                                                         14, Style.Unit.PX);
      
      observer_ = observer;
      callFrameItems_ = new ArrayList<CallFrameItem>();
   }

   public void setCallFrames(JsArray<CallFrame> frameList, int contextDepth)
   {
      clearCallFrames();
      
      // Check to see whether every function on the stack is internal. 
      // If it is, the traceback window may appear empty, so show everything
      // to give the user some context.
      boolean allInternal = true;
      int idxSourceEquiv = Integer.MAX_VALUE;

      for (int idx = 0; idx < frameList.length(); idx++)
      {
         CallFrame frame = frameList.get(idx);
         if (frame.isNavigable()) {
            allInternal = false;
         }
         if (frame.isSourceEquiv())
            idxSourceEquiv = idx;
      }
      
      for (int idx = frameList.length() - 1; idx >= 0; idx--)
      {
         CallFrame frame = frameList.get(idx);
         // Always show the first frame, since that's where execution is 
         // actually halted. From the remaining frames, show them if they are
         // "navigable" (user) frames, or if the user has elected to show all
         // frames.
         CallFrameItem item = new CallFrameItem(
               frame, 
               observer_, 
               frame.isHidden() ||
                  (!panelHost_.getShowInternalFunctions() && 
                     ((!frame.isNavigable()) || idx > idxSourceEquiv) &&
                     !allInternal &&
                     idx > 0));
         if (contextDepth == frame.getContextDepth())
         {
            item.setActive();
         }
         callFrameItems_.add(item);
      }
      
      // now walk forwards through the frames and add each to the UI
      Collections.reverse(callFrameItems_);
      for (CallFrameItem item: callFrameItems_)
      {
         callFramePanel.add(item);
      }
   }

   public void updateLineNumber(int newLineNumber)
   {
      if (callFrameItems_.size() > 0)
      {
         callFrameItems_.get(0).updateLineNumber(newLineNumber);
      }
   }

   public void clearCallFrames()
   {
      callFramePanel.clear();
      callFrameItems_.clear();
   }

   public int getDesiredPanelHeight()
   {
      return callFramePanelHeader.getOffsetHeight() + 
            callFramePanel.getOffsetHeight();
   }
   
   public boolean isMinimized()
   {
      return isMinimized_;
   }

   @UiField HTMLPanel callFramePanel;
   @UiField CallFramePanelStyle style;
   @UiField LayoutPanel callFramePanelHeader;
   
   EnvironmentObjectsObserver observer_;
   CallFramePanelHost panelHost_;
   ArrayList<CallFrameItem> callFrameItems_;
   boolean isMinimized_ = false;
}
