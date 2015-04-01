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

// We use a couple internal R functions here; in particular,
// simple accessors (which we know will not longjmp)
#define R_INTERNAL_FUNCTIONS

#include <core/Macros.hpp>
#include <core/algorithm/Set.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

#include "SessionRParser.hpp"
#include "SessionRTokenCursor.hpp"

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <boost/container/flat_set.hpp>
#include <boost/timer/timer.hpp>
#include <boost/bind.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rparser {

void LintItems::dump()
{
   for (std::size_t i = 0; i < lintItems_.size(); ++i)
      std::cerr << lintItems_[i].message << std::endl;
}

using namespace core;
using namespace core::r_util;
using namespace core::r_util::token_utils;
using namespace token_cursor;

namespace {

bool isWithinNseFunction(const ParseStatus& status)
{
   // Check if we've inferred the current function to be an NSE function
   if (status.withinNseCall())
      return true;
   
   return false;
}

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

bool isDataTableSingleBracketCall(RTokenCursor& cursor)
{
   if (!cursor.contentEquals(L"["))
      return false;
   
   RTokenCursor startCursor = cursor.clone();
   
   // Move off of '['
   if (!startCursor.moveToPreviousSignificantToken())
      return false;
   
   // Find start of evaluation (e.g. moving over '$' and friends)
   if (!startCursor.moveToStartOfEvaluation())
      return false;
   
   // Get the string encompassing the call
   std::string objectString = string_utils::wideToUtf8(std::wstring(
            startCursor.currentToken().begin(),
            cursor.currentToken().begin()));
   
   // Get the object and check if it inherits from data.table
   r::exec::RFunction getAnywhere(".rs.getAnywhere");
   getAnywhere.addParam(objectString);
   
   SEXP resultSEXP;
   r::sexp::Protect protect;
   Error error = getAnywhere.call(&resultSEXP, &protect);
   if (error)
      return false;
   
   return r::sexp::inherits(resultSEXP, "data.table");
}

class NSEDatabase : boost::noncopyable
{
public:
   
   NSEDatabase()
   {
   }
   
   void add(SEXP symbolSEXP, bool performsNse)
   {
      database_[address(symbolSEXP)] = performsNse;
   }
   
   bool isKnownToPerformNSE(SEXP symbolSEXP)
   {
      return database_.count(address(symbolSEXP)) &&
             database_[address(symbolSEXP)];
   }
   
   bool isKnownNotToPerformNSE(SEXP symbolSEXP)
   {
      return database_.count(address(symbolSEXP)) &&
             !database_[address(symbolSEXP)];
   }
   
   bool isNotYetCached(SEXP symbolSEXP)
   {
      return !database_.count(address(symbolSEXP));
   }
   
private:
   
   uintptr_t address(SEXP objectSEXP)
   {
      return reinterpret_cast<uintptr_t>(objectSEXP);
   }
   
