/*
 * EditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors;

import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.events.HasEnsureHeightHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.studio.client.common.ReadOnlyValue;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.common.filetypes.FileType;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.palette.model.CommandPaletteEntrySource;
import org.rstudio.studio.client.workbench.model.UnsavedChangesTarget;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetSource.EditingTargetNameProvider;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import java.util.HashSet;

public interface EditingTarget extends IsWidget,
                                       HasEnsureVisibleHandlers,
                                       HasEnsureHeightHandlers,
                                       HasCloseHandlers<Void>,
                                       UnsavedChangesTarget,
                                       CommandPaletteEntrySource
{
   String getId();
   
   /**
    * Used as the tab name
    */
   HasValue<String> getName();
   String getTitle();
   String getPath();
   String getContext();
   FileIcon getIcon();
   String getTabTooltip();
   
   FileType getFileType();
   TextFileType getTextFileType();

   void adaptToExtendedFileType(String extendedType);
   String getExtendedFileType();
   
   HashSet<AppCommand> getSupportedCommands();
   void manageCommands();
   boolean canCompilePdf();
   
   void verifyCppPrerequisites();
   void verifyPythonPrerequisites();
   void verifyD3Prerequisites();
   void verifyNewSqlPrerequisites();

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
   void navigateToPosition(SourcePosition position,
                           boolean recordCurrent,
                           boolean highlightLine,
                           boolean moveCursor,
                           Command onNavigationCompleted);

   void restorePosition(SourcePosition position);
   SourcePosition currentPosition();
   boolean isAtSourceRow(SourcePosition position);
   
   void forceLineHighlighting();
   
   void setSourceOnSave(boolean sourceOnSave);
   
   void setCursorPosition(Position position);
   void ensureCursorVisible();
   
   Position search(String regex);
   Position search(Position startPos, String regex);
   
   void highlightDebugLocation(
         SourcePosition startPos,
         SourcePosition endPos,
         boolean executing);   
   void endDebugHighlighting();
   
   void beginCollabSession(CollabEditStartParams params);
   void endCollabSession();
   
   /**
    * @return True if dismissal is allowed, false to cancel.
    */
   boolean onBeforeDismiss();
   void onDismiss(int dismissType);

   ReadOnlyValue<Boolean> dirtyState();
   
   boolean isSaveCommandActive();
   void forceSaveCommandActive();
   
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

   void initialize(SourceColumn column,
                   SourceDocument document,
                   FileSystemContext fileContext,
                   FileType type,
                   EditingTargetNameProvider defaultNameProvider);

   /**
    * Any bigger than this, and the file should NOT be allowed to open
    */
   long getFileSizeLimit();

   /**
    * Any bigger than this, and the user should be warned before opening
    */
   long getLargeFileSize();
   
   String getDefaultNamePrefix();

   /**
    * @return Summary of the pane's current state for screen readers (read out 
    * loud by user request)
    */
   public String getCurrentStatus();

   public final static int DISMISS_TYPE_CLOSE = 0;
   public final static int DISMISS_TYPE_MOVE = 1;
}
