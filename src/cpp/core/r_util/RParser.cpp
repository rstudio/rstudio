/*
 * RParser.hpp
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

#include <core/r_util/RParser.hpp>
#include <core/r_util/RTokenCursor.hpp>

namespace rstudio {
namespace core {
namespace r_util {

using namespace token_utils;

namespace {

std::map<std::string, std::string> makeComplementMap()
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
      makeComplementMap();

} // end anonymous namespace

std::string complement(const std::string& bracket)
{
   return s_complements[bracket];
}

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

#define RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, __ACTION__)               \
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
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, moveToNextToken);

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __STATUS__)                 \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, moveToNextSignificantToken);

#define FWD_OVER_WHITESPACE(__CURSOR__, __STATUS__)                            \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, fwdOverWhitespace);

#define FWD_OVER_WHITESPACE_AND_COMMENTS(__CURSOR__, __STATUS__)               \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, fwdOverWhitespaceAndComments);

#define FWD_OVER_BLANK(__CURSOR__, __STATUS__)                                 \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, fwdOverBlank);

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(__CURSOR__, __STATUS__)   \
   do                                                                          \
   {                                                                           \
      MOVE_TO_NEXT_TOKEN(__CURSOR__, __STATUS__);                              \
      if (isWhitespace(__CURSOR__) && !__CURSOR__.contentContains(L'\n'))      \
         __STATUS__.lint().unnecessaryWhitespace(__CURSOR__);                  \
      FWD_OVER_WHITESPACE_AND_COMMENTS(__CURSOR__, __STATUS__);                \
   } while (0)

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
void doHandleExpression(TokenCursor& cursor, ParseStatus& status, size_t depth);

// Control flow is handled specially.
void handleControlFlow(TokenCursor& cursor, ParseStatus& status, bool* pWasHandled);

void handleFor(TokenCursor& cursor, ParseStatus& status);
void handleWhile(TokenCursor& cursor, ParseStatus& status);
void handleIf(TokenCursor& cursor, ParseStatus& status);
void handleRepeat(TokenCursor& cursor, ParseStatus& status);

void handleFunction(TokenCursor& cursor, ParseStatus& status);
void handleFunctionParameterList(TokenCursor& cursor, ParseStatus& status);
void handleFunctionParameter(TokenCursor& cursor, ParseStatus& status);

void handleArgument(TokenCursor& cursor, ParseStatus& status);
void handleArgumentList(TokenCursor& cursor, ParseStatus& status);

void handleIdentifier(TokenCursor& cursor, ParseStatus& status);

void handleLeftBracket(TokenCursor& cursor, ParseStatus& status, std::size_t depth);
void handleRightBracket(TokenCursor& cursor, ParseStatus& status, std::size_t depth);
void handleBinaryOperator(TokenCursor& cursor, ParseStatus& status, std::size_t depth);
void handleStatement(TokenCursor& cursor, ParseStatus& status, std::size_t depth);

// Begin implementations of handlers
void handleControlFlow(TokenCursor& cursor,
                       ParseStatus& status,
                       bool* pWasHandled)
{
   *pWasHandled = true;
   
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
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::ID);
   status.node()->addDefinedSymbol(cursor);
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
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
   
   DEBUG("*- Begin while condition -* (" << cursor << ")");
   
   if (isRightBrace(cursor))
      status.lint().unexpectedToken(cursor);
   else
      handleExpression(cursor, status);
   
   DEBUG("*- End while condition -* (" << cursor << ")");
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
   status.push(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   
   DEBUG("*- Begin if condition -* (" << cursor << ")");
   
   if (isRightBrace(cursor))
      status.lint().unexpectedToken(cursor);
   else
      handleExpression(cursor, status);
   
   DEBUG("*- End if condition -* (" << cursor << ")");
   
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   status.pop(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   DEBUG("-- end handleIf -- (" << cursor << ")");
   
   // After handling the 'if' expression, the token cursor should either
   // be on a (token containing a) newline, or a semicolon, or a closing
   // brace.
   //
   // Check to see if the next significant token is an 'else', and continue
   // parsing here if so.
   if (cursor.contentEquals(L"else"))
   {
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
   return doHandleExpression(cursor, status, status.stack().size());
}

void doHandleExpression(TokenCursor& cursor,
                        ParseStatus& status,
                        std::size_t depth)
{
   DEBUG("-- doHandleExpression -- (" << cursor << ")");
   DEBUG("---- Depth: '" << depth << "'");
   DEBUG("---- Stack: '" << status.stack().size() << "'");
   
   // Move off of separators.
   // TODO: Check current brace stack to confirm that these
   // are the valid separators for this scope.
   while (cursor.isType(RToken::COMMA) ||
          cursor.isType(RToken::SEMI) ||
          cursor.isType(RToken::WHITESPACE))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   }
   
   // Check for control flow work. These are single expressions,
   // but are parsed separately.
   bool controlFlowHandled = true;
   handleControlFlow(cursor, status, &controlFlowHandled);
   if (controlFlowHandled)
   {
      // Does this control flow expression finish this expression?
      if (depth == status.stack().size())
         return;
      else
         return doHandleExpression(cursor, status, depth);
   }
   
   if (isLeftBrace(cursor))
      return handleLeftBracket(cursor, status, depth);
   else if (isRightBrace(cursor))
      return handleRightBracket(cursor, status, depth);
   else if (cursor.appearsToBeBinaryOperator())
      return handleBinaryOperator(cursor, status, depth);
   else if (isValidAsIdentifier(cursor) ||
            isValidAsUnaryOperator(cursor))
      return handleStatement(cursor, status, depth);
   else
   {
      status.lint().unexpectedToken(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   }
}

void handleLeftBracket(TokenCursor& cursor,
                       ParseStatus& status,
                       std::size_t depth)
{
   DEBUG("-- handleLeftBracket -- (" << cursor << ")");
   
   status.push(cursor);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   return doHandleExpression(cursor, status, depth);
}

void handleRightBracket(TokenCursor& cursor,
                        ParseStatus& status,
                        std::size_t depth)
{
   DEBUG("-- handleRightBracket -- (" << cursor << ")");
   DEBUG("---- Depth: (" << depth << ")");
   DEBUG("---- Stack size: (" << status.stack().size() << ")");
   
   // The question here is: do we consume the right bracket
   // as part of this expression, or return and let it be handled
   // as part of a different expression?
   //
   // E.g. to trace this out for an example (not all expressions
   // annotated)
   //
   //                  (for (i in 1:10) 10)
   //                  ^    ^         ^   ^
   //    Brace Stack:  1    2         1   0
   //    Expression :  A    BC       CB   A
   //
   // When parsing the expression 1:10, we do not want to
   // consume the ')' as part of that expression, as the
   // ')' __implicitly__ closes the expression.
   if (depth == status.stack().size())
   {
      return;
   }
   else if (status.stack().size() > depth)
   {
      status.pop(cursor);
      if (isBinaryOp(cursor.nextSignificantToken()))
      {
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         return handleBinaryOperator(cursor, status, depth);
      }
      else
      {
         if (cursor.isAtEndOfDocument())
            return;
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         
         // It is possible that this closing bracket __explicitly__ closes
         // the expression -- this will occur if the expression was
         // started by an opening bracket.
         if (depth == status.stack().size())
         {
            DEBUG("*-* Returning control to caller");
            return;
         }
         else
         {
            DEBUG("*-* Continuing parse of expression");
            return doHandleExpression(cursor, status, depth);
         }
      }
   }
   
   // If the stack size is less than the depth, this is
   // an error. Warn, but ensure we move.
   else
   {
      status.lint().unexpectedClosingBracket(cursor, status.stack());
      
      // Pop off the stack anyway to stay consistent
      if (status.stack().size() > 0)
         status.pop(cursor);
      
      // And ensure we move forward
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return;
   }
}

void handleBinaryOperator(TokenCursor& cursor,
                          ParseStatus& status,
                          std::size_t depth)
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
   return doHandleExpression(cursor, status, depth);
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
                     ParseStatus& status,
                     std::size_t depth)
{
   DEBUG("-- handleStatement -- (" << cursor << ")");
   
   // If we see a unary operator, we move over it and restart.
   // Note that any kind of expression can follow a unary operator,
   // e.g.
   //
   //    --!!--for(i in 1:10) 1
   //
   // is a legal R expression.
   if (isValidAsUnaryOperator(cursor))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
      return doHandleExpression(cursor, status, depth);
   }
   
   // We should now see a symbol (or string).
   if (!isValidAsIdentifier(cursor))
   {
      UNEXPECTED_TOKEN(cursor, status);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
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
      // Warn if there is non-newline containing whitespace separating
      // the cursor and the next argument.
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      
      DEBUG("*-* Before Arg List: " << cursor);
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
         {
            UNEXPECTED_TOKEN(cursor, status);
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         }
      }
      
      DEBUG("*-* After Arg List: " << cursor);
      if (!isRightBrace(cursor))
         status.lint().unexpectedToken(cursor);
      
   }
   
   // Check for a binary operator, to continue the statement.
   if (isBinaryOp(cursor.nextSignificantToken()))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return handleBinaryOperator(cursor, status, depth);
   }
   
   // Check for a right brace, to (implicitly or explicitly)
   // close the expression.
   if (isRightBrace(cursor.nextSignificantToken()))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return handleRightBracket(cursor, status, depth);
   }
   
   // Check for end of the document.
   if (cursor.isAtEndOfDocument())
      return;
   
   // Check for the end of the statement.
   DEBUG("*-* End of statement: " << cursor);
   DEBUG("*-* Next cursor     : " << cursor.nextToken());
   DEBUG("*-* Next significant: " << cursor.nextSignificantToken());
   
   // Note that, following a statement, _any_ kind of whitespace will end
   // the expression, not just newlines.
   if (cursor.isAtEndOfExpression())
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      if (cursor.isAtEndOfDocument())
         return;
      
      if (depth == status.stack().size())
         return;
      else
         return doHandleExpression(cursor, status, depth);
   }
   
   // Shouldn't get here!
   UNEXPECTED_TOKEN(cursor, status);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
}

// A function argument list is of the form:
//
//   '(' (<f-expr> (',' <f-expr>)*)? ')'
//
// where <f-expr> is of the form:
//
//    (<id> | <id> '=' <expr>)
//
// with comma delimiters between the 'function' expressions.
void handleFunctionParameterList(TokenCursor& cursor,
                                 ParseStatus& status)
{
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
   
   // empty argument list
   if (cursor.isType(RToken::RPAREN))
      return;
   
   // empty names forbidden
   if (cursor.isType(RToken::COMMA))
      status.lint().unexpectedToken(cursor);
   
   if (!cursor.isType(RToken::ID))
      status.lint().unexpectedToken(cursor);
   
   status.node()->addDefinedSymbol(cursor);
   
   handleFunctionParameter(cursor, status);
   while (cursor.isType(RToken::COMMA))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      status.node()->addDefinedSymbol(cursor);
      handleFunctionParameter(cursor, status);
   }
}

void handleFunctionParameter(TokenCursor& cursor,
                             ParseStatus& status)
{
   ENSURE_TYPE(cursor, status, RToken::ID);
   const AnnotatedRToken& next = cursor.nextSignificantToken();
   
   // Check for comma or ')' (meaing we just have an identifier)
   if (next.isType(RToken::COMMA) ||
       next.isType(RToken::RPAREN))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      return;
   }
   
   // If an '=' follows, we have a default argument.
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_CONTENT(cursor, status, L"=");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   
   // Return for default argument as missing, e.g.
   //
   //    function(a = )
   if (cursor.isType(RToken::COMMA) ||
       cursor.isType(RToken::RPAREN))
   {
      status.lint().unexpectedToken(cursor);
   }
   
   handleExpression(cursor, status);
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
   
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
   
   // Check for an empty argument list.
   if (isRightBrace(cursor))
   {
      status.pop(cursor);
      if (cursor.contentAsUtf8() != rhs)
         status.lint().unexpectedToken(cursor, string_utils::utf8ToWide(rhs));
      return;
   }
   
   while (cursor.isType(RToken::COMMA))
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   
   // Check for a single argument.
   DEBUG("**** Before First arg: " << cursor);
   handleArgument(cursor, status);
   DEBUG("**** After First arg : " << cursor);
   
   // We should now lie either on a comma, or on the closing bracket.
   while (cursor.isType(RToken::COMMA))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      handleArgument(cursor, status);
   }
   
   ENSURE_CONTENT(cursor, status, string_utils::utf8ToWide(rhs));
   status.pop(cursor);
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
      handleIdentifier(cursor, status);
      
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
   
   // Don't cache identifiers if the previous or next tokens
   // are 'extraction' operators (e.g. '$').
   //
   // TODO: Handle namespaced symbols explicitly.
   if (isExtractionOperator(cursor.previousSignificantToken()) ||
       isExtractionOperator(cursor.nextSignificantToken()))
      return;
   
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
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   handleFunctionParameterList(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   handleExpression(cursor, status);
   
   // Close the function scope.
   status.setParentAsCurrent();
}



} // end anonymous namespace

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
   
   return ParseResults(status.root(), status.lint());
}

} // namespace r_util
} // namespace core
} // namespace rstudio
