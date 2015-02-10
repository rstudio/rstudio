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

// #define RSTUDIO_ENABLE_PROFILING
// #define RSTUDIO_ENABLE_DEBUG_MACROS
#define RSTUDIO_DEBUG_LABEL "parser"
#include <core/Macros.hpp>

#include <core/r_util/RParser.hpp>
#include <core/r_util/RTokenCursor.hpp>

#include <boost/timer/timer.hpp>

namespace rstudio {
namespace core {
namespace r_util {

using namespace token_utils;

void LintItems::dump()
{
   for (std::size_t i = 0; i < lintItems_.size(); ++i)
      std::cerr << lintItems_[i].message << std::endl;
}

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

std::string& complement(const std::string& bracket)
{
   return s_complements[bracket];
}

std::map<std::wstring, std::wstring> makeWideComplementMap()
{
   std::map<std::wstring, std::wstring> m;
   m[L"["]  = L"]";
   m[L"("]  = L")";
   m[L"{"]  = L"}";
   m[L"[["] = L"]]";
   m[L"]"]  = L"[";
   m[L")"]  = L"(";
   m[L"}"]  = L"{";
   m[L"]]"] = L"[[";
   return m;
}

static std::map<std::wstring, std::wstring> s_wcomplements =
      makeWideComplementMap();

std::wstring& wideComplement(const std::wstring& bracket)
{
   return s_wcomplements[bracket];

} // end anonymous namespace

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
         DEBUG("(" << __LINE__ << "): Expected "                               \
                   << string_utils::wideToUtf8(typeToWideString(__TYPE__)));   \
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

} // anonymous namespace

void doParse(TokenCursor&, ParseStatus&);

ParseResults parse(const std::string& rCode,
                   const ParseOptions& parseOptions)
{
   return parse(string_utils::utf8ToWide(rCode), parseOptions);
}

ParseResults parse(const std::wstring& rCode,
                   const ParseOptions& parseOptions)
{
   using namespace boost::timer;
   
   if (rCode.empty() ||
       rCode.find_first_not_of(L" \n\t\v") == std::string::npos)
   {
      return ParseResults();
   }
   
   TIMER(timer);
   
   RTokens rTokens(rCode);
   REPORT(timer, "Tokenization");
   
   ParseStatus status(parseOptions);
   TokenCursor cursor(rTokens);
   
   doParse(cursor, status);
   REPORT(timer, "Parse");
   
   if (status.node()->getParent() != NULL)
   {
      DEBUG("** Parent is not null (not at top level): failed to close all scopes?");
      status.lint().unexpectedEndOfDocument(cursor.currentToken());
   }
   
   return ParseResults(status.root(), status.lint());
}

namespace {

bool closesArgumentList(const TokenCursor& cursor,
                        const ParseStatus& status)
{
   switch (status.currentState())
   {
   case ParseStatus::ParseStateParenArgumentList:
      return cursor.isType(RToken::RPAREN);
   case ParseStatus::ParseStateSingleBracketArgumentList:
      return cursor.isType(RToken::RBRACE);
   case ParseStatus::ParseStateDoubleBracketArgumentList:
      return cursor.isType(RToken::RDBRACKET);
   default:
      return false;
   }
}

void checkBinaryOperatorWhitespace(TokenCursor& cursor,
                                   ParseStatus& status)
{
   // There should not be whitespace around extraction operators.
   //
   //    x $ foo
   //
   // is bad style.
   if (isExtractionOperator(cursor))
   {
      if (isWhitespace(cursor.previousToken()) ||
          isWhitespace(cursor.nextToken()))
      {
         status.lint().unexpectedWhitespaceAroundOperator(cursor);
      }
   }
   
   // There should be whitespace around other operators.
   //
   //    x+1
   //
   // is bad style.
   else
   {
      if (!isWhitespace(cursor.previousToken()) ||
          !isWhitespace(cursor.nextToken()))
      {
         status.lint().expectedWhitespaceAroundOperator(cursor);
      }
   }
}

} // anonymous namespace

#define GOTO_INVALID_TOKEN(__CURSOR__)                                         \
   do                                                                          \
   {                                                                           \
      DEBUG("Invalid token: " << __CURSOR__);                                  \
      goto INVALID_TOKEN;                                                      \
   } while (0)

void doParse(TokenCursor& cursor,
             ParseStatus& status)
{
   DEBUG("Beginning parse...");
   // Return early if the document is empty (only whitespace or comments)
   if (cursor.isAtEndOfDocument())
      return;
   
   cursor.fwdOverWhitespaceAndComments();
   bool startedWithUnaryOperator = false;
   
   goto START;
   
   while (true)
   {
      
START:
      
      DEBUG("Start: " << cursor);
      // Move over unary operators -- any sequence is valid,
      // but certain tokens are not accepted following
      // unary operators.
      startedWithUnaryOperator = false;
      while (isValidAsUnaryOperator(cursor))
      {
         startedWithUnaryOperator = true;
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
      }
      
      // Check for keywords.
      if (cursor.contentEquals(L"function"))
         goto FUNCTION_START;
      else if (cursor.contentEquals(L"for"))
         goto FOR_START;
      else if (cursor.contentEquals(L"while"))
         goto WHILE_START;
      else if (cursor.contentEquals(L"if"))
         goto IF_START;
      
      // Left paren.
      if (cursor.isType(RToken::LPAREN))
      {
         status.pushState(ParseStatus::ParseStateWithinParens);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
         goto START;
      }
      
      // Left bracket.
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateWithinBraces);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
         goto START;
      }
      
