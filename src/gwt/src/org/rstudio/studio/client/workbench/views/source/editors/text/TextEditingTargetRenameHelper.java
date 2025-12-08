package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.TokenCursor;
import org.rstudio.studio.client.workbench.views.source.editors.text.assist.RChunkHeaderParser;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Timer;

public class TextEditingTargetRenameHelper
{
   public TextEditingTargetRenameHelper(DocDisplay docDisplay, UserPrefs prefs)
   {
      editor_ = (AceEditor) docDisplay;
      ranges_ = new ArrayList<>();
      state_ = new Stack<>();
      protectedNamesList_ = new ArrayList<>();
      renameChunkState_ = new RenameChunkState();
      prefs_ = prefs;
   }
   
   public int renameInScope()
   {
      init();

      TextFileType type = editor_.getFileType();
      if (type.isR() || type.isRhtml() || type.isRmd() || type.isRnw() || type.isRpres())
         return renameInScopeR();
      
      return 0;
   }
   
   private int renameInScopeR()
   {
      Position startPosition = editor_.hasSelection() ?
            editor_.getSelectionStart() :
            editor_.getCursorPosition();
            
      TokenCursor cursor = editor_.getSession().getMode().getCodeModel().getTokenCursor();
      if (!cursor.moveToPosition(startPosition, true))
         return 0;
      
      if (cursor.isRightBracket())
         if (!cursor.moveToPreviousToken())
            return 0;
      
      editor_.setCursorPosition(cursor.currentPosition());
      
      // Ensure the scope tree is built, since we use that for determining
      // the scope for the current refactor.
      editor_.buildScopeTree();
      
      // Validate that we're looking at an R identifier
      String targetValue = cursor.currentValue();
      String targetType = cursor.currentType();
      
      boolean isRefactorable =
            cursor.hasType("identifier", "constant.language", "string") ||
            cursor.typeEquals("keyword");
      
      if (!isRefactorable)
         return 0;
      
      // If we're refactoring a string, just do it through the whole document.
      if (cursor.typeEquals("string"))
      {
         editor_.selectAll(cursor.currentValue());
         return editor_.getNativeSelection().getAllRanges().length;
      }
      
      // Check to see if we're refactoring the name of an argument in a function call,
      // e.g.
      //
      //    if (foo(apple = 1)) { ... }
      //            ^^^^^
      //
      // If this is the case, then we want to search for all calls of this
      // form and rename just that argument.
      if (cursor.peekFwd(1).valueEquals("=") && cursor.isWithinFunctionCall())
      {
         String argName = cursor.currentValue();
         TokenCursor clone = cursor.cloneCursor();
         
         while (clone.findOpeningBracket("(", false))
         {
            if (!clone.moveToPreviousToken())
               break;
            
            String functionName = clone.currentValue();
            if (functionName != "function")
               return renameFunctionArgument(functionName, argName);
         }
      }
      
      // Determine the appropriate refactoring scope.
      //
      // Algorithm:
      //
      //    1. Get the function scope.
      //    2. If we have an argument of the same name, or function
      //       of the same name, rename in that scope.
      //    3. Otherwise, walk forward from the start of that scope,
      //       looking for assignments.
      //    4. If we discover an assignment, rename in that scope
      //       from that position.
      //    5. Repeat while not at top level.
      //
      // TODO: if renaming a function argument, we should also rename
      //       named usages of that function argument where possible.
      Scope scope = editor_.getScopeAtPosition(cursor.currentPosition());
      
      // If the cursor is on the name of a function, then pop up one scope.
      // We want to consider scopes as 'within' the braces, but the 'preamble'
      // of a scope begins with the function identifier.
      if (cursor.peekFwd(1).isLeftAssign() &&
          cursor.peekFwd(2).valueEquals("function") &&
          !scope.isTopLevel())
      {
         scope = scope.getParentScope();
      }
      
      while (!scope.isTopLevel())
      {
         // If we're within a function definition, the variable should be scoped to that function.
         // Rename only within the scope of that function.
         if (scope.isFunction())
         {
            ScopeFunction scopeFn = (ScopeFunction) scope;
            
            String fnName = scopeFn.getFunctionName();
            if (fnName == targetValue)
               return renameVariablesInScope(scope, targetValue, targetType);
            
            JsArrayString fnArgs = scopeFn.getFunctionArgs();
            for (int i = 0; i < fnArgs.length(); i++)
               if (fnArgs.get(i) == targetValue)
                  return renameVariablesInScope(scope, targetValue, targetType);
         }
         
         // If we're renaming variables within a chunk,
         // apply the refactor across all R chunks.
         if (scope.isChunk())
         {
            int count = renameChunkState_.nudge();
            if (count == 0)
               renameChunkState_.setCursorPosition(editor_.getCursorPosition());
            
            if (prefs_.rmdRenameInScopeBehavior().getValue().equals(UserPrefsAccessor.RMD_RENAME_IN_SCOPE_BEHAVIOR_ALL))
               count += 1;

            if (count % 2 == 0)
            {
               List<Range> ranges = renameChunkState_.getCurrentChunkRanges();
               if (!ranges.isEmpty())
               {
                  ranges_.clear();
                  ranges_.addAll(ranges);
                  return applyRanges();
               }
               else
               {
                  // rename in current chunk
                  renameVariablesInScope(
                     scope,
                     scope.getPreamble(),
                     targetValue,
                     targetType,
                     false);

                  renameChunkState_.setCurrentChunkRanges(ranges_);
                  return applyRanges();
               }
            }
            else
            {
               List<Range> ranges = renameChunkState_.getAllChunkRanges();
               if (!ranges.isEmpty())
               {
                  ranges_.clear();
                  ranges_.addAll(ranges);
                  return applyRanges();
               }
               else
               {
                  // rename in all chunks
                  JsArray<Scope> scopeTree = editor_.getScopeTree();
                  for (int i = 0, n = scopeTree.length(); i < n; i++)
                  {
                     if (scopeTree.get(i).isChunk())
                     {
                        String headerLine = editor_.getLine(scopeTree.get(i).getPreamble().getRow());
                        Map<String, String> chunkOptions = RChunkHeaderParser.parse(headerLine);
                        String engine = chunkOptions.getOrDefault("engine", "r");
                        if (engine.toLowerCase() == "\"r\"")
                        {
                           renameVariablesInScope(
                                 scopeTree.get(i),
                                 scopeTree.get(i).getBodyStart(),
                                 targetValue,
                                 targetType,
                                 false);
                        }
                     }
                  }

                  renameChunkState_.setAllChunkRanges(ranges_);
                  return applyRanges();
               }
            }
         }
         
         if (!cursor.moveToPosition(scope.getBodyStart(), true))
            continue;
         
         while (cursor.moveToNextToken())
         {
            if (cursor.fwdToMatchingToken())
               continue;
            
            if (cursor.currentPosition().isAfterOrEqualTo(scope.getEnd()) ||
                cursor.currentPosition().isAfter(startPosition))
            {
               break;
            }
            
            if (cursor.peekFwd(1).isLeftAssign() &&
                !cursor.peekBwd(1).isExtractionOperator())
            {
               return renameVariablesInScope(
                     scope,
                     cursor.currentPosition(),
                     targetValue,
                     targetType);
            }
         }
         
         scope = scope.getParentScope();
      }
      
      return renameVariablesInScope(
            scope,
            targetValue,
            targetType);
   }
   
