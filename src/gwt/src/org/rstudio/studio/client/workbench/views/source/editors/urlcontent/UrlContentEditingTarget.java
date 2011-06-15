/*
 * UrlContentEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.urlcontent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.widget.Widgetable;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.HashSet;

public class UrlContentEditingTarget implements EditingTarget
{
   public interface Display extends Widgetable
   {
      void print();
   }

   interface MyBinder extends CommandBinder<Commands, UrlContentEditingTarget>
   {}

   @Inject
   public UrlContentEditingTarget(SourceServerOperations server,
                                  Commands commands,
                                  GlobalDisplay globalDisplay)
   {
      server_ = server;
      commands_ = commands;
      globalDisplay_ = globalDisplay;
   }

   public String getId()
   {
      return doc_.getId();
   }

   public HasValue<String> getName()
   {
      String title = getContentTitle();
      return new Value<String>(title);
   }

   public String getPath()
   {
      return null;
   }

   public ImageResource getIcon()
   {
      return FileIconResources.INSTANCE.iconText();
   }

   public String getTabTooltip()
   {
      return null;
   }

   public HashSet<AppCommand> getSupportedCommands()
   {
      HashSet<AppCommand> commands = new HashSet<AppCommand>();
      commands.add(commands_.printSourceDoc());
      commands.add(commands_.popoutDoc());
      return commands;
   }

   @Handler
   void onPrintSourceDoc()
   {
      view_.print();
   }

   @Handler
   void onPopoutDoc()
   {
      globalDisplay_.openWindow(getContentUrl());
   }

   public void focus()
   {
   }

   public void onActivate()
   {
      if (commandReg_ != null)
      {
         Debug.log("Warning: onActivate called twice without intervening onDeactivate");
         commandReg_.removeHandler();
         commandReg_ = null;
      }
      commandReg_ = binder_.bind(commands_, this);
   }

   public void onDeactivate()
   {
      commandReg_.removeHandler();
      commandReg_ = null;
   }

   @Override
   public void onInitiallyLoaded()
   {
   }

   public boolean onBeforeDismiss()
   {
      return true;
   }

   public void onDismiss()
   {
      server_.removeContentUrl(getContentUrl(),
                               new ServerRequestCallback<org.rstudio.studio.client.server.Void>()
                               {
                                  @Override
                                  public void onError(ServerError error)
                                  {
                                     Debug.logError(error);
                                  }
                               });
   }

   protected String getContentTitle()
   {
      return getContentItem().getTitle();
   }

   protected String getContentUrl()
   {
      return getContentItem().getContentUrl();
   }

   public ReadOnlyValue<Boolean> dirtyState()
   {
      return dirtyState_;
   }
   
   public void save(Command onCompleted)
   {
      onCompleted.execute();
   }

   public void initialize(SourceDocument document,
                          FileSystemContext fileContext,
                          FileType type,
                          Provider<String> defaultNameProvider)
   {
      doc_ = document;
      view_ = createDisplay();
   }

   protected Display createDisplay()
   {
      return new UrlContentEditingTargetWidget(commands_,
                                                getContentUrl());
   }

   public long getFileSizeLimit()
   {
      return Long.MAX_VALUE;
   }

   public long getLargeFileSize()
   {
      return Long.MAX_VALUE;
   }

   public Widget toWidget()
   {
      return view_.toWidget();
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return new HandlerRegistration()
      {
         public void removeHandler()
         {
         }
      };
   }

   public HandlerRegistration addCloseHandler(
         CloseHandler<Void> voidCloseHandler)
   {
      return addEnsureVisibleHandler(null);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      assert false : "Not implemented";
   }

   private ContentItem getContentItem()
   {
      return (ContentItem)doc_.getProperties().cast();
   }

   protected SourceDocument doc_;
   private Value<Boolean> dirtyState_ = new Value<Boolean>(false);

   protected final SourceServerOperations server_;
   protected final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private Display view_;
   private HandlerRegistration commandReg_;

   private static final MyBinder binder_ = GWT.create(MyBinder.class);
}
