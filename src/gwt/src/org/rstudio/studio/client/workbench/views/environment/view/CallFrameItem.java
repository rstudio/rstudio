/*
 * CallFrameItem.java
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects.Observer;

public class CallFrameItem extends Composite
   implements ClickHandler
{
   public interface Binder extends UiBinder<Widget, CallFrameItem>
   {
   }

   interface Style extends CssResource
   {
      String activeFrame();
      String callFrame();
   }

   public CallFrameItem(CallFrame frame, Observer observer)
   {
      isActive_ = false;
      observer_ = observer;
      frame_ = frame;
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));
      functionName.setText(
              frame.getFunctionName() + " at " +
              frame.getFileName().trim() + ":" +
              frame.getLineNumber());
      functionName.addClickHandler(this);
   }

   public void setActive()
   {
      functionName.addStyleName(style.activeFrame());
      isActive_ = true;
   }

   public void onClick(ClickEvent event)
   {
      if (!isActive_)
      {
         observer_.changeContextDepth(frame_.getContextDepth());
      }
   }

   @UiField Label functionName;
   @UiField Style style;

   Observer observer_;
   CallFrame frame_;
   boolean isActive_;
}