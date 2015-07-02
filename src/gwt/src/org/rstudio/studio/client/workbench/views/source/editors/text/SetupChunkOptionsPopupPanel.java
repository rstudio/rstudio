/*
 * SetupChunkOptionsPopupPanel.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.TextBox;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.TriStateCheckBox;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SetupChunkOptionsPopupPanel extends ChunkOptionsPopupPanel
{
   @Inject
   public void initialize(CodeToolsServerOperations server)
   {
      server_ = server;
   }
   
   public SetupChunkOptionsPopupPanel()
   {
      super(false);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      figureDimensionsPanel_.setVisible(false);
      useCustomFigureCheckbox_.setVisible(false);
      
      setHeader("Default Chunk Options", true);
   }
   
   String indented(String text)
   {
      // Ace will automatically translate tabs into appropriate
      // indent on insertion
      return "\t" + text;
   }
   
   private int findEndOfChunk()
   {
      int row = position_.getRow();
      
      int max = widget_.getEditor().getSession().getLength();
      for (int i = row; i < max; i++)
      {
         String line = widget_.getEditor().getSession().getLine(i);
         if (line.equals("```"))
            return i;
      }
      
      return -1;
      
   }
   
   private String trueString(boolean value)
   {
      return value ? "TRUE" : "FALSE";
   }
   
   private void addParam(Map<String, String> options, String name)
   {
      if (has(name))
         options.put(name, get(name));
   }
   
   private void addCheckboxParam(Map<String, String> options,
                                 TriStateCheckBox checkBox,
                                 String name)
   {
      if (!checkBox.isIndeterminate())
         options.put(name, trueString(checkBox.isChecked()));
   }
   
   private void addTextBoxParam(Map<String, String> options,
                                TextBox textBox,
                                String name)
   {
      String value = textBox.getValue();
      if (!StringUtil.isNullOrEmpty(value))
         options.put(name, value);
   }
   
   private int findOptsChunkLine(int endRow)
   {
      for (int i = endRow; i >= 0; i--)
      {
         String line = widget_.getEditor().getSession().getLine(i);
         if (line.contains("opts_chunk$set"))
            return i;
      }
      return -1;
   }
   
   private void syncSelection()
   {
      int endRow = findEndOfChunk();
      if (endRow == -1)
         return;
      
      int startRow = findOptsChunkLine(endRow);
      if (startRow == -1)
      {
         widget_.getEditor().clearSelection();
         widget_.getEditor().moveCursorTo(endRow, 0);
      }
      else
      {
         Range range = Range.create(startRow, 0, endRow, 0);
         widget_.getEditor().getSession().getSelection().setSelectionRange(range);
      }
   }
   
   private String joinOptions(Map<String, String> options)
   {
      return StringUtil.collapse(options, " = ", ",\n\t");
   }
   
   private String getChunkText()
   {
      int chunkStart = position_.getRow();
      int chunkEnd = findEndOfChunk();
      JsArrayString chunkText =
            widget_.getEditor().getSession().getLines(chunkStart + 1, chunkEnd - 1);
      return chunkText.join("\n");
   }
   
   @Override
   protected void initOptions(final Command afterInit)
   {
      String chunkText = getChunkText();
      server_.extractChunkOptions(
            chunkText,
            new ServerRequestCallback<JsObject>()
            {
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
               
               @Override
               public void onResponseReceived(JsObject object)
               {
                  JsArrayString keys = object.keys();
                  for (String key : JsUtil.asIterable(keys))
                     chunkOptions_.put(key, object.getAsString(key));
                  afterInit.execute();
               }
            });
   }
   
   @Override
   protected void synchronize()
   {
      int endRow = findEndOfChunk();
      if (endRow == -1)
         return;
      
      syncSelection();
      
      Map<String, String> options = new LinkedHashMap<String, String>();
      
      Set<String> keys = chunkOptions_.keySet();
      for (String key : keys)
         options.put(key, chunkOptions_.get(key));
      
      addParam(options, "echo");
      addParam(options, "eval");
      addParam(options, "include");
      
      addCheckboxParam(options, showMessagesInOutputCb_, "message");
      addCheckboxParam(options, showWarningsInOutputCb_, "warning");
      
      addTextBoxParam(options, figHeightBox_, "fig.height");
      addTextBoxParam(options, figWidthBox_, "fig.width");
      
      if (options.isEmpty())
      {
         widget_.getEditor().insert("");
         return;
      }
      
      // For 2 or fewer options, display all on one line
      if (options.size() <= 2)
      {
         String joined = StringUtil.collapse(options, " = ", ", ");
         String code = "knitr::opts_chunk$set(" + joined + ")\n";
         widget_.getEditor().insert(code);
         return;
      }
      
      Map<String, String> sorted = sortedOptions(options);
      
      String code = "knitr::opts_chunk$set(\n\t";
      code += joinOptions(sorted);
      code += "\n)\n";
      
      widget_.getEditor().insert(code);
   }
   
   @Override
   protected void revert()
   {
   }
   
   private CodeToolsServerOperations server_;

}