   std::map<uintptr_t, bool> database_;
};

NSEDatabase& nseDatabase()
{
   static NSEDatabase instance;
   return instance;
}

// TODO: Implement per-parse cache so that we avoid repeated
// evaluations of the same symbol
SEXP resolveFunctionAtCursor(RTokenCursor cursor,
                             r::sexp::Protect* pProtect,
                             bool* pCacheable = NULL)
{
   DEBUG("--- Resolve function call: " << cursor);
   
   if (canOpenArgumentList(cursor))
      if (!cursor.moveToPreviousSignificantToken())
         return R_UnboundValue;
   
   // Attempt to resolve (potentially evaluate) the symbol, or statement,
   // forming the function call.
   SEXP symbolSEXP = R_UnboundValue;
   if (pCacheable) *pCacheable = true;
   
   if (cursor.isSimpleCall())
   {
      std::string symbol = string_utils::strippedOfQuotes(
               cursor.contentAsUtf8());
      
      symbolSEXP = r::sexp::findFunction(symbol);
   }
   else if (cursor.isSimpleNamespaceCall())
   {
      std::string symbol = string_utils::strippedOfQuotes(
               cursor.contentAsUtf8());
      
      std::string ns = string_utils::strippedOfQuotes(
               cursor.previousSignificantToken(2).contentAsUtf8());
      
      symbolSEXP = r::sexp::findFunction(symbol, ns);
   }
   else
   {
      if (pCacheable) *pCacheable = false;
      
      std::string call = string_utils::wideToUtf8(cursor.getCallingString());
      
      // Don't evaluate nested function calls.
      if (call.find('(') != std::string::npos)
         return R_UnboundValue;
      
      Error error = r::exec::evaluateString(call, &symbolSEXP, pProtect);
      if (error)
      {
         DEBUG("- Failed to evaluate call '" << call << "'");
         return R_UnboundValue;
      }
   }
   
   return symbolSEXP;
}

std::set<std::wstring> makeWideNsePrimitives()
{
   std::set<std::wstring> wide;
   const std::set<std::string>& nsePrimitives = r::sexp::nsePrimitives();
   
   BOOST_FOREACH(const std::string& primitive, nsePrimitives)
   {
      wide.insert(string_utils::utf8ToWide(primitive));
   }

   return wide;
}

const std::set<std::wstring>& wideNsePrimitives()
{
   static std::set<std::wstring> wideNsePrimitives = makeWideNsePrimitives();
   return wideNsePrimitives;
}

bool maybePerformsNSE(RTokenCursor cursor)
{
   if (!cursor.nextSignificantToken().isType(RToken::LPAREN))
      return false;
   
   if (!cursor.moveToNextSignificantToken())
      return false;
   
   RTokenCursor endCursor = cursor.clone();
   if (!endCursor.fwdToMatchingToken())
      return false;
   
   const std::set<std::wstring>& nsePrimitives = wideNsePrimitives();
   
   const RTokens& rTokens = cursor.tokens();
   std::size_t offset = cursor.offset();
   std::size_t endOffset = endCursor.offset();
   while (offset != endOffset)
   {
      const RToken& token = rTokens.at(offset);

      if (token.isType(RToken::ID))
      {
         const RToken& next = rTokens.at(offset + 1);
         if (next.isType(RToken::LPAREN) &&
             nsePrimitives.count(token.content()))
         {
            return true;
         }
      }
      ++offset;
   }
   return false;
}

bool mightPerformNonstandardEvaluation(const RTokenCursor& origin,
                                       ParseStatus& status)
{
   RTokenCursor cursor = origin.clone();
   
   if (canOpenArgumentList(cursor))
      if (!cursor.moveToPreviousSignificantToken())
         return false;
   
   if (!canOpenArgumentList(cursor.nextSignificantToken()))
      return false;
   
   // TODO: How should we resolve conflicts between a function on
   // the search path with some set of arguments, versus an identically
   // named function in the source document which has not yet been sourced?
   //
   // For now, we prefer the current source document + the source
   // database, and then use the search path after if necessary.
   const ParseNode* pNode;
   if (status.node()->findFunction(cursor.contentAsUtf8(),
                                   cursor.currentPosition(),
                                   &pNode))
   {
      DEBUG("--- Found function: '" << pNode->name());
      if (cursor.moveToPosition(pNode->position()))
         if (maybePerformsNSE(cursor))
            return true;
   }
   
   bool cacheable = true;
   r::sexp::Protect protect;
   SEXP symbolSEXP = resolveFunctionAtCursor(cursor, &protect, &cacheable);
   
   if (symbolSEXP == R_UnboundValue || symbolSEXP == R_NilValue)
      return false;
   
   NSEDatabase& nseDb = nseDatabase();
   if (cacheable)
   {
      if (nseDb.isKnownToPerformNSE(symbolSEXP))
         return true;
      else if (nseDb.isKnownNotToPerformNSE(symbolSEXP))
         return false;
   }
   
   bool result = r::sexp::maybePerformsNSE(symbolSEXP);
   if (cacheable)
      nseDb.add(symbolSEXP, result);
   
   return result;
}

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

#define ENSURE_TYPE_NOT(__CURSOR__, __STATUS__, __TYPE__)                      \
   do                                                                          \
   {                                                                           \
      if (__CURSOR__.isType(__TYPE__))                                         \
      {                                                                        \
         DEBUG("(" << __LINE__ << "): Unexpected "                             \
                   << string_utils::wideToUtf8(typeToWideString(__TYPE__)));   \
         __STATUS__.lint().unexpectedToken(__CURSOR__);                        \
         return;                                                               \
      }                                                                        \
   } while (0)


#define UNEXPECTED_TOKEN(__CURSOR__, __STATUS__)                               \
   do                                                                          \
   {                                                                           \
      DEBUG("(" << __LINE__ << "): Unexpected token (" << __CURSOR__ << ")");  \
      __STATUS__.lint().unexpectedToken(__CURSOR__);                           \
   } while (0)

void lookAheadAndWarnOnUsagesOfSymbol(const RTokenCursor& startCursor,
                                      RTokenCursor& clone,
                                      ParseStatus& status)
{
   std::size_t braceBalance = 0;
   std::wstring symbol = startCursor.content();
   
   do
   {
      // Skip over 'for' arg-list
      if (clone.contentEquals(L"for"))
      {
         if (!clone.moveToNextSignificantToken())
            return;
         
         if (!clone.fwdToMatchingToken())
            return;
         
         continue;
      }
      
      // Skip over functions
      if (clone.contentEquals(L"function"))
      {
         if (!clone.moveToNextSignificantToken())
            return;
         
         if (!clone.fwdToMatchingToken())
            return;
         
         if (!clone.moveToNextSignificantToken())
            return;
         
         if (clone.isType(RToken::LBRACE))
         {
            if (!clone.fwdToMatchingToken())
               return;
         }
         else
         {
            if (!clone.moveToEndOfStatement(status.isInParentheticalScope()))
               return;
         }
         
         if (clone.isAtEndOfExpression())
            return;
         
         continue;
      }
      
      const RToken& prev = clone.previousSignificantToken();
      const RToken& next = clone.nextSignificantToken();
      
      if (clone.contentEquals(symbol) &&
          !isRightAssign(prev) &&
          !isLeftAssign(next) &&
          !isLeftBracket(next) &&
          !isExtractionOperator(prev) &&
          !isExtractionOperator(next))
      {
         status.lint().noSymbolNamed(clone);
      }
      
      braceBalance += isLeftBracket(clone);
      if (isRightBracket(clone))
      {
         --braceBalance;
         if (isBinaryOp(clone.nextSignificantToken()))
         {
            if (!clone.moveToNextSignificantToken())
               return;
            continue;
         }
         
         if (braceBalance <= 0)
            return;
      }
      
      if (clone.nextToken().contentContains(L'\n') ||
          clone.isType(RToken::SEMI))
      {
         return;
      }
      
   } while (clone.moveToNextSignificantToken());
}

void handleIdentifier(RTokenCursor& cursor,
                      ParseStatus& status)
{
   // Check to see if we are defining a symbol at this location.
   // Note that both string and id are valid for assignments.
   bool skipReferenceChecks =
         isWithinNseFunction(status) ||
         (status.isInArgumentList() && !status.parseOptions().lintRFunctions());
   
   // Don't cache identifiers if:
   //
   // 1. The previous token is an extraction operator, e.g. `$`, `@`, `::`, `:::`,
   // 2. The following token is a namespace operator `::`, `:::`
   //
   // TODO: Handle namespaced symbols (e.g. for package lookup) and
   // provide lint appropriately.
   if (isExtractionOperator(cursor.previousSignificantToken()) ||
       isNamespace(cursor.nextSignificantToken()))
   {
      DEBUG("--- Symbol preceded by extraction op; not adding");
      return;
   }
   
   if (cursor.isType(RToken::ID) ||
       cursor.isType(RToken::STRING))
   {
      // Before we add a reference to this symbol, run ahead
      // in the statement and see if we reference that identifier
      // anywhere. We want to identify and warn about the case
      // where someone writes:
      //
      //    x <- x + 1
      //
      // and `x` isn't actually defined yet. Note that we need to be careful
      // because, for example,
      //
      //    foo <- foo()
      //
      // is legal, and `foo` might be an object on the search path. (For
      // variables, we prefer not searching the search path)
      if (!skipReferenceChecks &&
          isLocalLeftAssign(cursor.nextSignificantToken()) &&
          !status.node()->symbolHasDefinitionInTree(cursor.contentAsUtf8(), cursor.currentPosition()))
      {
         RTokenCursor clone = cursor.clone();
         if (clone.moveToNextSignificantToken() &&
             clone.moveToNextSignificantToken())
         {
            lookAheadAndWarnOnUsagesOfSymbol(cursor, clone, status);
         }
      }
      
      if (isLocalLeftAssign(cursor.nextSignificantToken()) ||
          isLocalRightAssign(cursor.previousSignificantToken()))
      {
         DEBUG("--- Adding definition for symbol");
         status.node()->addDefinedSymbol(cursor);
      }
      
   }
   
   // If this is truly an identifier, add a reference.
   if (!skipReferenceChecks && cursor.isType(RToken::ID))
   {
      DEBUG("--- Adding reference to symbol");
      status.node()->addReferencedSymbol(cursor);
   }
}

} // anonymous namespace

