/*
 * CppCompletionManager.java
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

package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.NavigableSourceEditor;

import com.google.gwt.dom.client.NativeEvent;

public class CppCompletionManager implements CompletionManager
{
   public CppCompletionManager(InputEditorDisplay input,
                               NavigableSourceEditor navigableSourceEditor,
                               CompletionPopupDisplay popup,
                               InitCompletionFilter initFilter)
   {
      input_ = input;
      navigableSourceEditor_ = navigableSourceEditor;
      popup_ = popup;
      initFilter_ = initFilter;
   }

   // return false to indicate key not handled
   @Override
   public boolean previewKeyDown(NativeEvent event)
   {
      return false;
   }

   // return false to indicate key not handled
   @Override
   public boolean previewKeyPress(char charCode)
   {
      return false;
   }

   // go to help at the current cursor location
   @Override
   public void goToHelp()
   {
      
      
   }

   // find the definition of the function at the current cursor location
   @Override
   public void goToFunctionDefinition()
   {
      
      
   }

   // perform completion at the current cursor location
   @Override
   public void codeCompletion()
   {
      if (initFilter_ == null || initFilter_.shouldComplete(null))
      {
         
         
      }
   }

   // close the completion popup (if any)
   @Override
   public void close()
   {
      popup_.hide();
      
   }
   
   @SuppressWarnings("unused")
   private final InputEditorDisplay input_ ;
   @SuppressWarnings("unused")
   private final NavigableSourceEditor navigableSourceEditor_;
   private final CompletionPopupDisplay popup_ ;
   private final InitCompletionFilter initFilter_ ;

}
