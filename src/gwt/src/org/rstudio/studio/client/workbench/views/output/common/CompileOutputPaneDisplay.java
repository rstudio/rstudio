/*
 * CompileOutputPaneDisplay.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.common;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.studio.client.common.compile.CompileOutput;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.workbench.WorkbenchView;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.HasClickHandlers;

public interface CompileOutputPaneDisplay extends WorkbenchView, HasEnsureHiddenHandlers
{
   void ensureVisible(boolean activate);
   void compileStarted(String text);
   void showOutput(CompileOutput output, boolean scrollToBottom);
   void showErrors(JsArray<SourceMarker> errors);
   void clearAll();
   void compileCompleted();
   HasClickHandlers stopButton();
   HasClickHandlers showLogButton();
   HasSelectionCommitHandlers<CodeNavigationTarget> errorList();
   boolean isEffectivelyVisible();
   void scrollToBottom();
   void setHasLogs(boolean logs);
   void setCanStop(boolean canStop);
}
