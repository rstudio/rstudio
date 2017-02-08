/*
 * ShellDisplay.java
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
package org.rstudio.studio.client.common.shell;

import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;

import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;

public interface ShellDisplay extends ShellOutputWriter, 
                                      CanFocus, 
                                      HasKeyPressHandlers,
                                      HasKeyUpHandlers
{
   void consoleWriteInput(String input, String console);
   void consoleWritePrompt(String prompt);
   void consolePrompt(String prompt, boolean showInput) ;
   void ensureInputVisible() ;
   InputEditorDisplay getInputEditorDisplay() ;
   void clearOutput() ;
   String processCommandEntry() ;
   int getCharacterWidth() ;
   boolean isPromptEmpty();
   String getPromptText();
   
   void setReadOnly(boolean readOnly);
   void setSuppressPendingInput(boolean suppressPendingInput);

   void playbackActions(RpcObjectList<ConsoleAction> actions);

   int getMaxOutputLines();
   void setMaxOutputLines(int maxLines);

   HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler);
   HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler);
   
   Widget getShellWidget();
}