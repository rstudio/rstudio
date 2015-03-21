/*
 * RoxygenHelper.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.common.r.roxygen;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;

public class RoxygenHelper
{
   public RoxygenHelper(DocDisplay docDisplay)
   {
      editor_ = (AceEditor) docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(RoxygenServerOperations server)
   {
      server_ = server;
   }
   
   private static native final String getFunctionName(Scope scope)
   /*-{
      return scope.attributes.name;
   }-*/;
   
   private static native final JsArrayString getFunctionArgs(Scope scope)
   /*-{
      return scope.attributes.args;
   }-*/;
   
   public void insertRoxygenSkeleton()
   {
      if (!DocumentMode.isCursorInRMode(editor_))
         return;
      
      Scope scope = editor_.getCurrentScope();
      if (scope != null && scope.isFunction())
      {
         insertRoxygenSkeletonFunction(scope);
         return;
      }
      
      TokenCursor cursor = getTokenCursor();
      if (cursorLiesWithinSetClass(cursor))
      {
         insertRoxygenSkeletonS4Class(cursor);
         return;
      }
   }
   
   private TokenCursor getTokenCursor()
   {
      return editor_.getSession().getMode().getCodeModel().getTokenCursor();
   }
   
   private void insertRoxygenSkeletonS4Class(TokenCursor cursor)
   {
      final Position startPos = cursor.currentPosition();
      if (!cursor.moveToNextToken())
         return;
      
      if (!cursor.currentValue().equals("("))
         return;
      
      if (!cursor.fwdToMatchingToken())
         return;
      
      Position endPos = cursor.currentPosition();
      endPos.setColumn(endPos.getColumn() + 1);
      
      String setClassCall = editor_.getSession().getTextRange(
            Range.fromPoints(startPos, endPos));
      
      server_.getSetClassSlots(
            setClassCall,
            new ServerRequestCallback<S4Slots>()
            {
               @Override
               public void onResponseReceived(S4Slots response)
               {
                  if (hasRoxygenBlock(startPos))
                  {
                     amendExistingRoxygenBlock(
                           startPos.getRow() - 1,
                           response.getClassName(),
                           response.getSlots(),
                           "slot",
                           RE_ROXYGEN_SLOT);
                  }
                  else
                  {
                     insertRoxygenTemplateForS4Class(
                           startPos,
                           response);
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private String slotsToExampleRoxygen(S4Slots slots)
   {
      if (slots.getNumSlots() == 0)
         return "";
      
      String output = slotAsRoxygen(slots, 0) + "\n";
      for (int i = 1; i < slots.getNumSlots(); i++)
         output += slotAsRoxygen(slots, i) + "\n";
      return output;
   }
   
   private String slotAsRoxygen(S4Slots slots, int index)
   {
      String slot = slots.getSlots().get(index);
      String type = slots.getTypes().get(index);
      
      return 
            "#' @slot " + 
            slot + 
            " " +
            type + ". " +
            "Description of slot '" + type + "'.";
   }
   
   private void insertRoxygenTemplateForS4Class(
         Position startPos,
         S4Slots slots)
   {
      String className = slots.getClassName();
      String slotDescriptions = slotsToExampleRoxygen(slots);
      
      // Add some spacing between params and the next tags,
      // if there were one or more arguments.
      if (slots.getSlots().length() != 0)
         slotDescriptions += "#'\n";
      
      String block = 
                  "#' Class '" + className + "'\n" +
                  "#'\n" +
                  "#' Provide a description of your S4 class in the\n" +
                  "#' first paragraph. It can span multiple lines.\n" +
                  "#'\n" +
                  "#' Provide extra details (if necessary) about the usage\n" +
                  "#' of your class in the second paragraph.\n" +
                  "#'\n" +
                  slotDescriptions +
                  "#' @export\n" +
                  "#'\n" +
                  "#' @examples\n" +
                  "#' ## How is '" + className + "' used?\n"
       ;
      
      editor_.insertCode(startPos, block);
   }
   
   
   // NOTE: Sets 'setClassPos' on success
   private boolean cursorLiesWithinSetClass(TokenCursor cursor)
   {
      if (!cursor.moveToPositionRightInclusive(editor_.getCursorPosition()))
         return false;
      
      if (cursor.currentValue().equals("setClass"))
         return true;
      
      if (cursor.currentValue().equals(")"))
         if (!cursor.bwdToMatchingToken())
            return false;
      
      while (cursor.findOpeningBracket("(", false))
      {
         if (!cursor.moveToPreviousToken())
            return false;
         
         if (cursor.currentValue().equals("setClass"))
            return true;
      }
      
      return false;
   }
   
   public void insertRoxygenSkeletonFunction(Scope scope)
   {
      // Attempt to find the bounds for the roxygen block
      // associated with this function, if it exists
      if (hasRoxygenBlock(scope.getPreamble()))
         amendExistingRoxygenBlock(
               scope.getPreamble().getRow() - 1,
               getFunctionName(scope),
               getFunctionArgs(scope),
               "param",
               RE_ROXYGEN_PARAM);
      else
         insertRoxygenTemplateForScope(scope);
   }
   
   private void amendExistingRoxygenBlock(
         int row,
         String objectName,
         JsArrayString objectArgs,
         String tagName,
         Pattern pattern)
   {
      // Get the range encompassing this Roxygen block.
      Range range = getRoxygenBlockRange(row);
      
      // Extract that block (as an array of strings)
      JsArrayString block = extractRoxygenBlock(
            editor_.getWidget().getEditor(),
            range);
      
      String roxygenDelim = RE_ROXYGEN.match(block.get(0), 0).getGroup(1);
      
      // The replacement block (we build by munging parts of
      // the old block
      JsArrayString replacement = JsArray.createArray().cast();
      
      // Scan through the block to get the names of
      // pre-existing parameters.
      JsArrayString params = listParametersInRoxygenBlock(
            block,
            pattern);
      
      // Figure out what parameters need to be removed, and remove them.
      // Any parameter not mentioned in the current function's argument list
      // should be stripped out.
      JsArrayString paramsToRemove = setdiff(params, objectArgs);
      
      int blockLength = block.length();
      for (int i = 0; i < blockLength; i++)
      {
         // If we encounter a param we don't want to extract, then
         // move over it.
         Match match = pattern.match(block.get(i), 0);
         if (match != null && contains(paramsToRemove, match.getGroup(1)))
         {
            i++;
            while (i < blockLength && !RE_ROXYGEN_WITH_TAG.test(block.get(i)))
               i++;
            
            i--;
            continue;
         }
         
         replacement.push(block.get(i));
      }
      
      // Now, add example roxygen for any parameters that are
      // present in the function prototype, but not present
      // within the roxygen block.
      int insertionPosition = findParamsInsertionPosition(replacement, pattern);
      JsArrayString paramsToAdd = setdiff(objectArgs, params);
      
      // NOTE: modifies replacement
      insertNewTags(
            replacement,
            paramsToAdd,
            roxygenDelim,
            tagName,
            insertionPosition);
      
      // Ensure space between final param and next tag
      ensureSpaceBetweenFirstParamAndPreviousEntry(replacement, roxygenDelim, pattern);
      ensureSpaceBetweenFinalParamAndNextTag(replacement, roxygenDelim, pattern);
      
      // Apply the replacement.
      editor_.getSession().replace(range, replacement.join("\n") + "\n");
   }
   
   private void ensureSpaceBetweenFirstParamAndPreviousEntry(
         JsArrayString replacement,
         String roxygenDelim,
         Pattern pattern)
   {
      int n = replacement.length();
      for (int i = 1; i < n; i++)
      {
         if (pattern.test(replacement.get(i)))
         {
            if (!RE_ROXYGEN_EMPTY.test(replacement.get(i - 1)))
               spliceIntoArray(replacement, roxygenDelim, i);
            return;
         }
      }
   }
   
   private void ensureSpaceBetweenFinalParamAndNextTag(
         JsArrayString replacement,
         String roxygenDelim,
         Pattern pattern)
   {
      int n = replacement.length();
      for (int i = n - 1; i >= 0; i--)
      {
         if (pattern.test(replacement.get(i)))
         {
            i++;
            if (i < n && RE_ROXYGEN_WITH_TAG.test(replacement.get(i)))
            {
               spliceIntoArray(replacement, roxygenDelim, i);
            }
            return;
         }
      }
   }
   
   private static final native void spliceIntoArray(
         JsArrayString array,
         String string,
         int pos)
   /*-{
      array.splice(pos, 0, string);
   }-*/;
   
   private static final native void insertNewTags(
         JsArrayString array,
         JsArrayString toInsert,
         String roxygenDelim,
         String tagName,
         int position)
   /*-{
      
      for (var i = 0; i < toInsert.length; i++)
         toInsert[i] = 
            roxygenDelim +
            " @" + tagName + " " + toInsert[i] + 
            " Description of '" + toInsert[i] + "'.";
         
      Array.prototype.splice.apply(
         array,
         [position, 0].concat(toInsert)
      );
      
   }-*/;
   
   private int findParamsInsertionPosition(
         JsArrayString block,
         Pattern pattern)
   {
      // Try to find the last '@param' block, and insert after that.
      int n = block.length();
      for (int i = n - 1; i >= 0; i--)
      {
         if (pattern.test(block.get(i)))
         {
            i++;
            
            // Move up to the next tag (or end)
            while (i < n && !RE_ROXYGEN_WITH_TAG.test(block.get(i)))
               i++;
            
            return i - 1;
         }
      }
      
      // Try to find the first tag, and insert before that.
      for (int i = 0; i < n; i++)
         if (RE_ROXYGEN_WITH_TAG.test(block.get(i)))
            return i;
      
      // Just insert at the end
      return block.length();
   }
   
   private static JsArrayString setdiff(
         JsArrayString self,
         JsArrayString other)
   {
      JsArrayString result = JsArray.createArray().cast();
      for (int i = 0; i < self.length(); i++)
         if (!contains(other, self.get(i)))
            result.push(self.get(i));
      return result;
   }
   
   private static native final boolean contains(
         JsArrayString array,
         String object)
   /*-{ 
      for (var i = 0, n = array.length; i < n; i++)
         if (array[i] === object)
            return true;
      return false;
   }-*/;
         
   
   private static native final JsArrayString extractRoxygenBlock(
         AceEditorNative editor,
         Range range)
   /*-{
      var lines = editor.getSession().getDocument().$lines;
      return lines.slice(range.start.row, range.end.row);
   }-*/;
   
   private JsArrayString listParametersInRoxygenBlock(
         JsArrayString block,
         Pattern pattern)
   {
      JsArrayString roxygenParams = JsArrayString.createArray().cast();
      for (int i = 0; i < block.length(); i++)
      {
         String line = block.get(i);
         Match match = pattern.match(line, 0);
         if (match != null)
            roxygenParams.push(match.getGroup(1));
      }
      
      return roxygenParams;
   }
   
   private Range getRoxygenBlockRange(int row)
   {
      while (row >= 0 && StringUtil.isWhitespace(editor_.getLine(row)))
         row--;
      
      int blockEnd = row + 1;
      
      while (row >= 0 && RE_ROXYGEN.test(editor_.getLine(row)))
         row--;
      
      if (row == 0 && !RE_ROXYGEN.test(editor_.getLine(row)))
         row++;
      
      int blockStart = row + 1;
      
      return Range.fromPoints(
            Position.create(blockStart, 0),
            Position.create(blockEnd, 0));
   }
   
   private boolean hasRoxygenBlock(Position position)
   {
      int row = position.getRow() - 1;
      if (row < 0) return false;
      
      // Skip whitespace.
      while (row >= 0 && StringUtil.isWhitespace(editor_.getLine(row)))
         row--;
      
      // Check if we landed on an Roxygen block.
      return RE_ROXYGEN.test(editor_.getLine(row));
   }
   
   private void insertRoxygenTemplateForScope(Scope scope)
   {
      String fnName = getFunctionName(scope);
      JsArrayString fnArgs = getFunctionArgs(scope);
      
      String roxygenParams = argsToExampleRoxygen(fnArgs);
      
      // Add some spacing between params and the next tags,
      // if there were one or more arguments.
      if (fnArgs.length() != 0)
         roxygenParams += "\n#'\n";
      
      String block = 
                  "#' Function '" + fnName + "'\n" +
                  "#'\n" +
                  "#' Provide a description of your function in the\n" +
                  "#' first paragraph. It can span multiple lines.\n" +
                  "#'\n" +
                  "#' Provide extra details (if necessary) about the usage\n" +
                  "#' of your function in the second paragraph.\n" +
                  "#'\n" +
                  roxygenParams +
                  "#' @return What does the function return?\n" +
                  "#' @export\n" +
                  "#'\n" +
                  "#' @examples\n" +
                  "#' ## How is '" + fnName + "' used?\n"
       ;
      
      editor_.insertCode(scope.getPreamble(), block);
   }
   
   private String argsToExampleRoxygen(JsArrayString fnArgs)
   {
      if (fnArgs.length() == 0) return "";
      String roxygen = argToExampleRoxygen(fnArgs.get(0));
      for (int i = 1; i < fnArgs.length(); i++)
         roxygen += "\n" + argToExampleRoxygen(fnArgs.get(i));
      return roxygen;
   }
   
   private String argToExampleRoxygen(String arg)
   {
      return "#' @param " + arg + " Description of '" + arg + "'";
   }
   
   public static class S4Slots extends JavaScriptObject
   {
      protected S4Slots() {}
      
      public final native String getClassName() /*-{ return this["className"]; }-*/;
      public final native JsArrayString getSlots() /*-{ return this["slots"]; }-*/;
      public final native JsArrayString getTypes() /*-{ return this["types"]; }-*/;
      public final native int getNumSlots() /*-{ return this["slots"].length; }-*/;
   }
   
   private final AceEditor editor_;
   private RoxygenServerOperations server_;
   
   private static final Pattern RE_ROXYGEN =
         Pattern.create("^(\\s*#+')", "");
   
   private static final Pattern RE_ROXYGEN_EMPTY =
         Pattern.create("^\\s*#+'\\s*$", "");
   
   private static final Pattern RE_ROXYGEN_PARAM =
         Pattern.create("^\\s*#+'\\s*@param\\s+([^\\s]+)", "");
   
   private static final Pattern RE_ROXYGEN_SLOT =
         Pattern.create("^\\s*#+'\\s*@slot\\s+([^\\s]+)", "");
   
   private static final Pattern RE_ROXYGEN_WITH_TAG =
         Pattern.create("^\\s*#+'\\s*@[^@]", "");
}
