/*
 * DelayLoadWorkbenchTab.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.events.EnsureHiddenEvent;
import org.rstudio.core.client.events.EnsureHiddenHandler;
import org.rstudio.core.client.events.EnsureMaximizedEvent;
import org.rstudio.core.client.events.EnsureMaximizedHandler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.studio.client.RStudioGinjector;

public abstract class DelayLoadWorkbenchTab<T extends IsWidget>
      implements WorkbenchTab
{
   protected DelayLoadWorkbenchTab(
         String title,
         DelayLoadTabShim<T, ? extends DelayLoadWorkbenchTab<T>> shimmed)
   {
      title_ = title;
      panel_ = new DockLayoutPanel(Style.Unit.PX);
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
   
   public void ensureMaximized()
   {
      handlers_.fireEvent(new EnsureMaximizedEvent());
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return handlers_.addHandler(EnsureVisibleEvent.TYPE, handler);
   }

   public HandlerRegistration addEnsureHiddenHandler(EnsureHiddenHandler handler)
   {
      return handlers_.addHandler(EnsureHiddenEvent.TYPE, handler);
   }
   
   public HandlerRegistration addEnsureMaximizedHandler(EnsureMaximizedHandler handler)
   {
      return handlers_.addHandler(EnsureMaximizedEvent.TYPE, handler);
   }

   protected void setInternalCallbacks(InternalCallbacks callbacks)
   {
      callbacks_ = callbacks;
   }

   protected interface InternalCallbacks
   {

      void onBeforeSelected();

      void onSelected();

   }

   protected void initialize(final WorkbenchPane pane, Panel panel)
   {
      assert !initialized_;

      initialized_ = true;

      pane.ensureWidget();
      panel.add(pane);
      pane.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            ensureVisible(event.getActivate());
         }
      });
      pane.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            ensureHidden();
         }
      });

      setInternalCallbacks(new InternalCallbacks()
      {
         public void onBeforeSelected()
         {
            pane.onBeforeSelected();
         }

         public void onSelected()
         {
            pane.onSelected();
         }
      });

      pane.onBeforeSelected();
      pane.onSelected();
   }

   protected void handleCodeLoadFailure(Throwable reason)
   {
      RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
            "Code Failed to Load",
            reason == null ? "(Unknown error)" : reason.getMessage());
   }

   private final HandlerManager handlers_ = new HandlerManager(null);
   @SuppressWarnings("unused")
   private SerializedCommandQueue initQueue = new SerializedCommandQueue();
   private boolean initialized_;
   protected final DockLayoutPanel panel_;
   private final String title_;
   @SuppressWarnings("unused")
   private InternalCallbacks callbacks_;
   private DelayLoadTabShim<T, ?> shimmed_;
}