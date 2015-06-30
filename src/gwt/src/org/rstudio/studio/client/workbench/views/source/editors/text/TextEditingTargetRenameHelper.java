package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JsArrayString;

import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class TextEditingTargetRenameHelper
{
   public TextEditingTargetRenameHelper(DocDisplay docDisplay)
   {
      editor_ = (AceEditor) docDisplay;
      state_ = new Stack<Integer>();
      newFnArgNames_ = new HashSet<String>();
      ranges_ = new ArrayList<Range>();
   }
   
   public void renameInFile()
   {
      init();
      TextFileType type = editor_.getFileType();
      if (type.isR() || type.isRhtml() || type.isRmd() || type.isRnw() || type.isRpres())
         renameInFileR();
   }
   
   private int renameInFileR()
   {
      Position position = editor_.hasSelection() ?
            editor_.getSelectionStart() :
            editor_.getCursorPosition();
            
      TokenCursor cursor = editor_.getSession().getMode().getCodeModel().getTokenCursor();
      if (!cursor.moveToPosition(position, true))
         return -1;
      
      editor_.setCursorPosition(position);
      
      // Validate that we're looking at an R identifier (TODO: refactor strings?)
      String targetValue = cursor.currentValue();
      String targetType = cursor.currentType();
      
      boolean isVariableType =
            targetType.equals("identifier") ||
            targetType.equals("keyword") ||
            targetType.equals("constant.language");
      
      if (!isVariableType)
         return -1;
      
      // Check to see if we're refactoring the name of an argument in a function call,
      // e.g.
      //
      //    if (foo(apple = 1)) { ... }
      //            ^^^^^
      //
      // If this is the case, then we want to search for all calls of this
      // form and rename just that argument.
      if (cursor.peekFwd(1).valueEquals("="))
      {
         String argName = cursor.currentValue();
         TokenCursor clone = cursor.cloneCursor();
         
         while (clone.findOpeningBracket("(", false))
         {
            if (!clone.moveToPreviousToken())
               break;
            
            String functionName = clone.currentValue();
            return renameFunctionArgument(functionName, argName);
            
         }
      }
      
      // Check to see if this is the name of a function argument. If so, we only want
      // to rename within that scope.
      Scope scope = editor_.getCurrentScope();
      while (!scope.isTopLevel())
      {
         if (scope.isFunction())
         {
            boolean isCursorOnFunctionDefn =
                  cursor.peek(1).isLeftAssign() &&
                  cursor.peek(2).getValue().equals("function");
            
            if (!isCursorOnFunctionDefn)
            {
               ScopeFunction scopeFunction = (ScopeFunction) scope;
               JsArrayString args = scopeFunction.getFunctionArgs();
               for (int i = 0; i < args.length(); i++)
                  if (args.get(i).equals(targetValue))
                     return renameVariablesInScope(scope, targetValue, targetType);
            }
         }
         scope = scope.getParentScope();
      }
      
      // Otherwise, just rename the variable within the current scope.
      // TODO: Do we need to look into parent scopes?
      return renameVariablesInScope(
            editor_.getCurrentScope(),
            targetValue,
            targetType);
   }
   
   private int renameFunctionArgument(String functionName, String argName)
   {
      TokenCursor cursor = editor_.getSession().getMode().getCodeModel().getTokenCursor();
      Stack<String> functionNames = new Stack<String>();
      boolean renaming = false;
      
      do
      {
         if (cursor.isLeftBracket())
         {
            if (cursor.valueEquals("(") &&
                cursor.peekBwd(1).isValidForFunctionCall())
            {
               String currentFunctionName = cursor.peekBwd(1).getValue();
               renaming = currentFunctionName.equals(functionName);
               functionNames.push(functionName);
               pushState(STATE_FUNCTION_CALL);
            }
            else
            {
               pushState(STATE_DEFAULT);
            }
         }
         
         if (cursor.isRightBracket())
         {
            popState();
            if (cursor.valueEquals(")") && !functionNames.empty())
            {
               functionNames.pop();
               renaming = !functionNames.empty() && functionNames.peek().equals(functionName);
            }
         }
         
         if (renaming &&
             peekState() == STATE_FUNCTION_CALL &&
             cursor.valueEquals(argName) &&
             cursor.peekFwd(1).valueEquals("="))
         {
            ranges_.add(getTokenRange(cursor));
         }
         
      } while (cursor.moveToNextToken());
      
      return applyRanges();
      
   }
   
   private int renameVariablesInScope(Scope scope, String targetValue, String targetType)
   {
      Position startPos = scope.getPreamble();
      Position endPos = scope.getEnd();
      
      // NOTE: Top-level scope does not have 'end' position
      if (scope.isTopLevel())
         endPos = Position.create(editor_.getSession().getLength(), 0);
      
      TokenCursor cursor = editor_.getSession().getMode().getCodeModel().getTokenCursor();
      if (!cursor.moveToPosition(startPos, true))
         return 0;
      
      // Workaround 'moveToPosition' not handling forward searches (yet)
      if (cursor.getRow() < startPos.getRow())
         if (!cursor.moveToNextToken())
            return 0;
      
      // NOTE: The token associated with the current cursor position
      // is added last, to ensure that after the 'multi-select' session has
      // ended the cursor remains where it started.
      Position cursorPos = editor_.getCursorPosition();
      
      do
      {
         // Left brackets push on the stack.
         if (cursor.isLeftBracket())
         {
            // Update state.
            if (cursor.valueEquals("("))
            {
               if (cursor.peekBwd(1).valueEquals("function"))
                  pushState(STATE_FUNCTION_DEFINITION);
               else
                  pushState(STATE_FUNCTION_CALL);
            }
            else
            {
               pushState(STATE_DEFAULT);
            }
            
            // Update protected names for braces.
            if (cursor.valueEquals("{"))
               updateProtectedNames(cursor.currentPosition(), scope, false);
         }
         
         // Right brackets pop the stack.
         if (cursor.isRightBracket())
         {
            popState();
            if (cursor.valueEquals("}"))
               updateProtectedNames(cursor.currentPosition(), scope, true);
         }
         
         // Bail if we've reached the end of the scope.
         if (cursor.currentPosition().isAfterOrEqualTo(endPos))
            break;
         
         if (cursor.currentType().equals(targetType) &&
             cursor.currentValue().equals(targetValue))
         {
            // Skip 'protected' names. These are names that have been overwritten
            // either as assignments, or exist as names to newly defined functions.
            if (newFnArgNames_.contains(cursor.currentValue()))
               continue;
            
            // Don't rename the argument names for named function calls.
            // For example, if we're refactoring a variable named 'bar', we
            // only want to refactor the underlined pieces:
            //
            //    bar <- bar + 1; foo(bar = bar)
            //    ~~~    ~~~                ~~~
            if (peekState() == STATE_FUNCTION_CALL && cursor.nextValue().equals("="))
               continue;
            
            // Don't rename argument names in function definitions.
            // For example, if we're refactoring a variable named 'bar', we
            // only want to refactor the underlined pieces:
            //
            //    bar <- bar + 1; foo <- function(bar) { ... }
            //    ~~~    ~~~        
            //
            // This is tricky because we only want to perform this skip for nested
            // functions; parent function definitions should be fine.
            // E.g.
            //
            //    foo <- function(bar) { bar <- bar + 1 }
            //                    ~~~    ~~~    ~~~
            if (peekState() == STATE_FUNCTION_DEFINITION &&
                editor_.getScopeAtPosition(cursor.currentPosition()) != scope)
            {
               String prevValue = cursor.peekBwd(1).getValue();
               if (prevValue.equals("(") ||
                   prevValue.equals(",") ||
                   prevValue.equals("="))
               {
                  continue;
               }
            }
            
            Range tokenRange = getTokenRange(cursor);
            if (!tokenRange.contains(cursorPos))
               ranges_.add(tokenRange);
         }
      } while (cursor.moveToNextToken());
      
      // Add the initial range last (ensuring that the cursor is placed here
      // after exiting 'multi-select' mode)
      if (cursor.moveToPosition(cursorPos, true))
         ranges_.add(getTokenRange(cursor));

      return applyRanges();
      
   }
   
   private int applyRanges()
   {
      // Clear any old selection...
      if (ranges_.size() > 0)
         editor_.clearSelection();

      // ... and select all of the new ranges of tokens.
      for (Range range : ranges_)
         editor_.getNativeSelection().addRange(range, false);

      return ranges_.size();
   }
   
   private void init()
   {
      ranges_.clear();
      state_.clear();
   }
   
   private int peekState()
   {
      return state_.empty() ?
            STATE_TOP_LEVEL :
            state_.peek();
   }
   
   private void pushState(int state)
   {
      state_.push(state);
   }
   
   private int popState()
   {
      return state_.empty() ?
            STATE_TOP_LEVEL :
            state_.pop();
   }
   
   private Range getTokenRange(TokenCursor cursor)
   {
      Position startPos = cursor.currentPosition();
      Position endPos = Position.create(
            startPos.getRow(), startPos.getColumn() + cursor.currentValue().length());
      return Range.fromPoints(startPos, endPos);
   }
   
   private void updateProtectedNames(Position position,
                                     Scope parentScope,
                                     boolean excludeCurrentState)
   {
      newFnArgNames_.clear();
      
      Scope scope = editor_.getScopeAtPosition(position);
      if (excludeCurrentState)
         scope = scope.getParentScope();
      
      while (scope != parentScope && !(scope.isTopLevel()))
      {
         if (scope.isFunction())
         {
            JsArrayString argNames = ((ScopeFunction) scope).getFunctionArgs();
            for (int i = 0; i < argNames.length(); i++)
               newFnArgNames_.add(argNames.get(i));
         }
         scope = scope.getParentScope();
      }
   }
   
   private final AceEditor editor_;
   private final List<Range> ranges_;
   private final Stack<Integer> state_;
   private final Set<String> newFnArgNames_;
   
   private static final int STATE_TOP_LEVEL = 1;
   private static final int STATE_DEFAULT = 2;
   private static final int STATE_FUNCTION_CALL = 3;
   private static final int STATE_FUNCTION_DEFINITION = 4;

}
