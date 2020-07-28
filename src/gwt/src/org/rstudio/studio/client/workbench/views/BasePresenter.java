/*
 * BasePresenter.java
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
package org.rstudio.studio.client.workbench.views;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.WorkbenchView;

public abstract class BasePresenter implements IsWidget
{
   protected BasePresenter(WorkbenchView view)
   {
      view_ = view;
   }

   public WorkbenchView getView()
   {
      return view_;
   }

   @Override
   public Widget asWidget()
   {
      return (Widget) view_;
   }

   public void onBeforeUnselected()
   {
      view_.onBeforeUnselected();
   }

   public void onBeforeSelected()
   {
      view_.onBeforeSelected();
   }

   public void onSelected()
   {
      view_.onSelected();
   }

   public void setFocus()
   {
      view_.setFocus();
   }
   
   private final WorkbenchView view_;
}
