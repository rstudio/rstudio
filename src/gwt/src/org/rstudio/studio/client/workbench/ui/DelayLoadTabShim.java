/*
 * DelayLoadTabShim.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.widget.Widgetable;

public abstract class DelayLoadTabShim<T extends Widgetable,
      TParentTab extends DelayLoadWorkbenchTab<T>> extends AsyncShim<T>
{

   protected final TParentTab getParentTab()
   {
      return parentTab_;
   }

   @SuppressWarnings("unchecked")
   public void setParentTab(DelayLoadWorkbenchTab<T> parentTab)
   {
      parentTab_ = (TParentTab) parentTab;
   }

   @Override
   protected void onDelayLoadSuccess(T obj)
   {
      super.onDelayLoadSuccess(obj);
      final Widget child = obj.toWidget();
      
      if (child instanceof HasEnsureVisibleHandlers)
      {
         ((HasEnsureVisibleHandlers)child).addEnsureVisibleHandler(
               new EnsureVisibleHandler()
         {
            public void onEnsureVisible(EnsureVisibleEvent event)
            {
               parentTab_.ensureVisible();
            }
         });
      }

      child.setSize("100%", "100%");
      parentTab_.getPanel().add(child);
   }

   public abstract void onBeforeSelected();
   public abstract void onSelected();

   private TParentTab parentTab_;
}
