/*
 * SourceDisplayManager.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.events.*;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DocTabLayoutPanel;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.BeforeShowCallback;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.AutoGlassAttacher;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.ui.unsaved.UnsavedChangesDialog;
import org.rstudio.studio.client.workbench.views.source.Source.Display;
import java.util.ArrayList;

public class SourceDisplayManager extends Composite
   implements HasEnsureVisibleHandlers, HasEnsureHeightHandlers
{
   public SourceDisplayManager(SourcePane pane,
                               Display display)
   {
      pane_ =  pane;
      display_ = display;

      final Widget child = display_.asWidget();
      if (child instanceof HasEnsureVisibleHandlers)
      {
         ((HasEnsureVisibleHandlers)child).addEnsureVisibleHandler(
            new EnsureVisibleHandler()
            {
               @Override
               public void onEnsureVisible(EnsureVisibleEvent event)
               {
                  pane_.fireEvent(event);
               }
            });
      }
      if (child instanceof HasEnsureHeightHandlers)
      {
         ((HasEnsureHeightHandlers)child).addEnsureHeightHandler(
            new EnsureHeightHandler()
            {
            
               @Override
               public void onEnsureHeight(EnsureHeightEvent event)
               {
                  pane_.fireEvent(event);
               }
            });
      }
      child.setSize("100%", "100%");
      pane_.addToPanel(child);
   }

   public SourcePane sourcePane()
   {
      return pane_;
   }

   public Display display()
   {
      return display_;
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleHandler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   public HandlerRegistration addEnsureHeightHandler(EnsureHeightHandler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   public Widget asWidget()
   {
      return this;
   }

   public void onBeforeShow()
   {
      pane_.onBeforeShow();
   }

   public void loadPanels()
   {
   }

   private SourcePane pane_;
   private Display display_;
}
