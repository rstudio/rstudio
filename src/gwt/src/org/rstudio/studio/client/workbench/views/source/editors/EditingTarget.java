/*
 * EditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors;

import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HasValue;
import com.google.inject.Provider;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.widget.Widgetable;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import java.util.HashSet;

public interface EditingTarget extends Widgetable,
                                       HasEnsureVisibleHandlers,
                                       HasCloseHandlers<Void>
{
   String getId();

   /**
    * Used as the tab name
    */
   HasValue<String> getName();
   String getPath();
   ImageResource getIcon();
   String getTabTooltip();

   HashSet<AppCommand> getSupportedCommands();

   void focus();
   void onActivate();
   void onDeactivate();

   void onInitiallyLoaded();

   /**
    * @return True if dismissal is allowed, false to cancel.
    */
   boolean onBeforeDismiss();
   void onDismiss();

   ReadOnlyValue<Boolean> dirtyState();

   void initialize(SourceDocument document,
                   FileSystemContext fileContext,
                   FileType type,
                   Provider<String> defaultNameProvider);

   /**
    * Any bigger than this, and the file should NOT be allowed to open
    */
   long getFileSizeLimit();

   /**
    * Any bigger than this, and the user should be warned before opening
    */
   long getLargeFileSize();
}