void doParse(RTokenCursor&, ParseStatus&);

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
       rCode.find_first_not_of(L" \r\n\t\v") == std::string::npos)
   {
      return ParseResults();
   }
   
   RTokens rTokens(rCode);
   
   ParseStatus status(parseOptions);
   RTokenCursor cursor(rTokens);
   
   doParse(cursor, status);
   
   if (status.node()->getParent() != NULL)
   {
      DEBUG("** Parent is not null (not at top level): failed to close all scopes?");
      status.lint().unexpectedEndOfDocument(cursor.currentToken());
   }
   
   return ParseResults(status.root(), status.lint());
}

namespace {

bool closesArgumentList(const RTokenCursor& cursor,
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

void checkBinaryOperatorWhitespace(RTokenCursor& cursor,
                                   ParseStatus& status)
{
   // There should not be whitespace around extraction operators.
   //
   //    x $ foo
   //
   // is bad style. Note that ':' is the other 'no-whitespace-preferred'
   // binary operator.
   bool isExtraction = isExtractionOperator(cursor);
   bool isColon = cursor.contentEquals(L":");
   if (isExtraction || isColon)
   {
      if (isWhitespace(cursor.previousToken()) ||
          isWhitespace(cursor.nextToken()))
      {
         status.lint().unexpectedWhitespaceAroundOperator(cursor);
      }
      
      // Extraction operators must be followed by a string or an
      // identifier
      if (isExtraction)
      {
         const RToken& next = cursor.nextSignificantToken();
         if (!(next.isType(RToken::ID) || next.isType(RToken::STRING)))
            status.lint().unexpectedToken(next);
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

// Extract named arguments in a (tokenized) function call, e.g.
//
//     foo(alpha = 1, beta = 2, gamma = 3)
//         ^^^^^      ^^^^      ^^^^^
//
std::vector<std::string> extractNamedArguments(RTokenCursor cursor)
{
   std::vector<std::string> result;
   
   if (isLeftBracket(cursor.nextSignificantToken()))
      if (!cursor.moveToNextSignificantToken())
         return result;
   
   if (!isLeftBracket(cursor))
      return result;
   
   if (!cursor.moveToNextSignificantToken())
      return result;
   
   // Extract the first argument.
   if (cursor.isType(RToken::ID) &&
       cursor.nextSignificantToken().contentEquals(L"="))
   {
      result.push_back(cursor.contentAsUtf8());
   }
   
   // Extract the rest of the arguments by finding the token sequence
   //
   //     , <id> =
   //
   // Bail once we find a closing paren, and walk over matching parens
   // to handle expressions within the function argument list.
   do
   {
      if (isRightBracket(cursor))
         break;
      
      if (cursor.fwdToMatchingToken())
         continue;
      
      if (cursor.isType(RToken::COMMA) &&
          cursor.nextSignificantToken().isType(RToken::ID) &&
          cursor.nextSignificantToken(2).contentEquals(L"="))
      {
         result.push_back(cursor.nextSignificantToken().contentAsUtf8());
      }
      
   } while (cursor.moveToNextSignificantToken());
   
   return result;
}

bool extractFormalsFromFunctionDefinition(RTokenCursor cursor,
                                          std::vector<std::string>* pNames)
{
   do
   {
      if (cursor.contentEquals(L"function"))
         break;
      
   } while (cursor.moveToNextSignificantToken());
   
   if (!cursor.moveToNextSignificantToken())
      return false;
   
   if (!cursor.isType(RToken::LPAREN))
      return false;
   
   if (!cursor.moveToNextSignificantToken())
      return false;
   
   if (cursor.isType(RToken::RPAREN))
      return true;
   
   if (cursor.isType(RToken::ID))
      pNames->push_back(cursor.contentAsUtf8());
   
   do
   {
      if (cursor.fwdToMatchingToken())
         continue;
      
      if (isRightBracket(cursor))
         return true;
      
      if (cursor.isType(RToken::COMMA) &&
          cursor.nextSignificantToken().isType(RToken::ID))
      {
         pNames->push_back(cursor.nextSignificantToken().contentAsUtf8());
      }
   } while (cursor.moveToNextSignificantToken());
   
   return false;
}

bool getFormalsAssociatedWithFunctionAtCursor(RTokenCursor cursor,
                                              ParseStatus& status,
                                              std::vector<std::string>* pNames)
{
   // If this is a direct call to a symbol, then first attempt to
   // find this function in the current document, or the source database.
   if (cursor.isSimpleCall())
   {
      if (cursor.isType(RToken::LPAREN))
         if (!cursor.moveToPreviousSignificantToken())
            return false;
      
      DEBUG("***** Attempting to resolve source function: '" << cursor.contentAsUtf8() << "'");
      const ParseNode* pNode;
      if (status.node()->findFunction(
             cursor.contentAsUtf8(),
             cursor.currentPosition(),
             &pNode))
      {
         DEBUG("***** Found function: '" << pNode->name() << "' at: " << pNode->position());
         if (cursor.moveToPosition(pNode->position()))
         {
            DEBUG("***** Moved to position");
            if (extractFormalsFromFunctionDefinition(cursor, pNames))
            {
               DEBUG("Extracted arguments");
               return true;
            }
         }
      }
   }
   
   // If the above failed, we'll fall back to evaluating and looking up
   // the symbol on the search path.
   r::sexp::Protect protect;
   SEXP functionSEXP = resolveFunctionAtCursor(cursor, &protect);
   if (functionSEXP == R_UnboundValue || !Rf_isFunction(functionSEXP))
      return false;
   
   // TODO: Handle primitives.
   if (Rf_isPrimitive(functionSEXP))
      return false;
   
   // Get the formals associated with this function.
   Error error = r::sexp::extractFormalNames(functionSEXP, pNames);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   return true;
}

void validateFunctionCall(RTokenCursor cursor,
                          ParseStatus& status)
{
   using namespace r::sexp;
   using namespace r::exec;
   
   if (!cursor.isType(RToken::LPAREN))
      return;
   
   RTokenCursor endCursor = cursor.clone();
   endCursor.fwdToMatchingToken();
   
   if (!cursor.moveToPreviousSignificantToken())
      return;
   
   std::vector<std::string> names;
   if (!getFormalsAssociatedWithFunctionAtCursor(cursor, status, &names))
      return;
   
   RTokenCursor startCursor = cursor.clone();
   startCursor.moveToStartOfEvaluation();
   
   // Bail if this function takes '...' -- TODO is to wire in
   // what we can for inferring a correct call.
   if (std::find(names.begin(), names.end(), "...") != names.end())
      return;
   
   // Get the arguments within the function call.
   std::vector<std::string> args = extractNamedArguments(cursor);
   
   // Figure out which named arguments specified by the user don't actually
   // match the names of the available formals.
   std::vector<std::string> noExactMatchArgs =
         core::algorithm::set_difference(args, names);
   
   // Of the unmatched arguments, figure out which are / aren't prefix matches.
   typedef std::pair<std::string, std::string> Pair;
   std::vector<Pair> prefixMatchedPairs;
   BOOST_FOREACH(const std::string& arg, noExactMatchArgs)
   {
      BOOST_FOREACH(const std::string& name, names)
      {
         if (string_utils::isPrefixOf(name, arg))
         {
            prefixMatchedPairs.push_back(std::make_pair(arg, name));
            break;
         }
      }
   }

   // Now, collect the arguments with no match at all.
   std::vector<std::string> prefixMatchedArgs;
   BOOST_FOREACH(const Pair& pair, prefixMatchedPairs)
   {
      prefixMatchedArgs.push_back(pair.first);
   }
   
   std::vector<std::string> unmatchedArgs =
         core::algorithm::set_difference(
            noExactMatchArgs,
            prefixMatchedArgs);
   
   // Collect warning + error messages. TODO: Do we want to keep these
   // separated so we can provide nicer markers?
   if (!prefixMatchedPairs.empty())
   {
      std::string prefix = prefixMatchedPairs.size() == 1 ?
               "partial match of argument: " :
               "partial match of arguments: ";
      
      std::string prefixMatched =
            "['" + prefixMatchedPairs[0].first + "' -> " +
             "'" + prefixMatchedPairs[0].second + "'";
      
      std::size_t n = prefixMatchedPairs.size();
      
      for (std::size_t i = 1; i < n; ++i)
         prefixMatched += ", '" + prefixMatchedPairs[i].first + "' -> " +
               "'" + prefixMatchedPairs[i].second + "'";
      
      prefixMatched += "]";
      
      status.lint().add(
               startCursor.row(),
               startCursor.column(),
               endCursor.row(),
               endCursor.column() + endCursor.length(),
               LintTypeWarning,
               prefix + prefixMatched);
   }
   
   if (!unmatchedArgs.empty())
   {
      std::string unmatched = "'" + boost::algorithm::join(unmatchedArgs, "', '") + "'";
      std::string prefix = unmatchedArgs.size() == 1 ?
               "unused argument: " :
               "unused arguments: ";
      
      status.lint().add(
               startCursor.row(),
               startCursor.column(),
               endCursor.row(),
               endCursor.column() + endCursor.length(),
               LintTypeError,
               prefix + unmatched);
   }
}

bool skipFormulas(RTokenCursor& cursor,
                  ParseStatus& status)
{
   if (cursor.nextSignificantToken().contentEquals(L"~"))
      cursor.moveToNextSignificantToken();
   
   if (cursor.contentEquals(L"~"))
   {
      if (cursor.moveToEndOfStatement(status.isInParentheticalScope()))
         return cursor.moveToNextSignificantToken();
      
   }
   
   return false;
}

// Enter a function scope, starting at the first paren associated
// with the 'function' token, e.g.
//
//    foo <- function(x, y, z) { ... }
//                   ^
//
// We do a bit of lookaround to get the symbol name.
void enterFunctionScope(RTokenCursor cursor,
                        ParseStatus& status)
{
   std::string symbol = "(unknown function)";
   Position position = cursor.currentPosition();
   
   if (cursor.moveToPreviousSignificantToken() &&
       cursor.contentEquals(L"function") &&
       cursor.moveToPreviousSignificantToken() &&
       isLeftAssign(cursor) &&
       cursor.moveToPreviousSignificantToken())
   {
      symbol = string_utils::wideToUtf8(cursor.getCallingString());
      position = cursor.currentPosition();
   }
   
   status.enterFunctionScope(symbol, position);
}

} // anonymous namespace

#define GOTO_INVALID_TOKEN(__CURSOR__)                                         \
   do                                                                          \
   {                                                                           \
      DEBUG("Invalid token: " << __CURSOR__);                                  \
      goto INVALID_TOKEN;                                                      \
   } while (0)

void doParse(RTokenCursor& cursor,
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
      
      DEBUG("== Current state: " << status.currentStateAsString());
      
      // We want to skip over formulas if necessary.
      if (skipFormulas(cursor, status))
      {
         if (cursor.isAtEndOfDocument())
            return;
      }
      
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
      else if (cursor.contentEquals(L"repeat"))
         goto REPEAT_START;
      
      // 'else'. Implicitly ending an 'if' statement.
      if (cursor.contentEquals(L"else"))
      {
         // Ensure that we're in an 'if' statement or expression.
         if (status.currentState() == ParseStatus::ParseStateIfStatement ||
             status.currentState() == ParseStatus::ParseStateIfExpression)
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto START;
         }
         else
         {
            GOTO_INVALID_TOKEN(cursor);
         }
      }
      
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
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
            goto ARGUMENT_START;
         case ParseStatus::ParseStateFunctionArgumentList:
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
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
            status.popState();
            
            if (cursor.isAtEndOfDocument())
               return;
            
            // Handle an 'else' following when we are closing an 'if' statement
            if (status.currentState() == ParseStatus::ParseStateIfStatement &&
                cursor.nextSignificantToken().contentEquals(L"else"))
            {
               status.popState();
               MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
               MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
               goto START;
            }
            
            // Check to see if this paren closes the current statement.
            // If so, it effectively closes any parent conditional statements.
            if (cursor.isAtEndOfStatement(status))
               while (status.isInControlFlowStatement())
                  status.popState();
            
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
            handleIdentifier(cursor, status);
         
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
         const RToken& next = cursor.nextSignificantToken();
         
         if (isBinaryOp(next))
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            goto BINARY_OPERATOR;
         }
         
         // Identifiers followed by brackets are function calls.
         // Only non-numeric symbols can be function calls.
         if (!isNumber && canOpenArgumentList(next))
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
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
             isRightBracket(next) ||
             isComma(next)))
         {
            while (status.isInControlFlowStatement())
               status.popState();
         }
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
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
      validateFunctionCall(cursor, status);
      
