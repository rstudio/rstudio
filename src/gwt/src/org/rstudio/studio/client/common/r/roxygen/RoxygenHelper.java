/*
 * RoxygenHelper.java
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
package org.rstudio.studio.client.common.r.roxygen;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.DocumentMode;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.WarningBarDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;

import java.util.ArrayList;
import java.util.Arrays;

public class RoxygenHelper
{
   public RoxygenHelper(DocDisplay docDisplay,
                        WarningBarDisplay view)
   {
      editor_ = (AceEditor) docDisplay;
      view_ = view;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   RoxygenServerOperations server)
   {
      server_ = server;
      globalDisplay_ = globalDisplay;
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
      
      // We check these first because we might lie within an
      // anonymous function scope, whereas what we first want
      // to check is for an enclosing `setGeneric` etc.
      TokenCursor cursor = getTokenCursor();
      if (cursor.moveToPosition(editor_.getCursorPosition(), true))
      {
         String enclosingScope = findEnclosingScope(cursor);
         
         if (enclosingScope == "setClass")
            insertRoxygenSkeletonS4Class(cursor);
         else if (enclosingScope == "setGeneric")
            insertRoxygenSkeletonSetGeneric(cursor);
         else if (enclosingScope == "setMethod")
            insertRoxygenSkeletonSetMethod(cursor);
         else if (enclosingScope == "setRefClass")
            insertRoxygenSkeletonSetRefClass(cursor);
            
         if (enclosingScope != null)
            return;
      }
      
      // If the above checks failed, we'll want to insert a
      // roxygen skeleton for a 'regular' function call.
      Scope scope = editor_.getCurrentScope();
      if (scope != null && scope.isFunction())
      {
         insertRoxygenSkeletonFunction(scope);
      }
      else
      {
         globalDisplay_.showErrorMessage(
             "Insert Roxygen Skeleton",
             "Unable to insert skeleton (the cursor is not currently " +
             "inside an R function definition).");
      }
   }
   
   private TokenCursor getTokenCursor()
   {
      return editor_.getSession().getMode().getCodeModel().getTokenCursor();
   }
   
   private String extractCall(TokenCursor cursor)
   {
      // Force document tokenization
      editor_.getSession().getMode().getCodeModel().tokenizeUpToRow(
            editor_.getSession().getDocument().getLength());
      
      TokenCursor clone = cursor.cloneCursor();
      final Position startPos = clone.currentPosition();
      
      if (!clone.moveToNextToken())
         return null;
      
      if (clone.currentValue() != "(")
         return null;
      
      if (!clone.fwdToMatchingToken())
         return null;
      
      Position endPos = clone.currentPosition();
      endPos.setColumn(endPos.getColumn() + 1);
      
      return editor_.getSession().getTextRange(
            Range.fromPoints(startPos, endPos));
   }
   
   private void insertRoxygenSkeletonSetRefClass(TokenCursor cursor)
   {
      final Position startPos = cursor.currentPosition();
      String call = extractCall(cursor);
      if (call == null)
         return;
      
      server_.getSetRefClassCall(
            call,
            new ServerRequestCallback<SetRefClassCall>()
            {
               @Override
               public void onResponseReceived(SetRefClassCall response)
               {
                  if (hasRoxygenBlock(startPos))
                  {
                     amendExistingRoxygenBlock(
                           startPos.getRow() - 1,
                           response.getClassName(),
                           response.getFieldNames(),
                           response.getFieldTypes(),
                           "field",
                           RE_ROXYGEN_FIELD);
                  }
                  else
                  {
                     insertRoxygenTemplate(
                           response.getClassName(),
                           response.getFieldNames(),
                           response.getFieldTypes(),
                           "field",
                           "reference class",
                           startPos);
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
               
            });
   }
   
   private void insertRoxygenSkeletonSetGeneric(TokenCursor cursor)
   {
      final Position startPos = cursor.currentPosition();
      String call = extractCall(cursor);
      if (call == null)
         return;
      
      server_.getSetGenericCall(
            call,
            new ServerRequestCallback<SetGenericCall>()
            {
               @Override
               public void onResponseReceived(SetGenericCall response)
               {
                  if (hasRoxygenBlock(startPos))
                  {
                     amendExistingRoxygenBlock(
                           startPos.getRow() - 1,
                           response.getGeneric(),
                           response.getParameters(),
                           null,
                           "param",
                           RE_ROXYGEN_PARAM);
                  }
                  else
                  {
                     insertRoxygenTemplate(
                           response.getGeneric(),
                           response.getParameters(),
                           null,
                           "param",
                           "generic function",
                           startPos);
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
               
            });
   }
   
   private void insertRoxygenSkeletonSetMethod(TokenCursor cursor)
   {
      final Position startPos = cursor.currentPosition();
      String call = extractCall(cursor);
      if (call == null)
         return;
      
      server_.getSetMethodCall(
            call,
            new ServerRequestCallback<SetMethodCall>()
            {
               @Override
               public void onResponseReceived(SetMethodCall response)
               {
                  if (hasRoxygenBlock(startPos))
                  {
                     amendExistingRoxygenBlock(
                           startPos.getRow() - 1,
                           response.getGeneric(),
                           response.getParameterNames(),
                           response.getParameterTypes(),
                           "param",
                           RE_ROXYGEN_PARAM);
                  }
                  else
                  {
                     insertRoxygenTemplate(
                           response.getGeneric(),
                           response.getParameterNames(),
                           response.getParameterTypes(),
                           "param",
                           "method",
                           startPos);
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
               
            });
   }
   
   
   private void insertRoxygenSkeletonS4Class(TokenCursor cursor)
   {
      final Position startPos = cursor.currentPosition();
      String setClassCall = extractCall(cursor);
      if (setClassCall == null)
         return;
      
      server_.getSetClassCall(
            setClassCall,
            new ServerRequestCallback<SetClassCall>()
            {
               @Override
               public void onResponseReceived(SetClassCall response)
               {
                  if (hasRoxygenBlock(startPos))
                  {
                     amendExistingRoxygenBlock(
                           startPos.getRow() - 1,
                           response.getClassName(),
                           response.getSlots(),
                           null,
                           "slot",
                           RE_ROXYGEN_SLOT);
                  }
                  else
                  {
                     insertRoxygenTemplate(
                           response.getClassName(),
                           response.getSlots(),
                           response.getTypes(),
                           "slot",
                           "S4 class",
                           startPos);
                  }
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private String findEnclosingScope(TokenCursor cursor)
   {
      if (ROXYGEN_ANNOTATABLE_CALLS.contains(cursor.currentValue()))
         return cursor.currentValue();
      
      // Check to see if we're on e.g. `x <- setRefClass(...)`.
      if (cursor.isLeftAssign() &&
          ROXYGEN_ANNOTATABLE_CALLS.contains(cursor.nextValue()))
      {
         cursor.moveToNextToken();
         return cursor.currentValue();
      }
      
      if (ROXYGEN_ANNOTATABLE_CALLS.contains(cursor.nextValue(2)))
      {
         cursor.moveToNextToken();
         cursor.moveToNextToken();
         return cursor.currentValue();
      }
         
      while (cursor.currentValue() ==")")
         if (!cursor.moveToPreviousToken())
            return null;
      
      while (cursor.findOpeningBracket("(", false))
      {
         if (!cursor.moveToPreviousToken())
            return null;
         
         if (ROXYGEN_ANNOTATABLE_CALLS.contains(cursor.currentValue()))
            return cursor.currentValue();
      }
      
      return null;
   }
   
   public void insertRoxygenSkeletonFunction(Scope scope)
   {
      // Attempt to find the bounds for the roxygen block
      // associated with this function, if it exists
      if (hasRoxygenBlock(scope.getPreamble()))
      {
         amendExistingRoxygenBlock(
               scope.getPreamble().getRow() - 1,
               getFunctionName(scope),
               getFunctionArgs(scope),
               null,
               "param",
               RE_ROXYGEN_PARAM);
      }
      else
      {
         insertRoxygenTemplate(
               getFunctionName(scope),
               getFunctionArgs(scope),
               null,
               "param",
               "function",
               scope.getPreamble());
      }
   }
   
   private void amendExistingRoxygenBlock(
         int row,
         String objectName,
         JsArrayString argNames,
         JsArrayString argTypes,
         String tagName,
         Pattern pattern)
   {
      // Get the range encompassing this Roxygen block.
      Range range = getRoxygenBlockRange(row);
      
      // Extract that block (as an array of strings)
      JsArrayString block = extractRoxygenBlock(
            editor_.getWidget().getEditor(),
            range);
      
      // If the block contains roxygen parameters that require
      // non-local information (e.g. @inheritParams), then
      // bail.
      for (int i = 0; i < block.length(); i++)
      {
         if (RE_ROXYGEN_NONLOCAL.test(block.get(i)))
         {
            view_.showWarningBar(
                  "Cannot automatically update roxygen blocks " +
                  "that are not self-contained.");
            return;
         }
      }
      
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
      JsArrayString paramsToRemove = setdiff(params, argNames);
      
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
      JsArrayInteger indices = setdiffIndices(argNames, params);
      
      // NOTE: modifies replacement
      insertNewTags(
            replacement,
            argNames,
            argTypes,
            indices,
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
         JsArrayString argNames,
         JsArrayString argTypes,
         JsArrayInteger indices,
         String roxygenDelim,
         String tagName,
         int position)
   /*-{
      
      var newRoxygenEntries = [];
      for (var i = 0; i < indices.length; i++) {
         
         var idx = indices[i];
         var arg = argNames[idx];
         var type = argTypes == null ? null : argTypes[idx];
         
         var entry = roxygenDelim + " @" + tagName + " " + arg + " ";
         
         if (type != null)
            entry += type = ". ";
            
         newRoxygenEntries.push(entry);
      }
         
      Array.prototype.splice.apply(
         array,
         [position, 0].concat(newRoxygenEntries)
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
   
   private static JsArrayInteger setdiffIndices(
         JsArrayString self,
         JsArrayString other)
   {
      JsArrayInteger result = JsArray.createArray().cast();
      for (int i = 0; i < self.length(); i++)
         if (!contains(other, self.get(i)))
            result.push(i);
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
   
   private void insertRoxygenTemplate(
         String name,
         JsArrayString argNames,
         JsArrayString argTypes,
         String argTagName,
         String type,
         Position position)
   {
      String roxygenParams = argsToExampleRoxygen(
            argNames,
            argTypes,
            argTagName);
      
      // Add some spacing between params and the next tags,
      // if there were one or more arguments.
      if (argNames.length() != 0)
         roxygenParams += "\n#'\n";
      
      String block = 
                  "#' Title\n" +
                  "#'\n" +
                  roxygenParams +
                  "#' @return\n" +
                  "#' @export\n" +
                  "#'\n" +
                  "#' @examples\n";
      
      Position insertionPosition = Position.create(
            position.getRow(),
            0);
      
      editor_.insertCode(insertionPosition, block);
   }
   
   private String argsToExampleRoxygen(
         JsArrayString argNames,
         JsArrayString argTypes,
         String tagName)
   {
      String roxygen = "";
      if (argNames.length() == 0) return "";
      
      if (argTypes == null)
      {
         roxygen += argToExampleRoxygen(argNames.get(0), null, tagName);
         for (int i = 1; i < argNames.length(); i++)
            roxygen += "\n" + argToExampleRoxygen(argNames.get(i), null, tagName);
      }
      else
      {
         roxygen += argToExampleRoxygen(argNames.get(0), argTypes.get(0), tagName);
         for (int i = 1; i < argNames.length(); i++)
            roxygen += "\n" + argToExampleRoxygen(argNames.get(i), argTypes.get(i), tagName);
      }
      
      return roxygen;
   }
   
   private String argToExampleRoxygen(String argName, String argType, String tagName)
   {
      String output = "#' @" + tagName + " " + argName + " ";
      if (argType != null)
         output += argType + ". ";
      return output;
   }
   
   public static class SetClassCall extends JavaScriptObject
   {
      protected SetClassCall() {}
      
      public final native String getClassName() /*-{ return this["Class"]; }-*/;
      public final native JsArrayString getSlots() /*-{ return this["slots"]; }-*/;
      public final native JsArrayString getTypes() /*-{ return this["types"]; }-*/;
      public final native int getNumSlots() /*-{ return this["slots"].length; }-*/;
   }
   
   public static class SetGenericCall extends JavaScriptObject
   {
      protected SetGenericCall() {}
      
      public final native String getGeneric() /*-{ return this["generic"]; }-*/;
      public final native JsArrayString getParameters() /*-{ return this["parameters"]; }-*/;
   }
   
   public static class SetMethodCall extends JavaScriptObject
   {
      protected SetMethodCall() {}
      
      public final native String getGeneric() /*-{ return this["generic"]; }-*/;
      public final native JsArrayString getParameterNames() /*-{ return this["parameter.names"]; }-*/;
      public final native JsArrayString getParameterTypes() /*-{ return this["parameter.types"]; }-*/;
   }
   
   public static class SetRefClassCall extends JavaScriptObject
   {
      protected SetRefClassCall() {}
      
      public final native String getClassName() /*-{ return this["Class"]; }-*/;
      public final native JsArrayString getFieldNames() /*-{ return this["field.names"]; }-*/;
      public final native JsArrayString getFieldTypes() /*-{ return this["field.types"]; }-*/;
   }
   
   private final AceEditor editor_;
   private final WarningBarDisplay view_;
   private GlobalDisplay globalDisplay_;
   
   private RoxygenServerOperations server_;
   
   private static final Pattern RE_ROXYGEN =
         Pattern.create("^(\\s*#+')", "");
   
   private static final Pattern RE_ROXYGEN_EMPTY =
         Pattern.create("^\\s*#+'\\s*$", "");
   
   private static final Pattern RE_ROXYGEN_PARAM =
         Pattern.create("^\\s*#+'\\s*@param\\s+([^\\s]+)", "");
   
   private static final Pattern RE_ROXYGEN_FIELD =
         Pattern.create("^\\s*#+'\\s*@field\\s+([^\\s]+)", "");
   
   private static final Pattern RE_ROXYGEN_SLOT =
         Pattern.create("^\\s*#+'\\s*@slot\\s+([^\\s]+)", "");
   
   private static final Pattern RE_ROXYGEN_WITH_TAG =
         Pattern.create("^\\s*#+'\\s*@[^@]", "");
   
   private static final ArrayList<String> ROXYGEN_ANNOTATABLE_CALLS =
      new ArrayList<String>(
            Arrays.asList(new String[] {
            "setClass",
            "setRefClass",
            "setMethod",
            "setGeneric"
      }));
   
   private static final Pattern RE_ROXYGEN_NONLOCAL =
         Pattern.create("^\\s*#+'\\s*@(?:inheritParams|template)", "");
}
