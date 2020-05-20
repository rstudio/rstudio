/*
 * CallFrameItem.java
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;

public class CallFrameItem extends Composite
   implements ClickHandler
{
   public interface Binder extends UiBinder<Widget, CallFrameItem>
   {
   }

   interface Style extends CssResource
   {
      String callFrame();
      String activeFrame();
      String noSourceFrame();
      String hiddenFrame();
   }

   public CallFrameItem(CallFrame frame, 
                        EnvironmentObjectsObserver observer, boolean hidden)
   {
      isActive_ = false;
      isVisible_ = true;
      observer_ = observer;
      frame_ = frame;
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      functionName.addStyleName(ThemeResources.INSTANCE.themeStyles().handCursor());
      functionName.addClickHandler(this);
      if (!frame.isNavigable() || hidden)
      {
         functionName.addStyleName(style.noSourceFrame());
         
         // hide call frames for which we don't have usable sources--but leave
         // them in the DOM (we may want to easily show/hide these at the user's
         // request)
         if (hidden)
         {
            functionName.addStyleName(style.hiddenFrame());
            isVisible_ = false;
         }
      }
      setDisplayText(frame_.getLineNumber());
      
      FontSizer.applyNormalFontSize(this);
   }

   public void setActive()
   {
      functionName.addStyleName(style.activeFrame());
      isActive_ = true;
   }

   public void updateLineNumber(int newLineNumber)
   {
      setDisplayText(newLineNumber);
   }

   public void onClick(ClickEvent event)
   {
      if (!isActive_)
      {
         observer_.changeContextDepth(frame_.getContextDepth());
      }
   }
   
   public void setVisible(boolean visible)
   {
      if (visible != isVisible_)
      {
         if (visible)
         {
            functionName.removeStyleName(style.hiddenFrame());
         }
         else
         {
            functionName.addStyleName(style.hiddenFrame());
         }
         isVisible_ = visible;
      }
   }

   public boolean isNavigable()
   {
      return frame_.isNavigable();
   }
   
   public boolean isHidden()
   {
      return frame_.isHidden();
   }

   // Private functions -------------------------------------------------------

   private void setDisplayText(int lineNumber)
   {
      if (frame_.getContextDepth() > 0)
      {
         String fileLocation = "";
         if (hasFileLocation())
         {
            fileLocation = " at " +
                           FilePathUtils.friendlyFileName(
                                 frame_.getFileName()) + ":" +
                           lineNumber;
         }
         functionName.setText(
                 getFrameLabel() + 
                 fileLocation);
      }
      else
      {
         functionName.setText(getFrameLabel());
      }
   }

   private boolean hasFileLocation()
   {
      return CallFrame.isNavigableFilename(frame_.getFileName());
   }
   
   private String getFrameLabel()
   {
      if (frame_.isSourceEquiv())
      {
         return "[Debug source]";
      }
      if (frame_.getShinyFunctionLabel().isEmpty())
      {
         return frame_.getCallSummary();
      }
      else
      {
         return "[Shiny: " + frame_.getShinyFunctionLabel() + "]";
      }
   }

   @UiField Label functionName;
   @UiField Style style;

   EnvironmentObjectsObserver observer_;
   CallFrame frame_;
   boolean isActive_;
   boolean isVisible_;
}