      // Update the current state.
      switch (cursor.type())
      {
      case RToken::LPAREN:
         status.pushFunctionCallState(
                  ParseStatus::ParseStateParenArgumentList,
                  cursor.previousSignificantToken().content());
         break;
      case RToken::LBRACKET:
         status.pushFunctionCallState(
                  ParseStatus::ParseStateSingleBracketArgumentList,
                  cursor.previousSignificantToken().content());
         break;
      case RToken::LDBRACKET:
         status.pushFunctionCallState(
                  ParseStatus::ParseStateDoubleBracketArgumentList,
                  cursor.previousSignificantToken().content());
         break;
      default:
         GOTO_INVALID_TOKEN(cursor);
      }
      
      // Skip over data.table `[` calls
      if (isDataTableSingleBracketCall(cursor))
      {
         cursor.fwdToMatchingToken();
         
         if (cursor.isAtEndOfDocument())
            return;
         
         goto ARGUMENT_LIST_END;
      }
      
      // If we're about to enter a function call that performs
      // non-standard evaluation, then we want to record symbol definitions
      // but _not_ lint symbol usage.
      if (mightPerformNonstandardEvaluation(cursor, status))
         status.pushNseCall(cursor.getCallingString());
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
ARGUMENT_START:
      
      while (cursor.isType(RToken::COMMA))
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
      if (closesArgumentList(cursor, status))
         goto ARGUMENT_LIST_END;
      