   private int renameFunctionArgument(String functionName, String argName)
   {
      TokenCursor cursor = editor_.getSession().getMode().getCodeModel().getTokenCursor();
      Stack<String> functionNames = new Stack<>();
      boolean renaming = false;
      
      do
      {
         if (cursor.isLeftBracket())
         {
            if (cursor.valueEquals("(") &&
                cursor.peekBwd(1).isValidForFunctionCall())
            {
               String currentFunctionName = cursor.peekBwd(1).getValue();
               renaming = currentFunctionName == functionName;
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
               renaming = !functionNames.empty() && functionNames.peek() == functionName;
            }
         }
         
         if (renaming &&
             peekState() == STATE_FUNCTION_CALL &&
             cursor.valueEquals(argName) &&
             cursor.peekFwd(1).valueEquals("="))
         {
            addRange(getTokenRange(cursor));
         }
         
      } while (cursor.moveToNextToken());
      
      return applyRanges();
      
   }
 
   private int renameVariablesInScope(Scope scope,
                                      String targetValue,
                                      String targetType)
   {
      return renameVariablesInScope(scope, scope.getPreamble(), targetValue, targetType, true);
   }
 
   private int renameVariablesInScope(Scope scope,
                                      Position startPos,
                                      String targetValue,
                                      String targetType)
   {
      return renameVariablesInScope(scope, startPos, targetValue, targetType, true);
   }
   
