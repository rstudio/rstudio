/*
 * ObjectExplorerEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.explorer;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
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
      fileType_ = FileTypeRegistry.OBJECT_EXPLORER;
      events_ = events;
      isActive_ = false;
   }
   
   // Implementation ----

   @Override
   protected Display createDisplay()
   {
      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setSize("100%", "100%");
      Roles.getTabpanelRole().set(progressPanel_.getElement());
      setAccessibleName(null);
      reloadDisplay();
      return new Display()
      {
         public void print()
         {
            ((Display)progressPanel_.getWidget()).print();
         }

         public void setAccessibleName(String name)
         {
            ObjectExplorerEditingTarget.this.setAccessibleName(name);
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
   public void onDismiss(int dismissType)
   {
      // explicitly avoid calling super method as we don't
      // have an associated content URL to clean up
   }
   
   @Override
   public String getPath()
   {
      return getHandle().getPath();
   }

   @Override
   public FileIcon getIcon()
   {
      return FileIcon.OBJECT_EXPLORER_ICON;
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
      events_.fireEvent(new PopoutDocEvent(getId(), null, null));
   }
   
   @Override
   public FileType getFileType()
   {
      return fileType_;
   }
   
   // Public methods ----
   
   public void update(ObjectExplorerHandle handle)
   {
      if (isActive_)
      {
         reloadDisplay();
      }
      
      view_.refresh();
   }

   @Override
   public String getCurrentStatus()
   {
      return "Object Explorer displayed";
   }

   // Private methods ----
   
   private void reloadDisplay()
   {
      if (view_ != null)
      {
         view_.removeFromParent();
         view_ = null;
      }
      
      view_ = new ObjectExplorerEditingTargetWidget(getHandle(), doc_, column_);
      view_.setSize("100%", "100%");
      progressPanel_.setWidget(view_);
   }

   private void setAccessibleName(String accessibleName)
   {
      if (StringUtil.isNullOrEmpty(accessibleName))
         accessibleName = "Untitled Object Explorer";
      Roles.getTabpanelRole().setAriaLabelProperty(progressPanel_.getElement(), accessibleName +
            " Object Explorer");
   }

   private SimplePanelWithProgress progressPanel_;
   private ObjectExplorerEditingTargetWidget view_;
   private final EventBus events_;
   private final FileType fileType_;
   private boolean isActive_;
}
