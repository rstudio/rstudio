/*
 * ObjectExplorerEditingTarget.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.model.ObjectExplorerHandle;
import org.rstudio.studio.client.workbench.views.source.editors.explorer.view.ObjectExplorerEditingTargetWidget;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

public class ObjectExplorerEditingTarget
      extends UrlContentEditingTarget
{
   @Inject
   public ObjectExplorerEditingTarget(SourceServerOperations server,
                                      Commands commands,
                                      GlobalDisplay globalDisplay,
                                      EventBus events)
   {
      super(server, commands, globalDisplay, events);
      events_ = events;
      isActive_ = false;
   }
   
   // Implementation ----

   @Override
   protected Display createDisplay()
   {
      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setSize("100%", "100%");
      reloadDisplay();
      return new Display()
      {
         public void print()
         {
            ((Display)progressPanel_.getWidget()).print();
         }

         public Widget asWidget()
         {
            return progressPanel_;
         }
      };
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      view_.onActivate();
      isActive_ = true;
   }

   @Override
   public void onDeactivate()
   {
      super.onDeactivate();
      view_.onDeactivate();
      isActive_ = false;
   }
   
   @Override
   public String getPath()
   {
      return getHandle().getPath();
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(FileIconResources.INSTANCE.iconObjectExplorer2x());
   }

   private ObjectExplorerHandle getHandle()
   {
      return doc_.getProperties().cast();
   }

   @Override
   protected String getContentTitle()
   {
      return getHandle().getTitle();
   }

   @Override
   protected String getContentUrl()
   {
      return getHandle().getPath();
   }

   @Override
   public void popoutDoc()
   {
      events_.fireEvent(new PopoutDocEvent(getId(), null));
   }
   
   // Public methods ----
   
   public void update(ObjectExplorerHandle handle)
   {
      if (isActive_)
      {
         // TODO
      }
   }
   
   // Private methods ----
   
   private void reloadDisplay()
   {
      if (view_ != null)
      {
         view_.removeFromParent();
         view_ = null;
      }
      
      view_ = new ObjectExplorerEditingTargetWidget(getHandle(), doc_);
      view_.setSize("100%", "100%");
      progressPanel_.setWidget(view_);
   }
   

   private SimplePanelWithProgress progressPanel_;
   private ObjectExplorerEditingTargetWidget view_;
   private final EventBus events_;
   private boolean isActive_;
}