   private int renameVariablesInScope(Scope scope,
                                      Position startPos,
                                      String targetValue,
                                      String targetType,
                                      boolean apply)
   {
      Position endPos = scope.getEnd();
      if (endPos == null)
         endPos = Position.create(editor_.getSession().getLength(), 0);
      
      TokenCursor cursor = editor_.getSession().getMode().getCodeModel().getTokenCursor();
      cursor.moveToPosition(startPos, true);
      
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
               pushProtectedNames(cursor.currentPosition(), scope);
            
            continue;
         }
         
         // Right brackets pop the stack.
         if (cursor.isRightBracket())
         {
            popState();
            if (cursor.valueEquals("}"))
               popProtectedNames(cursor.currentPosition());
            
            continue;
         }
         
         // Protect a name if it's the target of an assignment in a child scope.
         if (cursor.hasType("identifier") &&
             cursor.peekFwd(1).isLeftAssign() &&
             !cursor.peekBwd(1).isExtractionOperator())
         {
            Scope candidate = editor_.getScopeAtPosition(cursor.currentPosition());
            
            // Skip default arguments for nested functions
            if (peekState() == STATE_FUNCTION_DEFINITION && scope != candidate)
               continue;
            
            if (cursor.peekFwd(2).valueEquals("function") && !candidate.isTopLevel())
               candidate = candidate.getParentScope();
            
            if (candidate != scope)
            {
               addProtectedName(cursor.currentValue());
               continue;
            }
         }
         
         // Bail if we've reached the end of the scope.
         if (cursor.currentPosition().isAfterOrEqualTo(endPos))
            break;
         
         if (cursor.currentValue() == targetValue)
         {
            // Skip 'protected' names. These are names that have been overwritten
            // either as assignments, or exist as names to newly defined functions.
            if (isProtectedName(cursor.currentValue()))
               continue;
            
            // Skip variables following an 'extraction' operator.
            if (cursor.peekBwd(1).isExtractionOperator())
               continue;
            
            // Skip default arguments for nested functions
            Scope candidate = editor_.getScopeAtPosition(cursor.currentPosition());
            if (peekState() == STATE_FUNCTION_DEFINITION && scope != candidate)
               continue;
            
            // Don't rename the argument names for named function calls.
            // For example, if we're refactoring a variable named 'bar', we
            // only want to refactor the underlined pieces:
            //
            //    bar <- bar + 1; foo(bar = bar)
            //    ~~~    ~~~                ~~~
            if (peekState() == STATE_FUNCTION_CALL && cursor.nextValue() == "=")
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
               if (prevValue == "(" ||
                   prevValue == "," ||
                   prevValue == "=")
               {
                  continue;
               }
            }
            
