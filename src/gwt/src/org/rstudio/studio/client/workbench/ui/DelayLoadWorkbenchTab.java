/*
 * DelayLoadWorkbenchTab.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureVisibleEvent;

public abstract class DelayLoadWorkbenchTab<T extends IsWidget>
      implements WorkbenchTab
{
   protected DelayLoadWorkbenchTab(
         String title,
         DelayLoadTabShim<T, ? extends DelayLoadWorkbenchTab<T>> shimmed)
   {
      title_ = title;
      panel_ = new DockLayoutPanel(Style.Unit.PX);
      Roles.getTabpanelRole().set(panel_.getElement());
      Roles.getTabpanelRole().setAriaLabelProperty(panel_.getElement(), title_);

      // Assign a unique ID to the pane based on its title
      ElementIds.assignElementId(panel_.getElement(),
            ElementIds.WORKBENCH_PANEL + "_" + ElementIds.idSafeString(title_));

      shimmed_ = shimmed;
      shimmed_.setParentTab(this);
   }

   public Widget asWidget()
   {
      return panel_;
   }

   public String getTitle()
   {
      return title_;
   }

   public Panel getPanel()
   {
      return panel_;
   }

   public final void onBeforeSelected()
   {
      shimmed_.onBeforeSelected();
   }

   public void onBeforeUnselected()
   {
      shimmed_.onBeforeUnselected();
   }

   public void prefetch(final Command continuation)
   {
      shimmed_.forceLoad(true, continuation);
   }

   public final void onSelected()
   {
      shimmed_.onSelected();
   }

   public final void setFocus()
   {
      shimmed_.setFocus();
   }

   public boolean isSuppressed()
   {
      return false;
   }

   @Override
   public boolean closeable()
   {
      return false;
   }

   @Override
   public void confirmClose(Command onConfirmed)
   {
      onConfirmed.execute();
   }

   public void ensureVisible(boolean activate)
   {
      handlers_.fireEvent(new EnsureVisibleEvent(activate));
   }

   public void ensureHidden()
   {
      handlers_.fireEvent(new EnsureHiddenEvent());
   }

   public void ensureHeight(int height)
   {
      handlers_.fireEvent(new EnsureHeightEvent(height));
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return handlers_.addHandler(EnsureVisibleEvent.TYPE, handler);
   }

   public HandlerRegistration addEnsureHiddenHandler(EnsureHiddenEvent.Handler handler)
   {
      return handlers_.addHandler(EnsureHiddenEvent.TYPE, handler);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return handlers_.addHandler(EnsureHeightEvent.TYPE, handler);
   }

   private final HandlerManager handlers_ = new HandlerManager(null);
   protected final DockLayoutPanel panel_;
   private final String title_;
   private DelayLoadTabShim<T, ?> shimmed_;
}