      // Comma. Only valid within argument lists.
      if (cursor.isType(RToken::COMMA))
      {
         if (startedWithUnaryOperator)
            GOTO_INVALID_TOKEN(cursor);
         
         // Commas can close statements, when within argument lists.
         switch (status.currentState())
         {
         case ParseStatus::ParseStateParenArgumentList:
         case ParseStatus::ParseStateSingleBracketArgumentList:
         case ParseStatus::ParseStateDoubleBracketArgumentList:
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto ARGUMENT_START;
         case ParseStatus::ParseStateFunctionArgumentList:
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto FUNCTION_ARGUMENT_START;
         default:
            GOTO_INVALID_TOKEN(cursor);
         }
      }
      
      // Right paren, or right bracket.
      // Closes a parenthetical scope of some kind:
      //
      //    1. Argument list (function def'n or call)
      //    2. Control flow condition
      //    3. Parens wrapping statement
      if (canCloseArgumentList(cursor))
      {
         DEBUG("** canCloseArgumentList: " << cursor);
         DEBUG("State: " << status.currentStateAsString());
         
         if (startedWithUnaryOperator)
            GOTO_INVALID_TOKEN(cursor);
         
         switch (status.currentState())
         {
         case ParseStatus::ParseStateParenArgumentList:
         case ParseStatus::ParseStateSingleBracketArgumentList:
         case ParseStatus::ParseStateDoubleBracketArgumentList:
            goto ARGUMENT_LIST_END;
         case ParseStatus::ParseStateFunctionArgumentList:
            goto FUNCTION_ARGUMENT_LIST_END;
         case ParseStatus::ParseStateIfCondition:
            goto IF_CONDITION_END;
         case ParseStatus::ParseStateForCondition:
            goto FOR_CONDITION_END;
         case ParseStatus::ParseStateWhileCondition:
            goto WHILE_CONDITION_END;
         case ParseStatus::ParseStateWithinParens:
            DEBUG("Within parens");
            status.popState();
            while (status.isInControlFlowStatement()) status.popState();
            if (cursor.isAtEndOfDocument()) return;
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            if (isBinaryOp(cursor))
               goto BINARY_OPERATOR;
            else if (canOpenArgumentList(cursor))
               goto ARGUMENT_LIST;
            goto START;
         default:
            GOTO_INVALID_TOKEN(cursor);
         }
      }
      
      // Right brace. Closes an expression and parent statements, as in
      // e.g.
      //
      //    if (foo) { while (1) 1 }
      //
      if (cursor.isType(RToken::RBRACE))
      {
         if (startedWithUnaryOperator)
            GOTO_INVALID_TOKEN(cursor);
         
         if (status.isInParentheticalScope())
            status.lint().unexpectedToken(cursor, L")");
         
         // Check for 'else' following for 'if' expressions.
         if (status.currentState() == ParseStatus::ParseStateIfExpression &&
             cursor.nextSignificantToken().contentEquals(L"else"))
         {
            status.popState();
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto START;
         }
         
         // Close an explicit expression, and any parent
         // control flow statements.
         status.popState();
         while (status.isInControlFlowStatement()) status.popState();
         if (cursor.isAtEndOfDocument()) return;
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         if (isBinaryOp(cursor) &&
             status.currentState() == ParseStatus::ParseStateWithinBraces)
         {
            goto BINARY_OPERATOR;
         }

         if (canOpenArgumentList(cursor))
            goto ARGUMENT_LIST;

         if (cursor.isType(RToken::LBRACE))
            GOTO_INVALID_TOKEN(cursor);
         
         goto START;
      }
      
