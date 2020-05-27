/*
 * CompletionPopupDisplay.java
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
package org.rstudio.studio.client.workbench.views.console.shell.assist;

import java.util.Map;

import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionRequester.QualifiedName;
import org.rstudio.studio.client.workbench.views.help.model.HelpInfo;

public interface CompletionPopupDisplay 
                     extends HasSelectionCommitHandlers<QualifiedName>,
                             HasSelectionHandlers<QualifiedName>,
                             HasCloseHandlers<PopupPanel>,
                             HasMouseDownHandlers,
                             HasAttachHandlers
{
   void clearCompletions();
   void showCompletionValues(QualifiedName[] results,
                             PositionCallback callback,
                             boolean truncated);
   
   void showErrorMessage(String userMessage, PositionCallback callback);
   void hide();
   boolean isShowing();

   void setPopupPosition(int x, int y);
   void placeOffscreen();
   boolean isOffscreen();
   int getPopupLeft();
   int getPopupTop();
   
   int getOffsetHeight();

   QualifiedName getSelectedValue();
   QualifiedName getLastSelectedValue();
   Rectangle getSelectionRect();

   boolean selectPrev();
   boolean selectNext();
   boolean selectPrevPage();
   boolean selectNextPage();
   boolean selectFirst();
   boolean selectLast();
   
   void setHelpVisible(boolean visible);
   void displayHelp(HelpInfo.ParsedInfo help);
   void displayParameterHelp(Map<String, String> map, String parameter);
   void displayPackageHelp(HelpInfo.ParsedInfo helpInfo);
   void displayDataHelp(HelpInfo.ParsedInfo helpInfo);
   void displaySnippetHelp(String contents);
   /**
    * Clear out the current help info
    * @param downloadOperationPending If true, the current value is being
    *    cleared in preparation for a new value that is being downloaded.
    *    Implementations may choose to show a progress indicator in this case.
    */
   void clearHelp(boolean downloadOperationPending);
   boolean isHelpVisible();
   
   boolean hasCompletions();
   int numAvailableCompletions();
   
   QualifiedName[] getItems();
}
