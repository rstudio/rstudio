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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects.Observer;

public class CallFramePanel extends ResizeComposite
{
   public interface Binder extends UiBinder<Widget, CallFramePanel>
   {
   }

   public CallFramePanel(Observer observer)
   {
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      observer_ = observer;
   }

   public void setCallFrames(JsArray<CallFrame> frameList, int contextDepth)
   {
      callFramePanel.clear();
      for (int idx = 0; idx < frameList.length(); idx++)
      {
         CallFrame frame = frameList.get(idx);
         CallFrameItem item = new CallFrameItem(frame, observer_);
         if (contextDepth == frame.getContextDepth())
         {
            item.setActive();
         }
         callFramePanel.add(item);
      }
   }

   public void clearCallFrames()
   {
      callFramePanel.clear();
   }

   @UiField
   VerticalPanel callFramePanel;

   Observer observer_;
}