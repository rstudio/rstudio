/*
 * CallFramePanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects.Observer;

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
   }
   
   public CallFramePanel(Observer observer, CallFramePanelHost panelHost)
   {
      final ThemeStyles globalStyles = ThemeResources.INSTANCE.themeStyles();
      panelHost_ = panelHost;
      
      // import the minimize button from the global theme resources
      HTML minimize = new HTML();
      minimize.setStylePrimaryName(globalStyles.minimize());
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

      Label tracebackTitle = new Label("Traceback");
      tracebackTitle.addStyleName(style.tracebackHeader());
      
      callFramePanelHeader.addStyleName(globalStyles.windowframe());
      callFramePanelHeader.add(tracebackTitle);
      callFramePanelHeader.add(minimize);
      callFramePanelHeader.setWidgetRightWidth(minimize, 14, Style.Unit.PX, 
                                                         14, Style.Unit.PX);
      
      observer_ = observer;
      callFrameItems_ = new ArrayList<CallFrameItem>();
   }

   public void setCallFrames(JsArray<CallFrame> frameList, int contextDepth)
   {
      clearCallFrames();
      
      // walk backwards through the call frames so we can figure out when 
      // user code was first encountered on the callstack. 
      boolean encounteredUserCode = false;
      for (int idx = frameList.length() - 1; idx >= 0; idx--)
      {
         CallFrame frame = frameList.get(idx);
         if (CallFrameItem.isNavigableFilename(frame.getFileName())) 
         {
            encounteredUserCode = true;
         }
         // hide the frame if it isn't the top frame and we haven't yet 
         // encountered user code
         CallFrameItem item = new CallFrameItem(
               frame, 
               observer_, 
               !encounteredUserCode && idx > 0);
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

   public int getHeightOfAllFrames()
   {
      if (callFrameItems_.size() == 0)
      {
         return 0;
      }
      else
      {
         int totalFrameSize = 0;
         for (int idx = 0; idx < callFrameItems_.size(); idx++)
         {
            totalFrameSize += callFrameItems_.get(idx).getHeight();
         }
         return totalFrameSize + style.callFramePanelMargin();
      }
   }
   
   public boolean isMinimized()
   {
      return isMinimized_;
   }

   @UiField
   HTMLPanel callFramePanel;
   @UiField
   CallFramePanelStyle style;
   @UiField
   LayoutPanel callFramePanelHeader;
   
   Observer observer_;
   CallFramePanelHost panelHost_;
   ArrayList<CallFrameItem> callFrameItems_;
   boolean isMinimized_ = false;
}