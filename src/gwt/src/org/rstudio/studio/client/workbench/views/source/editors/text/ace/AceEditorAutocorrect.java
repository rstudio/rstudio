/*
 * AceEditorAutocorrect.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.DocumentChangedEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class AceEditorAutocorrect
{
   public AceEditorAutocorrect(AceEditor editor)
   {
      editor_ = editor;
      dictionary_ = new SafeMap<String, String>();
      
      initDictionary();
      initHandlers();
   }
   
   private void initDictionary()
   {
      dictionary_.put("packaegs", "packages");
      dictionary_.put("packaeg", "package");
   }
    
   private void initHandlers()
   {
      editor_.addDocumentChangedHandler(new DocumentChangedEvent.Handler()
      {
         @Override
         public void onDocumentChanged(DocumentChangedEvent event)
         {
            AceDocumentChangeEventNative nativeEvent = event.getEvent();
            
            // ignore non-text insertions
            String action = nativeEvent.getAction();
            String text = nativeEvent.getText();
            if (!action.equals("insertText") || text.length() != 1)
               return;
            
            // bail if this was a word character insertion
            if (text.matches(RegexUtil.wordCharacter()))
               return;
            
            // get current word
            final Range wordRange = getCurrentWordRange();
            String word = editor_.getTextForRange(wordRange);
            
            // check to see if it lives in the dictionary
            if (!dictionary_.containsKey(word))
               return;
            
            // replace word
            final String corrected = dictionary_.get(word);
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  final Position cursorPos = editor_.getCursorPosition();
                  editor_.replaceRange(wordRange, corrected);
                  editor_.setCursorPosition(cursorPos);
               }
            });
         }
      });
   }
   
   private Range getCurrentWordRange()
   {
      Position cursorPos = editor_.getCursorPosition();
      int row = cursorPos.getRow();
      String line = editor_.getLine(row);
      
      int startIdx = cursorPos.getColumn() - 1;
      int endIdx = cursorPos.getColumn();
      
      for (; startIdx >= 0; startIdx--)
      {
         if (!Character.isLetterOrDigit(line.charAt(startIdx)))
         {
            startIdx++;
            break;
         }
      }
      
      return Range.create(row, startIdx, row, endIdx);
   }
   
   private final AceEditor editor_;
   private final SafeMap<String, String> dictionary_;
}
