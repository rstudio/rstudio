/*
 * CppCompletionToolTip.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class CppCompletionToolTip extends PopupPanel
{
   public CppCompletionToolTip()
   {
      this("");
   }
   
   public CppCompletionToolTip(String text)
   {
      super(true);  
      panel_ = new HorizontalPanel();
      panel_.addStyleName(CppCompletionResources.INSTANCE.styles().toolTip());
      panel_.add(label_ = new Label()); 
      setWidget(panel_);
      setText(text);
   }

   public void setText(String text)
   {
      if (!text.equals(label_.getText()))
         label_.setText(text);
   }
   
   public void addLeftWidget(Widget widget)
   {
      panel_.insert(widget, 0);
   }
   
   public void setMaxWidth(int maxWidth)
   {
      getElement().getStyle().setPropertyPx("maxWidth", maxWidth);
   }
   
   private Label label_;
   private HorizontalPanel panel_;
}
