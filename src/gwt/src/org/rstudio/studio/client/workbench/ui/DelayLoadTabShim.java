/*
 * DelayLoadTabShim.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;

public abstract class DelayLoadTabShim<T extends IsWidget,
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
      final Widget child = obj.asWidget();

      if (child instanceof HasEnsureVisibleHandlers)
      {
         ((HasEnsureVisibleHandlers)child).addEnsureVisibleHandler(event ->
         {
            parentTab_.ensureVisible(event.getActivate());
         });
      }

      if (child instanceof HasEnsureHeightHandlers)
      {
         ((HasEnsureHeightHandlers)child).addEnsureHeightHandler(heightEvent ->
         {
            parentTab_.ensureHeight(heightEvent.getHeight());
         });
      }

      if (child instanceof HasEnsureHiddenHandlers)
      {
         ((HasEnsureHiddenHandlers)child).addEnsureHiddenHandler(event ->
         {
            parentTab_.ensureHidden();
         });
      }

      child.setSize("100%", "100%");
      parentTab_.getPanel().add(child);
   }

   public abstract void onBeforeUnselected();
   public abstract void onBeforeSelected();
   public abstract void onSelected();
   public abstract void setFocus();

   private TParentTab parentTab_;
}
