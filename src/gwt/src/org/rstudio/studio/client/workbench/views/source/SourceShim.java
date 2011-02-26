/*
 * SourceShim.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.widget.BeforeShowCallback;
import org.rstudio.core.client.widget.Widgetable;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileEvent;
import org.rstudio.studio.client.common.filetypes.events.OpenSourceFileHandler;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.events.*;

public class SourceShim extends Composite
   implements Widgetable, HasEnsureVisibleHandlers, BeforeShowCallback,
              ProvidesResize, RequiresResize, RequiresVisibilityChanged
{
   public interface Binder extends CommandBinder<Commands, AsyncSource> {}

   public abstract static class AsyncSource extends AsyncShim<Source>
      implements OpenSourceFileHandler, InsertSourceHandler, FileEditHandler
   {
      public abstract void onOpenSourceFile(OpenSourceFileEvent event);
      @Handler
      public abstract void onNewSourceDoc();
      @Handler
      public abstract void onOpenSourceDoc();
      @Handler
      public abstract void onCloseSourceDoc();
      @Handler
      public abstract void onCloseAllSourceDocs();
      @Handler
      public abstract void onActivateSource();
      @Handler
      public abstract void onPreviousTab();
      @Handler
      public abstract void onNextTab();
      @Handler
      public abstract void onFirstTab();
      @Handler
      public abstract void onLastTab();
      @Handler
      public abstract void onSwitchToTab();

      @Override
      protected void preInstantiationHook(Command continuation)
      {
         AceEditor.load(continuation);
      }

      @Override
      protected void onDelayLoadSuccess(final Source obj)
      {
         final Widget child = obj.toWidget();
         if (child instanceof HasEnsureVisibleHandlers)
         {
            ((HasEnsureVisibleHandlers)child).addEnsureVisibleHandler(
                  new EnsureVisibleHandler()
                  {
                     public void onEnsureVisible(EnsureVisibleEvent event)
                     {
                        parent_.fireEvent(new EnsureVisibleEvent());
                     }
                  });
         }
         child.setSize("100%", "100%");
         parent_.panel_.add(child);
         parent_.panel_.setWidgetTopBottom(child, 0, Unit.PX, 0, Unit.PX);
         parent_.panel_.setWidgetLeftRight(child, 0, Unit.PX, 0, Unit.PX);
      }

      public void setParent(SourceShim parent)
      {
         parent_ = parent;
      }

      private SourceShim parent_;
   }

   @Inject
   public SourceShim(AsyncSource asyncSource,
                     final Commands commands,
                     EventBus events,
                     Binder binder)
   {
      panel_ = new LayoutPanel();
      panel_.setSize("100%", "100%");
      initWidget(panel_);

      binder.bind(commands, asyncSource);
      asyncSource.setParent(this);
      events.addHandler(OpenSourceFileEvent.TYPE, asyncSource);
      events.addHandler(InsertSourceEvent.TYPE, asyncSource);
      asyncSource_ = asyncSource;

      events.fireEvent(new DocTabsChangedEvent(new String[0],
                                               new ImageResource[0],
                                               new String[0],
                                               new String[0]));

      events.addHandler(FileEditEvent.TYPE, asyncSource);
   }

   public Widget toWidget()
   {
      return this;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public void forceLoad()
   {
      asyncSource_.forceLoad(false);
      AceEditor.preload();
   }

   public void onBeforeShow()
   {
      for (Widget w : panel_)
         if (w instanceof BeforeShowCallback)
            ((BeforeShowCallback)w).onBeforeShow();
   }

   public void onResize()
   {
      panel_.onResize();
   }

   public void onVisibilityChanged(boolean visible)
   {
      for (Widget w : panel_)
         if (w instanceof RequiresVisibilityChanged)
            ((RequiresVisibilityChanged)w).onVisibilityChanged(visible);
   }

   private final LayoutPanel panel_;
   private AsyncSource asyncSource_;
}
