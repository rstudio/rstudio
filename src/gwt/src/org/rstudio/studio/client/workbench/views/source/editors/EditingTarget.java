/*
 * EditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors;

import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Provider;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.HashSet;

public interface EditingTarget extends IsWidget,
                                       HasEnsureVisibleHandlers,
                                       HasCloseHandlers<Void>,
                                       UnsavedChangesTarget
{
   String getId();

   /**
    * Used as the tab name
    */
   HasValue<String> getName();
   String getTitle();
   String getPath();
   String getContext();
   ImageResource getIcon();
   String getTabTooltip();

   HashSet<AppCommand> getSupportedCommands();
   boolean canCompilePdf();
   
   void verifyPrerequisites();

   void focus();
   void onActivate();
   void onDeactivate();

   void onInitiallyLoaded();

   void recordCurrentNavigationPosition();
   void navigateToPosition(SourcePosition position, 
                           boolean recordCurrent);
   void navigateToPosition(SourcePosition position, 
                           boolean recordCurrent,
                           boolean highlightLine);
   void restorePosition(SourcePosition position);
   boolean isAtSourceRow(SourcePosition position);
   
   void setCursorPosition(Position position);
   
   Position search(String regex);
        
   
   /**
    * @return True if dismissal is allowed, false to cancel.
    */
   boolean onBeforeDismiss();
   void onDismiss();

   ReadOnlyValue<Boolean> dirtyState();
   
   boolean isSaveCommandActive();
   
   /**
    * Save the document, prompting only if the file is dirty and untitled
    */
   void save(Command onCompleted);
   
   /**
    * Save the document, always prompting if the file is dirty
    */
   void saveWithPrompt(Command onCompleted, Command onCancelled);
   
   /**
    * Revert any changes
    */
   void revertChanges(Command onCompleted);

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