            Range tokenRange = getTokenRange(cursor);
            if (!tokenRange.contains(cursorPos))
               addRange(tokenRange);
         }
      } while (cursor.moveToNextToken());
      
      // Add the initial range last (ensuring that the cursor is placed here
      // after exiting 'multi-select' mode)
      if (cursor.moveToPosition(cursorPos, true))
         addRange(getTokenRange(cursor));

      return apply ? applyRanges() : ranges_.size();
      
   }
   
   private int applyRanges()
   {
      // Exit early if we have no ranges.
      if (ranges_.isEmpty())
         return 0;

      // Clear any existing selection.
      editor_.clearSelection();

      // Figure out the relevant cursor position, for determining
      // the primary selection. The most recently added range becomes
      // the 'primary' selection range, so we'll need to add that last.
      //
      // Doing this is important so that actions which cancel the
      // multi-select command, e.g. cursor movement, use the appropriate
      // cursor position afterwards.
      Position cursorPos = renameChunkState_.getCursorPosition();
      if (cursorPos == null)
         cursorPos = editor_.getCursorPosition();

      // First, check if one of the selected ranges contains the last
      // saved cursor position. If so, we'll want to preserve that.
      int rangeIndex = 0;
      for (int i = 0, n = ranges_.size(); i < n; i++)
      {
         Range range = ranges_.get(i);
         if (range.contains(cursorPos))
         {
            rangeIndex = i;
            break;
         }
      }
      
      // Select all 'secondary' ranges.
      for (int i = 0, n = ranges_.size(); i < n; i++)
      {
         if (i != rangeIndex)
         {
            Range range = ranges_.get(i);
            editor_.getNativeSelection().addRange(range, false);
         }
      }

      // Now select the 'primary' range.
      Range range = ranges_.get(rangeIndex);
      editor_.getNativeSelection().addRange(range, false);

      // Return the number of selected ranges.
      return ranges_.size();
   }
   
   private void init()
   {
      ranges_.clear();
      state_.clear();
      protectedNamesList_.clear();
      protectedNamesList_.add(new HashSet<>());
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
   
   private boolean isProtectedName(String name)
   {
      for (int i = 0; i < protectedNamesList_.size(); i++)
         if (protectedNamesList_.get(i).contains(name))
            return true;
      
      return false;
   }
   
   private void addProtectedName(String name)
   {
      protectedNamesList_.get(protectedNamesList_.size() - 1).add(name);
   }
   
   private void pushProtectedNames(Position position, Scope parentScope)
   {
      protectedNamesList_.add(new HashSet<>());
      Scope scope = editor_.getScopeAtPosition(position);
      if (scope.isFunction() && scope != parentScope)
      {
         JsArrayString argNames = ((ScopeFunction) scope).getFunctionArgs();
         for (int i = 0; i < argNames.length(); i++)
            addProtectedName(argNames.get(i));
      }
   }
   
   private void popProtectedNames(Position position)
   {
      if (protectedNamesList_.size() <= 1)
         return;
      
      protectedNamesList_.remove(protectedNamesList_.size() - 1);
   }
   
   private void addRange(Range range)
   {
      for (int i = 0, n = ranges_.size(); i < n; i++)
      {
         if (range.isEqualTo(ranges_.get(i)))
         {
            return;
         }
      }
      
      ranges_.add(range);
   }
   
   private static class RenameChunkState
   {
      public RenameChunkState()
      {
         currentChunkRanges_ = new ArrayList<>();
         allChunkRanges_ = new ArrayList<>();
         renameCount_ = 0;
         resetTimer_ = new Timer() 
         {
            @Override
            public void run()
            {
               reset();
            }
         };
      }

      public int nudge()
      {
         resetTimer_.schedule(CHUNK_RENAME_TIMEOUT_MS);
         return renameCount_++;
      }

      public List<Range> getCurrentChunkRanges()
      {
         return currentChunkRanges_;
      }

      public void setCurrentChunkRanges(List<Range> ranges)
      {
         currentChunkRanges_.clear();
         currentChunkRanges_.addAll(ranges);
      }

      public List<Range> getAllChunkRanges()
      {
         return allChunkRanges_;
      }

      public void setAllChunkRanges(List<Range> ranges)
      {
         allChunkRanges_.clear();
         allChunkRanges_.addAll(ranges);
      }

      public void setCursorPosition(Position cursorPosition)
      {
         cursorPosition_ = cursorPosition;
      }

      public Position getCursorPosition()
      {
         return cursorPosition_;
      }

      private void reset()
      {
         currentChunkRanges_.clear();
         allChunkRanges_.clear();
         cursorPosition_ = null;
         renameCount_ = 0;
      }

      private int renameCount_;
      private Position cursorPosition_;

      private final List<Range> currentChunkRanges_;
      private final List<Range> allChunkRanges_;
      private final Timer resetTimer_;
   }

   private final AceEditor editor_;
   private final List<Range> ranges_;
   private final Stack<Integer> state_;
   private final List<Set<String>> protectedNamesList_;
   private final RenameChunkState renameChunkState_;
   private final UserPrefs prefs_;

   private static final int STATE_TOP_LEVEL = 1;
   private static final int STATE_DEFAULT = 2;
   private static final int STATE_FUNCTION_CALL = 3;
   private static final int STATE_FUNCTION_DEFINITION = 4;

   private static final int CHUNK_RENAME_TIMEOUT_MS = 1000;
}