      // Semicolon. Only valid following statements, or within
      // compound expressions (started by open '{'). Can end
      // control flow statements.
      if (cursor.isType(RToken::SEMI))
      {
         // Pop the state if we're in a control flow statement.
         while (status.isInControlFlowStatement())
            status.popState();
         
         if (startedWithUnaryOperator)
            GOTO_INVALID_TOKEN(cursor);
         
         if (status.isInParentheticalScope())
            GOTO_INVALID_TOKEN(cursor);
         
         // Ensure that we don't have excess semi-colons following.
         MOVE_TO_NEXT_TOKEN(cursor, status);
         if (isBlank(cursor))
            MOVE_TO_NEXT_TOKEN(cursor, status);
         
         while (cursor.isType(RToken::SEMI))
         {
            status.lint().unexpectedToken(cursor);
            MOVE_TO_NEXT_TOKEN(cursor, status);
            if (isBlank(cursor))
               MOVE_TO_NEXT_TOKEN(cursor, status);
         }
         
         if (isWhitespace(cursor))
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         
         goto START;
      }
      
      // Newlines can end statements.
      if (status.isInControlFlowStatement() &&
          cursor.contentContains(L'\n'))
      {
         status.popState();
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         goto START;
      }
      
      // Identifier-like symbol. (id, string, number)
      if (isValidAsIdentifier(cursor))
      {
         DEBUG("-- Identifier -- " << cursor);
         if (cursor.isAtEndOfDocument())
         {
            while (status.isInControlFlowStatement())
               status.popState();
            return;
         }
         
         if (cursor.isType(RToken::ID))
         {
            handleIdentifier(cursor, status);
         }
         
         // Identifiers following identifiers on the same line is
         // illegal (except for else), e.g.
         //
         //    a b c                      /* illegal */
         //    if (foo) bar else baz      /* legal */
         //
         // Check for the 'else' special case first, then the
         // other cases.
         if (status.currentState() == ParseStatus::ParseStateIfStatement &&
             cursor.nextSignificantToken().contentEquals(L"else"))
         {
            DEBUG("-- Found 'else' following end of 'if' statement");
            status.popState();
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto START;
         }
         
         if (cursor.nextSignificantToken().row() == cursor.row() &&
             isValidAsIdentifier(cursor.nextSignificantToken()))
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            GOTO_INVALID_TOKEN(cursor);
         }
         
         // Check to see if we're in a statement, and the next
         // token contains a newline. We check this first as
         // normally newlines are considered non-significant;
         // they are significant in this context.
         if ((status.isInControlFlowStatement() ||
              status.currentState() == ParseStatus::ParseStateTopLevel) &&
             (cursor.nextToken().contentContains(L'\n') ||
              cursor.isAtEndOfDocument()))
         {
            while (status.isInControlFlowStatement())
               status.popState();
            if (cursor.isAtEndOfDocument()) return;
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto START;
         }
         
         // Move to the next token.
         bool isNumber = cursor.isType(RToken::NUMBER);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         
         // Binary operators continue the statement.
         if (isBinaryOp(cursor))
            goto BINARY_OPERATOR;
         
         // Identifiers followed by brackets are function calls.
         // Only non-numeric symbols can be function calls.
         if (!isNumber && canOpenArgumentList(cursor))
         {
            goto ARGUMENT_LIST;
         }
         
         // Statement implicitly ended by closing bracket following.
         // Note that this will end _all_ control flow statements, 
         // e.g.
         //
         //    if (foo) if (bar) if (baz) bat
         //
         // The 'bat' identifier (well, the newline following)
         // closes all statements.
         if (status.isInControlFlowStatement() && (
             isRightBracket(cursor) ||
             isComma(cursor)))
         {
            while (status.isInControlFlowStatement())
               status.popState();
         }
         
         goto START;
         
      }
      
      GOTO_INVALID_TOKEN(cursor);
      
BINARY_OPERATOR:
      
      checkBinaryOperatorWhitespace(cursor, status);
      if (isExtractionOperator(cursor))
      {
         if (!(cursor.nextSignificantToken().isType(RToken::ID) ||
              (cursor.nextSignificantToken().isType(RToken::STRING))))
         {
            status.lint().unexpectedToken(cursor.nextSignificantToken());
         }
      }
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      goto START;
      
      // Argument list for a function / subset call, e.g.
      //
      //    a()
      //    b[]
      //    c[[]]
      //
ARGUMENT_LIST:
      
      DEBUG("-- Begin argument list " << cursor);
      
      switch (cursor.type())
      {
      case RToken::LPAREN:
         status.pushState(ParseStatus::ParseStateParenArgumentList);
         break;
      case RToken::LBRACKET:
         status.pushState(ParseStatus::ParseStateSingleBracketArgumentList);
         break;
      case RToken::LDBRACKET:
         status.pushState(ParseStatus::ParseStateDoubleBracketArgumentList);
         break;
      default:
         GOTO_INVALID_TOKEN(cursor);
      }
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
ARGUMENT_START:
      
      while (cursor.isType(RToken::COMMA))
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
      if (closesArgumentList(cursor, status))
         goto ARGUMENT_LIST_END;
      