      // Step over named arguments.
      if (cursor.isType(RToken::ID) &&
          cursor.nextSignificantToken().contentEquals(L"="))
      {
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      }
      
      goto START;
      
ARGUMENT_LIST_END:
      
      DEBUG("Argument list end: " << cursor);
      DEBUG("== State: " << status.currentStateAsString());
      
      status.popState();
      status.popNseCall();
      
      DEBUG("== State: " << status.currentStateAsString());
      
      // An argument list may end _two_ states, e.g. in this:
      //
      //    if (foo) a() else if (b()) b()
      //
      // We need to close both the 'if' and the argument list.
      if (status.isInControlFlowStatement() &&
          !canContinueStatement(cursor.nextSignificantToken()))
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
      
      // Pop out of control flow statements if this ends the statement.
      if (cursor.isAtEndOfStatement(status))
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
      enterFunctionScope(cursor, status);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      if (cursor.isType(RToken::RPAREN))
         goto FUNCTION_ARGUMENT_LIST_END;
      
FUNCTION_ARGUMENT_START:
      
      DEBUG("** Function argument start");
      if (cursor.isType(RToken::ID) &&
          cursor.nextSignificantToken().contentEquals(L"="))
      {
         status.node()->addDefinedSymbol(cursor, status.node()->position());
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         goto START;
      }
      
      if (cursor.isType(RToken::ID))
      {
         status.node()->addDefinedSymbol(cursor, status.node()->position());
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
      ENSURE_TYPE_NOT(cursor, status, RToken::RPAREN);
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
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN);
      status.pushState(ParseStatus::ParseStateWhileCondition);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE_NOT(cursor, status, RToken::RPAREN);
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
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      ENSURE_TYPE_NOT(cursor, status, RToken::RPAREN);
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
      
REPEAT_START:
      
      DEBUG("** Repeat start ** " << cursor);
      ENSURE_CONTENT(cursor, status, L"repeat");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateRepeatExpression);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      else
         status.pushState(ParseStatus::ParseStateRepeatStatement);
      goto START;
      
INVALID_TOKEN:
      
      status.lint().unexpectedToken(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   }
   return;
}

} // namespace rparser
} // namespace modules
} // namespace session
} // namespace rstudio
