/*
 * InlineEditorFactory.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.inject.Inject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceObjectInfo;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceServerOperations;

public class InlineEditorFactory
{
   @Inject
   public InlineEditorFactory(WorkspaceServerOperations server,
                             EventBus events,
                             GlobalDisplay globalDisplay)
   {
      server_ = server ;
      events_ = events ;
      globalDisplay_ = globalDisplay ;
   }

   public <T> InlineEditor<T> create(WorkspaceObjectInfo object,
                                    InlineEditor.Display<T> display)
   {
      return new InlineEditor<T>(object, server_, events_, display,
                                globalDisplay_) ;
   }

   private final WorkspaceServerOperations server_ ;
   private final EventBus events_ ;
   private final GlobalDisplay globalDisplay_ ;
}
