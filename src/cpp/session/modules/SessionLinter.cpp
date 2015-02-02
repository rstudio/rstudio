/*
 * SessionLinter.cpp
 *
 * Copyright (C) 2009-2015 by RStudio, Inc.
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

#include "SessionLinter.hpp"

#include <set>

#include <core/Exec.hpp>
#include <core/Error.hpp>

#include <session/SessionModuleContext.hpp>

#include <boost/shared_ptr.hpp>
#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/range/adaptor/map.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/r_util/RTokenizer.hpp>
#include <core/FileUtils.hpp>
#include <core/collection/Tree.hpp>
#include <core/collection/Stack.hpp>

#define RSTUDIO_ENABLE_DEBUG_MACROS
#define RSTUDIO_DEBUG_LABEL "linter"
#include <core/Macros.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

using namespace core;
using namespace core::r_util;
using namespace core::r_util::token_utils;
using namespace core::collection;

namespace {

std::vector<std::string> makeNSEFunctions()
{
   std::vector<std::string> s;
   
   s.push_back("library");
   s.push_back("require");
   s.push_back("quote");
   s.push_back("substitute");
   s.push_back("enquote");
   s.push_back("expression");
   s.push_back("evalq");
   
   return s;
}

static std::vector<std::string> s_nseFunctions =
      makeNSEFunctions();

std::map<std::string, std::string> makeComplementVector()
{
   std::map<std::string, std::string> m;
   m["["] = "]";
   m["("] = ")";
   m["{"] = "}";
   m["[["] = "]]";
   m["]"] = "[";
   m[")"] = "(";
   m["}"] = "{";
   m["]]"] = "[[";
   return m;
}

static std::map<std::string, std::string> s_complements =
      makeComplementVector();

} // anonymous namespace

std::string complement(const std::string& string)
{
   return s_complements[string];
}

namespace {

void addUnreferencedSymbol(const ParseItem& item,
                           LintItems& lint)
{
   const ParseNode* pNode = item.pNode;
   if (!pNode)
      return;
   
   // Attempt to find a similarly named candidate in scope
   std::string candidate = pNode->suggestSimilarSymbolFor(item);
   lint.noSymbolNamed(item, candidate);
   
   // Check to see if there is a symbol in that node of
   // the parse tree (but defined later)
   ParseNode::SymbolPositions& symbols =
         const_cast<ParseNode::SymbolPositions&>(pNode->getDefinedSymbols());
   
   if (symbols.count(item.symbol))
   {
      ParseNode::Positions positions = symbols[item.symbol];
      BOOST_FOREACH(const Position& position, positions)
      {
         lint.symbolDefinedAfterUsage(item, position);
      }
   }
}

} // end anonymous namespace

// The ParseStatus contains:
//
// 1. The parse tree,
// 2. The current active node,
// 3. The brace stack,
// 4. The lint items collected while parsing.
namespace {

std::wstring typeToWideString(char type)
{
        if (type == RToken::LPAREN) return L"LPAREN";
   else if (type == RToken::RPAREN) return L"RPAREN";
   else if (type == RToken::LBRACKET) return L"LBRACKET";
   else if (type == RToken::RBRACKET) return L"RBRACKET";
   else if (type == RToken::LBRACE) return L"LBRACE";
   else if (type == RToken::RBRACE) return L"RBRACE";
   else if (type == RToken::COMMA) return L"COMMA";
   else if (type == RToken::SEMI) return L"SEMI";
   else if (type == RToken::WHITESPACE) return L"WHITESPACE";
   else if (type == RToken::STRING) return L"STRING";
   else if (type == RToken::NUMBER) return L"NUMBER";
   else if (type == RToken::ID) return L"ID";
   else if (type == RToken::OPER) return L"OPER";
   else if (type == RToken::UOPER) return L"UOPER";
   else if (type == RToken::ERR) return L"ERR";
   else if (type == RToken::LDBRACKET) return L"LDBRACKET";
   else if (type == RToken::RDBRACKET) return L"RDBRACKET";
   else if (type == RToken::COMMENT) return L"COMMENT";
   else return L"<unknown>";
}

std::string typeToString(char type)
{
   return string_utils::wideToUtf8(typeToWideString(type));
}

#undef RS_TYPE_TO_STRING_CASE

#define RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, __ACTION__)                \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.__ACTION__())                                            \
      {                                                                        \
         DEBUG("(" << __LINE__ << ":" << #__ACTION__ << "): Failed");          \
         __STATUS__.lint().unexpectedEndOfDocument(__CURSOR__);                \
         return;                                                               \
      }                                                                        \
   } while (0)

#define MOVE_TO_NEXT_TOKEN(__CURSOR__, __STATUS__)                             \
   RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, moveToNextToken);

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __STATUS__)                 \
   RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, moveToNextSignificantToken);

#define FWD_OVER_WHITESPACE(__CURSOR__, __STATUS__)                            \
   RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, fwdOverWhitespace);

#define FWD_OVER_WHITESPACE_AND_COMMENTS(__CURSOR__, __STATUS__)               \
   RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, fwdOverWhitespaceAndComments);


#define FWD_OVER_BLANK(__CURSOR__, __STATUS__)                                 \
   RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, fwdOverBlank);

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(__CURSOR__,          \
                                                          __STATUS__)          \
   do                                                                          \
   {                                                                           \
      MOVE_TO_NEXT_TOKEN(__CURSOR__, __STATUS__);                              \
      if (isWhitespace(__CURSOR__))                                            \
         __STATUS__.lint().unnecessaryWhitespace(__CURSOR__);                  \
      FWD_OVER_WHITESPACE_AND_COMMENTS(__CURSOR__, __STATUS__);                \
   } while (0)

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(__CURSOR__,       \
                                                             __STATUS__)       \
   do                                                                          \
   {                                                                           \
      if (isWhitespace(__CURSOR__))                                            \
      {                                                                        \
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __STATUS__);               \
      }                                                                        \
      else                                                                     \
      {                                                                        \
         MOVE_TO_NEXT_TOKEN(__CURSOR__, __STATUS__);                           \
         if (!isWhitespace(__CURSOR__))                                        \
            __STATUS__.lint().expectedWhitespace(__CURSOR__);                  \
         FWD_OVER_WHITESPACE_AND_COMMENTS(__CURSOR__, __STATUS__);             \
      }                                                                        \
   } while (0)

#define ENSURE_CONTENT(__CURSOR__, __STATUS__, __CONTENT__)                    \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.contentEquals(__CONTENT__))                              \
      {                                                                        \
         DEBUG("(" << __LINE__ << "): Expected "                               \
                   << string_utils::wideToUtf8(__CONTENT__));                  \
         __STATUS__.lint().unexpectedToken(__CURSOR__, __CONTENT__);           \
         return;                                                               \
      }                                                                        \
   } while (0)

#define ENSURE_TYPE(__CURSOR__, __STATUS__, __TYPE__)                          \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.isType(__TYPE__))                                        \
      {                                                                        \
         DEBUG("(" << __LINE__ << "): Expected " << typeToString(__TYPE__));   \
         __STATUS__.lint().unexpectedToken(                                    \
             __CURSOR__, L"'" + typeToWideString(__TYPE__) + L"'");            \
         return;                                                               \
      }                                                                        \
   } while (0)

#define UNEXPECTED_TOKEN(__CURSOR__, __STATUS__)                               \
   do                                                                          \
   {                                                                           \
      DEBUG("(" << __LINE__ << "): Unexpected token (" << __CURSOR__ << ")");  \
      __STATUS__.lint().unexpectedToken(__CURSOR__);                           \
   } while (0)

// We define a number of 'handlers' that take a token cursor,
// and a parse status, and handle different stages of the parse
// process.
//
// All 'handler's take their items by non-const reference just
// for brevity; because this pattern is common throughout the
// handlers it shouldn't be too surprising that the handlers
// may modify the objects passed in.
//
void handleExpression(TokenCursor& cursor, ParseStatus& status);
void doHandleExpression(TokenCursor& cursor, ParseStatus& status);

// Control flow is handled specially.
void handleControlFlow(TokenCursor& cursor, ParseStatus& status, bool* pWasHandled);

void handleFor(TokenCursor& cursor, ParseStatus& status);
void handleWhile(TokenCursor& cursor, ParseStatus& status);
void handleIf(TokenCursor& cursor, ParseStatus& status);
void handleArgument(TokenCursor& cursor, ParseStatus& status);
void handleArgumentList(TokenCursor& cursor, ParseStatus& status);
void handleFunction(TokenCursor& cursor, ParseStatus& status);
void handleRepeat(TokenCursor& cursor, ParseStatus& status);
void handleIdentifier(TokenCursor& cursor, ParseStatus& status);
void handleLeftBracket(TokenCursor &cursor, ParseStatus &status);
void handleRightBracket(TokenCursor &cursor, ParseStatus &status);
void handleBinaryOperator(TokenCursor &cursor, ParseStatus &status);
void handleStatement(TokenCursor &cursor, ParseStatus &status);

void handleControlFlow(TokenCursor& cursor,
                       ParseStatus& status,
                       bool* pWasHandled)
{
   if (cursor.contentEquals(L"if"))
      return handleIf(cursor, status);
   else if (cursor.contentEquals(L"for"))
      return handleFor(cursor, status);
   else if (cursor.contentEquals(L"while"))
      return handleWhile(cursor, status);
   else if (cursor.contentEquals(L"repeat"))
      return handleRepeat(cursor, status);
   else if (cursor.contentEquals(L"function"))
      return handleFunction(cursor, status);
   
   *pWasHandled = false;
}

// A 'for-in' loop is of the form:
//
//    for (<id> in <statement-or-expression>) <statement-or-expression>
//
void handleFor(TokenCursor& cursor,
               ParseStatus& status)
{
   DEBUG("-- handleFor -- (" << cursor << ")");
   ENSURE_CONTENT(cursor, status, L"for");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   status.push(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::ID);
   handleIdentifier(cursor, status);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_CONTENT(cursor, status, L"in");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   DEBUG("-- handleFor: Expression following 'in' -- (" << cursor << ")");
   handleExpression(cursor, status);
   DEBUG("-- handleFor: Ended expression -- (" << cursor << ")");
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   status.pop(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   DEBUG("-- end handleFor -- (" << cursor << ")");
}

// A 'while' loop is of the form:
//
//    while (<statment-or-expression>) <statement-or-expression>
//
void handleWhile(TokenCursor& cursor,
                 ParseStatus& status)
{
   DEBUG("-- handleWhile -- (" << cursor << ")");
   ENSURE_CONTENT(cursor, status, L"while");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   status.push(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   status.pop(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   DEBUG("-- end handleWhile -- (" << cursor << ")");
}

// Handle an 'if-else' statement, e.g.
//
//    if (<statement-or-expression) <statement-or-expression>
//    
// which is followed by optional 'else-if', or final 'else'.
void handleIf(TokenCursor& cursor,
              ParseStatus& status)
{
   DEBUG("-- handleIf -- (" << cursor << ")");
   ENSURE_CONTENT(cursor, status, L"if");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   handleExpression(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   
   // After handling the 'if' expression, the token cursor should either
   // be on a (token containing a) newline, or a semicolon, or a closing
   // brace.
   //
   // Check to see if the next significant token is an 'else', and continue
   // parsing here if so.
   if (cursor.nextSignificantToken().contentEquals(L"else"))
   {
      // Move onto the 'else' token
      DEBUG("** Moving on to 'else' token");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      
      // Move to the next token
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      
      // Now, handle a new 'if' block, or a final expression.
      if (cursor.contentEquals(L"if"))
         return handleIf(cursor, status);
      else
         return handleExpression(cursor, status);
   }
}

// Repeat is simply
//
//    repeat <statement-or-expression>
//
// Interestingly, for example, 'repeat break' is a valid expression.
void handleRepeat(TokenCursor& cursor,
                  ParseStatus& status)
{
   ENSURE_CONTENT(cursor, status, L"repeat");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
}

// Handle a generic R expression. An R expression is of the form:
//
//     1. ('(' or '{')* (<statement [separator])* (')' or '}')*
//
// Notes:
//
//   * The statement may be of length zero,
//   * Statements may be joined with binary operators,
//   * The separator for '{}' blocks is either a newline or a semicolon,
//   * The separator for '()' blocks is a comma.
//   * A closing bracket implicitly closes the inner expression.
//
// This function handles each of the above possible 'states'.
void handleExpression(TokenCursor& cursor,
                      ParseStatus& status)
{
   DEBUG("-- Handle expression -- (" << cursor << ")");
   status.setDepth(status.stack().size());
   status.setLastCursorPosition(cursor.currentPosition());
   return doHandleExpression(cursor, status);
}

void doHandleExpression(TokenCursor& cursor,
                        ParseStatus& status)
{
   DEBUG("-- doHandleExpression -- (" << cursor << ")");
   // Check for control flow work. These are single expressions,
   // but are parsed separately.
   bool controlFlowHandled = true;
   handleControlFlow(cursor, status, &controlFlowHandled);
   if (controlFlowHandled)
      return;
   
   // Move off of separators.
   while (cursor.isType(RToken::COMMA) ||
          cursor.isType(RToken::SEMI) ||
          cursor.isType(RToken::WHITESPACE))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   }
   
   if (isLeftBrace(cursor))
      return handleLeftBracket(cursor, status);
   else if (isRightBrace(cursor))
      return handleRightBracket(cursor, status);
   else if (isBinaryOp(cursor))
      return handleBinaryOperator(cursor, status);
   else
      return handleStatement(cursor, status);
}

void handleLeftBracket(TokenCursor& cursor,
                       ParseStatus& status)
{
   DEBUG("-- handleLeftBracket -- (" << cursor << ")");
   
   status.push(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   return doHandleExpression(cursor, status);
}

void handleRightBracket(TokenCursor& cursor,
                        ParseStatus& status)
{
   DEBUG("-- handleRightBracket -- (" << cursor << ")");
   DEBUG("---- Depth: (" << status.getDepth() << ")");
   DEBUG("---- Stack size: (" << status.stack().size() << ")");
   
   // The question here is: do we consume the right bracket
   // as part of this expression, or return and let it be handled
   // as part of a different expression? We only want to handle
   // it here if the stack size is greater than the start depth.
   //
   // E.g. to trace this out for an example:
   //
   //                  (for (i in 1:10) 10)
   //                  ^    ^         ^   ^
   //    Brace Stack:  1    2         1   0
   //
   // When parsing the expression 1:10, we do not want to
   // consume the ')' as part of that expression.
   std::size_t depth = status.getDepth();
   std::size_t stackSize = status.stack().size();
   
   if (depth == stackSize)
   {
      return;
   }
   
   // If the stack size is greater than the depth, that implies
   // this expression contains its own parens, e.g.
   //
   //                  ( for ( i in ( 1 : 10 ) ) )
   //                  ^     ^      ^        ^ ^ ^ 
   //    Brace Stack:  1     2      3        2 1 0    
   //    Depth:                     2        2 
   else if (stackSize > depth)
   {
      status.pop(cursor);
      if (isBinaryOp(cursor.nextSignificantToken()))
      {
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         return handleBinaryOperator(cursor, status);
      }
      else
      {
         if (cursor.isAtEndOfDocument())
            return;
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         return doHandleExpression(cursor, status);
      }
   }
   
   // If the stack size is less than the depth, this is
   // an error. Warn, but ensure we move.
   else
   {
      status.lint().unexpectedClosingBracket(cursor, status.stack());
      
      // Pop off the stack anyway to stay consistent
      if (stackSize > 0)
         status.pop(cursor);
      
      // And ensure we move forward (there is no parent to handle)
      if (cursor.isAtEndOfDocument())
         return;
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return;
   }
}

void handleBinaryOperator(TokenCursor& cursor,
                          ParseStatus& status)
{
   DEBUG("-- handleBinaryOperator -- (" << cursor << ")");
   
   if (!isBinaryOp(cursor))
      UNEXPECTED_TOKEN(cursor, status);
   
   // Certain binary operators should have whitespace surrounding.
   if (!(cursor.contentContains(L':') ||
         cursor.contentEquals(L"@") ||
         cursor.contentEquals(L"$")))
   {
      if (!isWhitespace(cursor.previousToken()))
         status.lint().expectedWhitespace(cursor.previousToken());

      if (!isWhitespace(cursor.nextToken()))
         status.lint().expectedWhitespace(cursor.nextToken());
   }
   
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   return doHandleExpression(cursor, status);
}

// A block statement can be ended by:
// And R statement (discounting enclosing brackets) is of the form:
//
//    (unary-op)? ([id string]) (arg-list)?
//
// where arg-list will show up for function calls / subsetting, e.g.
//
//    foo()
//    bar[]
//
// but can also be generally made of unlimited calls, e.g.
//
//    foo()()[[1, 2]]()
//
// Following a statement should either be a binary operator (continuing
// the statement), or a closing brace (closing a parent argument list
// or expression)
void handleStatement(TokenCursor& cursor,
                     ParseStatus& status)
{
   DEBUG("-- handleStatement -- (" << cursor << ")");
   
   // Move over a unary operator
   if (isValidAsUnaryOperator(cursor))
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   
   // We should now see a symbol (or string).
   if (!isValidAsIdentifier(cursor))
   {
      UNEXPECTED_TOKEN(cursor, status);
      return;
   }
   
   // Handler an identifier (ie, insert a reference in the parse tree)
   handleIdentifier(cursor, status);
   
   // There are a couple of things that can follow an identifier
   // in a statement:
   //
   //    1. <id> <l-paren>    - function call (or subset!)
   //    2. <id> <binary-op>  - (binary) function call
   //    3. <id> <new-line>   - finishing a statement
   //    4. <id> <r-paren>    - maybe closing a parent block
   //    5. <id> <comma>      - separating statements in function arg list
   //    6. <id> <semi>       - explicitly ending a statement
   //
   // We handle each case separately.
   
   // First, move over a function, parsing its argument list if necessary.
   if (isLeftBrace(cursor.nextSignificantToken()))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
      while (isLeftBrace(cursor))
      {
         if (cursor.isType(RToken::LPAREN) ||
             cursor.isType(RToken::LBRACKET) ||
             cursor.isType(RToken::LDBRACKET))
         {
            handleArgumentList(cursor, status);
            if (isLeftBrace(cursor.nextSignificantToken()))
            {
               MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
               continue;
            }
         }
         else
            UNEXPECTED_TOKEN(cursor, status);
      }
   }
   
   // Check for a binary operator, to continue the statement.
   if (isBinaryOp(cursor.nextSignificantToken()))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return handleBinaryOperator(cursor, status);
   }
   
   if (isRightBrace(cursor.nextSignificantToken()))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return doHandleExpression(cursor, status);
   }
   
   // Check for end of the document.
   if (cursor.isAtEndOfDocument())
      return;
   
   // Check for the end of the statement.
   if (cursor.nextToken().contentContains(L'\n') ||
       cursor.nextSignificantToken().isType(RToken::SEMI) ||
       cursor.nextSignificantToken().isType(RToken::COMMA))
   {
      if (cursor.nextToken().contentContains(L'\n'))
      {
         MOVE_TO_NEXT_TOKEN(cursor, status);
      }
      else
      {
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      
      if (cursor.isAtEndOfDocument())
         return;
      
      if (status.getDepth() > status.stack().size())
         return doHandleExpression(cursor, status);
      else
         return;
   }
   
   // Shouldn't get here!
   UNEXPECTED_TOKEN(cursor, status);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
}

// An argument list is of the form:
//
//    <l-bracket> <r-bracket>
//    <l-bracket> <arg-expr> <r-bracket>
//    <l-bracket> <arg-expr> (comma arg-expr)* <r-bracket>
//
// This is true for '(', '[' and '[['.
void handleArgumentList(TokenCursor& cursor,
                        ParseStatus& status)
{
   DEBUG("-- handleArgumentList -- (" << cursor << ")");
   
   if (!isLeftBrace(cursor))
      status.lint().unexpectedToken(cursor);
   
   status.push(cursor);
   
   std::string lhs = cursor.contentAsUtf8();
   std::string rhs = complement(lhs);
   
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   
   // Check for an empty argument list.
   if (isRightBrace(cursor))
   {
      if (cursor.contentAsUtf8() != rhs)
         status.lint().unexpectedToken(cursor, string_utils::utf8ToWide(rhs));
      return;
   }
   
   // Check for a single argument.
   handleArgument(cursor, status);
   
   // We should now lie either on a comma, or on the closing bracket.
   while (cursor.isType(RToken::COMMA))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      handleArgument(cursor, status);
   }
   
   ENSURE_CONTENT(cursor, status, string_utils::utf8ToWide(rhs));
}

// An argument is of the form:
//
//    <expr>
//    <id> <equals> <expr>
//
// For function declarations, we want to cache the
// identifier names (as they are made part of the scope of
// the associated expression)
void handleArgument(TokenCursor& cursor,
                    ParseStatus& status)
{
   DEBUG("-- handleArgument -- (" << cursor << ")");
   
   // Try handling a control flow expression.
   bool wasHandled = true;
   handleControlFlow(cursor, status, &wasHandled);
   if (wasHandled)
   {
      DEBUG("-- Handled control flow -- (" << cursor << ")");
      return;
   }
   
   // Try handling regular identifiers.
   if (isId(cursor) &&
       cursor.nextSignificantToken().contentEquals(L"="))
   {
      DEBUG("Found an '='");
      
      // Move on to '='
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      
      // Move to start of expression
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   }
   
   // An 'empty' argument will just be a comma -- in such a case,
   // just move over it.
   if (cursor.isType(RToken::COMMA) ||
       isRightBrace(cursor))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return;
   }
   
   // Handle a generic expression, which should be followed by a
   // comma or a closing bracket.
   handleExpression(cursor, status);
}

// Handle an identifier -- this means caching the location
// in the parse tree.
void handleIdentifier(TokenCursor& cursor,
                      ParseStatus& status)
{
   // Check to see if we are defining a symbol at this location.
   // Note that both string and id are valid for assignments.
   if (cursor.isType(RToken::ID) ||
       cursor.isType(RToken::STRING))
   {
      if (isLocalLeftAssign(cursor.nextSignificantToken()) ||
          isLocalRightAssign(cursor.previousSignificantToken()))
         status.node()->addDefinedSymbol(cursor);
   }
   
   // If this is truly an identifier, add a reference.
   if (cursor.isType(RToken::ID))
      status.node()->addReferencedSymbol(cursor);
}

// Handle a function declaration.
//
// A function declaration is of the form:
//
//    function <arg-list> <expr>
//
void handleFunction(TokenCursor& cursor,
                    ParseStatus& status)
{
   DEBUG("*** Begin handle function");
   DEBUG("Current token: " << cursor);
   
   // Create a new node for this function. Try to choose
   // an appropriate name for this function.
   TokenCursor clone = cursor.clone();
   std::string fnName = "<anonymous>";
   
   // TODO: What about e.g. 'x <- y <- function () ... ' ?
   if (clone.moveToPreviousSignificantToken() &&
       isLocalLeftAssign(clone) &&
       clone.moveToPreviousSignificantToken())
      fnName = clone.contentAsUtf8();
   
   // Create the child node, and set
   // that as the current node.
   boost::shared_ptr<ParseNode> pChild = ParseNode::createNode(fnName);
   status.addChildAndSetAsCurrentNode(pChild,
                                      cursor.currentPosition());
   
   ENSURE_CONTENT(cursor, status, L"function");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   handleArgumentList(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   
   // Close the function scope.
   status.setParentAsCurrent();
}

} // end anonymous namespace

// Parse a string (and generate lint).
ParseResults parse(const std::string& string)
{
   RTokens tokens(string_utils::utf8ToWide(string));
   AnnotatedRTokens rTokens(tokens);
   ParseStatus status;
   TokenCursor cursor(rTokens);
   
   cursor.fwdOverWhitespaceAndComments();
   Position lastPosition = cursor.currentPosition();
   
   while (!cursor.isAtEndOfDocument())
   {
      handleExpression(cursor, status);
      if (cursor.currentPosition() == lastPosition)
      {
         status.lint().unexpectedToken(cursor);
         DEBUG("** Failed to move to next expression -- parse error?");
         cursor.moveToNextSignificantToken();
      }
      lastPosition = cursor.currentPosition();
   }
   
   if (status.node()->getParent() != NULL)
      status.lint().unexpectedEndOfDocument(cursor.currentToken());
   
   std::vector<ParseItem> unresolvedItems;
   status.root()->findAllUnresolvedSymbols(&unresolvedItems);
   
   return std::make_pair(status.root(), status.lint());
}

ParseResults parseAndLintRFile(const FilePath& filePath,
                               const std::set<std::string>& objects)
{
   std::string contents = file_utils::readFile(filePath);
   if (contents.empty() || contents.find_first_not_of(" \n\t\v") == std::string::npos)
      return ParseResults();
   
   RTokens tokens(string_utils::utf8ToWide(contents));
   AnnotatedRTokens rTokens(tokens);
   
   // The parse status is a collection of lint, plus a stack
   // of braces (to check as we parse)
   ParseStatus status;
   
   // The token cursor is used to walk through the tokens
   // and update lint.
   TokenCursor cursor(rTokens);
   
   // Move to the first significant token in the document.
   cursor.fwdOverWhitespaceAndComments();
   Position lastPosition = cursor.currentPosition();
   
   while (!cursor.isAtEndOfDocument())
   {
      handleExpression(cursor, status);
      if (cursor.currentPosition() == lastPosition)
      {
         status.lint().unexpectedToken(cursor);
         DEBUG("** Failed to move to next expression -- parse error?");
         cursor.moveToNextSignificantToken();
      }
      lastPosition = cursor.currentPosition();
   }
   
   // TODO: Should we report missing matches? Probably need a way
   // of specifying a 'full' lint vs. 'partial' lint; ie,
   // to differentiate between 'does it look okay so far' and
   // 'is this entire document okay'.
   if (status.node()->getParent() != NULL)
   {
      DEBUG("Unexpected end of document; unclosed function brace");
      status.lint().unexpectedEndOfDocument(cursor.currentToken());
   }
   
   // Now that we have parsed through the whole document and
   // built a lint tree, we now walk through each identifier and
   // ensure that they exist either on the search path, or a parent
   // closure.
   std::vector<ParseItem> unresolvedItems;
   status.root()->findAllUnresolvedSymbols(&unresolvedItems);
   
   // Finally, prune the set of lintItems -- we exclude any symbols
   // that were on the search path.
   BOOST_FOREACH(const ParseItem& item, unresolvedItems)
   {
      if (objects.count(item.symbol) == 0)
      {
         addUnreferencedSymbol(item, status.lint());
      }
   }
   
   return std::make_pair(status.root(), status.lint());
}

SEXP rs_parseAndLintRFile(SEXP pathSEXP,
                          SEXP searchPathObjectsSEXP)
{
   std::string path = r::sexp::asString(pathSEXP);
   FilePath filePath(module_context::resolveAliasedPath(path));
   
   std::set<std::string> objects;
   bool success = r::sexp::fillSetString(searchPathObjectsSEXP, &objects);
   if (!success)
   {
      DEBUG("Failed to fill search path vector");
      return R_NilValue;
   }
   
   DEBUG("Number of objects on search path: " << objects.size());
   
   ParseResults lintResults = parseAndLintRFile(filePath, objects);
   std::vector<LintItem> lintItems = lintResults.second.get();
   
   using namespace rstudio::r::sexp;
   Protect protect;
   ListBuilder result(&protect);
   for (std::size_t i = 0; i < lintItems.size(); ++i)
   {
      const LintItem& item = lintItems[i];
      
      ListBuilder lintList(&protect);
      
      // NOTE: Convert from C++ to R indexing
      lintList.add("start.row", item.startRow + 1);
      lintList.add("end.row", item.endRow + 1);
      lintList.add("start.column", item.startColumn + 1);
      lintList.add("end.column", item.endColumn + 1);
      lintList.add("type", asString(item.type));
      lintList.add("message", item.message);
      
      result.add(static_cast<SEXP>(lintList));
   }
   
   return result;
}

core::Error initialize()
{
   using namespace rstudio::core;
   using boost::bind;
   using namespace module_context;
   
   RS_REGISTER_CALL_METHOD(rs_parseAndLintRFile, 2);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionLinter.R"));

   return initBlock.execute();

}

} // end namespace linter
} // end namespace modules
} // end namespace session
} // end namespace rstudio
