/*
 * BasePresenter.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views;

import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.Widgetable;
import org.rstudio.studio.client.workbench.WorkbenchView;

public abstract class BasePresenter implements Widgetable
{
   protected BasePresenter(WorkbenchView view)
   {
      view_ = view;
   }

   public Widget toWidget()
   {
      return (Widget) view_;
   }

   public void onBeforeSelected()
   {
      view_.onBeforeSelected();
   }

   public void onSelected()
   {
      view_.onSelected();
   }

   private final WorkbenchView view_;
}