      goto START;
      
ARGUMENT_LIST_END:
      
      DEBUG("Argument list end: " << cursor);
      status.popState();
      
      // An argument list may end _two_ states, e.g. in this:
      //
      //    if (foo) a() else if (b()) b()
      //
      // We need to close both the 'if' and the argument list.
      if (status.isInControlFlowStatement())
      {
         bool hasElse =
               status.currentState() == ParseStatus::ParseStateIfStatement &&
               cursor.nextSignificantToken().contentEquals(L"else");
         
         status.popState();
         if (hasElse)
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto START;
         }
      }
      
      while (status.isInControlFlowStatement())
         status.popState();
      
      // Following the end of an argument list, we may see:
      //
      // 1. The end of the document / expression,
      // 2. Another argument list,
      // 3. A binary operator,
      // 4. Another closing bracket (closing a parent expression).
      //
      // NOTE: The following two expressions are parsed differently
      // at the top level.
      //
      //    foo(1)  (2)
      //    foo(2)\n(2)
      //
      // In the first case, this is identical to `foo(1)(2)`; in
      // the second, it's `foo(1); (2)`.
      if (cursor.isAtEndOfDocument())
         return;
      
      // Newlines end function calls at the top level.
      else if (status.isAtTopLevel() &&
               cursor.nextToken().contentContains(L'\n'))
      {
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         goto START;
      }
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
      if (isLeftBracket(cursor))
         goto ARGUMENT_LIST;
      else if (isBinaryOp(cursor))
         goto BINARY_OPERATOR;
      else
         goto START;
      
FUNCTION_START:
      
      DEBUG("** Function start ** " << cursor);
      ENSURE_CONTENT(cursor, status, L"function");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN);
      status.pushState(ParseStatus::ParseStateFunctionArgumentList);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      if (cursor.isType(RToken::RPAREN))
         goto FUNCTION_ARGUMENT_LIST_END;
      
FUNCTION_ARGUMENT_START:
      
      DEBUG("** Function argument start");
      if (cursor.isType(RToken::ID) &&
          cursor.nextSignificantToken().contentEquals(L"="))
      {
         status.node()->addDefinedSymbol(cursor, status.getCachedPosition());
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         goto START;
      }
      
      if (cursor.isType(RToken::ID))
      {
         status.node()->addDefinedSymbol(cursor, status.getCachedPosition());
         if (cursor.nextSignificantToken().isType(RToken::COMMA))
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
             goto FUNCTION_ARGUMENT_START;
         }
         
         if (cursor.nextSignificantToken().isType(RToken::RPAREN))
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      
FUNCTION_ARGUMENT_LIST_END:
      
      ENSURE_TYPE(cursor, status, RToken::RPAREN);
      status.popState();
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateFunctionExpression);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      else
         status.pushState(ParseStatus::ParseStateFunctionStatement);
      goto START;
      
FOR_START:
      
      DEBUG("For start: " << cursor);
      ENSURE_CONTENT(cursor, status, L"for");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::ID);
      status.node()->addDefinedSymbol(cursor, cursor.currentPosition());
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      ENSURE_CONTENT(cursor, status, L"in");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      status.pushState(ParseStatus::ParseStateForCondition);
      goto START;
      
FOR_CONDITION_END:
      
      DEBUG("** For condition end ** " << cursor);
      ENSURE_TYPE(cursor, status, RToken::RPAREN);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      status.popState();
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateForExpression);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      else
         status.pushState(ParseStatus::ParseStateForStatement);
      goto START;
      
WHILE_START:
      
      DEBUG("** While start **");
      ENSURE_CONTENT(cursor, status, L"while");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN);
      status.pushState(ParseStatus::ParseStateWhileCondition);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      DEBUG("** Entering while condition: " << cursor);
      goto START;
      
WHILE_CONDITION_END:
      
      DEBUG("** While condition end ** " << cursor);
      ENSURE_TYPE(cursor, status, RToken::RPAREN);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      status.popState();
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateWhileExpression);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      else
         status.pushState(ParseStatus::ParseStateWhileStatement);
      goto START;
 
IF_START:
      
      DEBUG("** If start ** " << cursor);
      ENSURE_CONTENT(cursor, status, L"if");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      status.pushState(ParseStatus::ParseStateIfCondition);
      goto START;
      
IF_CONDITION_END:
      
      DEBUG("** If condition end ** " << cursor);
      ENSURE_TYPE(cursor, status, RToken::RPAREN);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      status.popState();
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateIfExpression);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      else
         status.pushState(ParseStatus::ParseStateIfStatement);
      goto START;
      
INVALID_TOKEN:
      
      status.lint().unexpectedToken(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   }
   return;
}

} // namespace r_util
} // namespace core
} // namespace rstudio
