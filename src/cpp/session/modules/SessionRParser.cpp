/*
 * SessionRParser.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

// #define RSTUDIO_DEBUG_LABEL "r_parser"
// #define RSTUDIO_ENABLE_DEBUG_MACROS

// Define this if you want extra debug printing for how
// RStudio attempts to parse and diagnose glue expressions.
// #define RSTUDIO_ENABLE_GLUE_DEBUG

// We use a couple internal R functions here; in particular,
// simple accessors (which we know will not longjmp)
#define R_INTERNAL_FUNCTIONS

#include <fmt/format.h>

#include <boost/bind/bind.hpp>
#include <boost/container/flat_set.hpp>

#include <core/Debug.hpp>
#include <core/Macros.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/algorithm/Set.hpp>
#include <core/algorithm/Map.hpp>
#include <core/r_util/RTokenCursor.hpp>
#include <core/text/TextCursor.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "SessionRParser.hpp"
#include "SessionCodeSearch.hpp"

using namespace boost::placeholders;

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

void doParse(RTokenCursor&, ParseStatus&);

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

Error safeEvaluateString(const std::string& string,
                         SEXP* pSEXP,
                         r::sexp::Protect* pProtect)
{
   // don't evaluate pipe placeholders
   // https://github.com/rstudio/rstudio/issues/14713
   if (string == "_")
   {
      *pSEXP = R_NilValue;
      return Success();
   }
   
   // only evaluate strings that consist of identifiers + extraction
   // operators, e.g. 'foo$bar[[1]]'
   boost::regex reSafeEvaluation("^[a-zA-Z0-9_$@\\[\\]]+$");
   if (!regex_utils::search(string, reSafeEvaluation))
   {
      *pSEXP = R_NilValue;
      return Success();
   }
   
   return r::exec::evaluateString(
            string,
            pSEXP,
            pProtect,
            r::exec::EvalFlagsSuppressWarnings);
}

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
   
   if (objectString.find('(') != std::string::npos)
      return false;
   
   // avoid output leaking to console
   r::session::utils::SuppressOutputInScope scope;
   
   // Get the object and check if it inherits from data.table
   SEXP objectSEXP;
   r::sexp::Protect protect;
   Error error = safeEvaluateString(objectString, &objectSEXP, &protect);
   if (error)
      return false;
   
   return r::sexp::inherits(objectSEXP, "data.table");
}

class NSEDatabase : boost::noncopyable
{
public:
   
   NSEDatabase()
   {
      // Add in a set of 'known' NSE-performing functinos.
      for (const std::string& name : r::sexp::nsePrimitives())
      {
         addNseFunction(name, "base");
      }
      
      addNseFunction("data", "base");
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
   
   void addNseFunction(const std::string& name,
                       const std::string& ns)
   {
      add(r::sexp::findFunction(name, ns), true);
   }
   
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

SEXP resolveObjectAssociatedWithCall(RTokenCursor cursor,
                                     ParseStatus& status,
                                     r::sexp::Protect* pProtect,
                                     bool functionsOnly = false,
                                     bool* pCacheable = nullptr)
{
   DEBUG("--- Resolve function call: " << cursor);
   
   if (canOpenArgumentList(cursor))
      if (!cursor.moveToPreviousSignificantToken())
         return R_UnboundValue;
   
   if (pCacheable)
      *pCacheable = true;
   
   // Attempt to resolve (potentially evaluate) the symbol, or statement,
   // forming the function call.
   SEXP symbolSEXP = R_UnboundValue;
   
   if (cursor.isAssignmentCall())
   {
      // TODO: Right now our match.call doesn't understand how
      // to handle `fn(x) <- bar` calls, as we don't fully parse these
      // expressions into the associated `foo<-`(x, bar) call. By
      // not resolving a function in these situations we simply avoid
      // linting such calls.
      return R_UnboundValue;
   }
   else if (cursor.isSimpleCall())
   {
      DEBUG("Resolving as 'simple' call");
      std::string symbol = string_utils::strippedOfQuotes(
               cursor.contentAsUtf8());
      
      if (functionsOnly)
         symbolSEXP = r::sexp::findFunction(symbol);
      else
         symbolSEXP = r::sexp::findVar(symbol);
   }
   else if (cursor.isSimpleNamespaceCall())
   {
      DEBUG("Resolving as 'namespaced' call");
      
      RToken symbolToken = cursor.currentToken();
      RToken packageToken = cursor.previousSignificantToken(2);
      
      std::string symbol  = string_utils::strippedOfQuotes(symbolToken.contentAsUtf8());
      std::string package = string_utils::strippedOfQuotes(packageToken.contentAsUtf8());
      
      SEXP namespaceSEXP = status.parseOptions().isExplicit()
            ? r::sexp::asNamespace(package)
            : r::sexp::findNamespace(package);
 
      if (TYPEOF(namespaceSEXP) == ENVSXP)
      {
         DEBUG("Resolving: '" << package << ":::" << symbol << "'");
      
         if (functionsOnly)
            symbolSEXP = r::sexp::findFunction(symbol, package);
         else
            symbolSEXP = r::sexp::findVar(symbol, package);
      }
      else
      {
         DEBUG("Not resolving: '" << package << ":::" << symbol << "' (not available)");
         
         // Only display these warnings in 'explicit' requests (avoid being too noisy).
         if (status.parseOptions().isExplicit())
         {
            if (status.parseOptions().checkArgumentsToRFunctionCalls() ||
                status.parseOptions().lintRFunctions())
            {
               status.lint().packageNotLoaded(packageToken, symbolToken);
            }
         }
      }
   }
   else
   {
      DEBUG("Resolving as generic evaluation");
      if (pCacheable) *pCacheable = false;
      
      std::string call = string_utils::wideToUtf8(cursor.getEvaluationAssociatedWithCall());
      
      // Don't evaluate nested function calls.
      if (call.find('(') != std::string::npos)
         return R_UnboundValue;
      
      // avoid output leaking to console
      r::session::utils::SuppressOutputInScope scope;
      
      Error error = safeEvaluateString(call, &symbolSEXP, pProtect);
      if (error)
      {
         DEBUG("- Failed to evaluate call '" << call << "'");
         return R_UnboundValue;
      }
      
      if (functionsOnly && !Rf_isFunction(symbolSEXP))
         return R_UnboundValue;
   }
   
   // protect the discovered symbol here, just in case it was produced
   // by an active binding
   pProtect->add(symbolSEXP);
   
   return symbolSEXP;
}

SEXP resolveFunctionAssociatedWithCall(RTokenCursor cursor,
                                       ParseStatus& status,
                                       r::sexp::Protect* pProtect,
                                       bool* pCacheable = nullptr)
{
   return resolveObjectAssociatedWithCall(cursor, status, pProtect, true, pCacheable);
}

std::set<std::wstring> makeWideNsePrimitives()
{
   std::set<std::wstring> wide;
   const std::set<std::string>& nsePrimitives = r::sexp::nsePrimitives();
   
   for (const std::string& primitive : nsePrimitives)
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
      const RToken& token = rTokens.atUnsafe(offset);

      if (token.isType(RToken::ID))
      {
         const RToken& next = rTokens.atUnsafe(offset + 1);
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
   
   DEBUG("- Checking whether NSE performed here: " << cursor);
   
   // TODO: How should we resolve conflicts between a function on
   // the search path with some set of arguments, versus an identically
   // named function in the source document which has not yet been sourced?
   //
   // For now, we prefer the current source document + the source
   // index, and then use the search path after if necessary.
   const ParseNode* pNode;
   if (status.node()->findFunction(cursor.contentAsUtf8(),
                                   cursor.currentPosition(),
                                   &pNode))
   {
      DEBUG("--- Found function in parse tree: '" << pNode->name() << "'");
      if (cursor.moveToPosition(pNode->position()))
         if (maybePerformsNSE(cursor))
            return true;
   }
   
   // Search the R source index if this is a simple call, and
   // we're within a package project.
   const std::string& symbol = cursor.contentAsUtf8();
   if (cursor.isSimpleCall() &&
       projects::projectContext().isPackageProject())
   {
      const PackageInformation& info = RSourceIndex::getPackageInformation(
               projects::projectContext().packageInfo().name());
      
      if (info.functionInfo.count(symbol))
      {
         DEBUG("--- Found function in source index");
         const FunctionInformation& fnInfo =
               const_cast<FunctionInformationMap&>(info.functionInfo)[symbol];
         
         if (fnInfo.performsNse())
            return true;
      }
   }
      
   // Search the whole index.
   bool failed = false;
   
   std::vector<std::string> inferredPkgs;
   if (status.filePath().exists())
   {
      boost::shared_ptr<RSourceIndex> pIndex =
            code_search::rSourceIndex().get(status.filePath());
      
      if (pIndex)
         inferredPkgs = pIndex->getInferredPackages();
   }
   
   const FunctionInformation& fnInfo =
         RSourceIndex::getFunctionInformationAnywhere(
            symbol,
            inferredPkgs,
            &failed);

   if (!failed)
   {
      DEBUG("--- Found function in pkgInfo index: " << *fnInfo.binding());
      return bool(fnInfo.performsNse());
   }
   
   // Handle some special cases first.
   if (isSymbolNamed(cursor, L"::") || isSymbolNamed(cursor, L":::"))
      return true;
   
   // Drop down into R.
   bool cacheable = true;
   r::sexp::Protect protect;
   SEXP symbolSEXP = resolveFunctionAssociatedWithCall(cursor, status, &protect, &cacheable);
   if (symbolSEXP == R_UnboundValue)
      return false;
   
   NSEDatabase& nseDb = nseDatabase();
   if (cacheable)
   {
      if (nseDb.isKnownToPerformNSE(symbolSEXP))
      {
         DEBUG("-- Known to perform NSE");
         return true;
      }
      else if (nseDb.isKnownNotToPerformNSE(symbolSEXP))
      {
         DEBUG("-- Known not to perform NSE");
         return false;
      }
   }
   
   bool result = r::sexp::maybePerformsNSE(symbolSEXP);
   DEBUG("----- Does '" << cursor << "' perform NSE? " << result);
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


// Extract a single formal from an in-source function _definition_. For example,
//
//    foo <- function(alpha = 1, beta, gamma) {}
//                    ^~~~>~~~~^
// This function should fill a formals map, with mappings such as:
//
//    alpha -> "1"
//    beta  -> <empty>
//    gamma -> <empty>
//
// This function will 'consume' all data associated with the formal, and place
// the cursor on the closing comma or right paren.
void extractFormal(
      RTokenCursor& cursor,
      FunctionInformation* pInfo)
      
{
   std::string formalName;
   
   bool hasDefaultValue = false;
   std::wstring::const_iterator defaultValueStart;
   
   if (cursor.isType(RToken::ID))
      formalName = getSymbolName(cursor);
   
   if (!cursor.moveToNextSignificantToken())
      return;
   
   if (cursor.contentEquals(L"="))
   {
      if (!cursor.moveToNextSignificantToken())
         return;
      
      if (cursor.isType(RToken::COMMA))
         return;
      
      hasDefaultValue = true;
      defaultValueStart = cursor.begin();
   }
      
   do
   {
      if (cursor.fwdToMatchingToken())
         continue;

      if (cursor.isType(RToken::COMMA) || isRightBracket(cursor))
         break;

   } while (cursor.moveToNextSignificantToken());
   
   FormalInformation info(formalName);
   
   if (hasDefaultValue)
      info.setDefaultValue(string_utils::wideToUtf8(
                              std::wstring(defaultValueStart, cursor.begin())));
   
   pInfo->addFormal(info);
   
   if (cursor.isType(RToken::COMMA))
      cursor.moveToNextSignificantToken();
}


// Extract all formals from an in-source function _definition_. For example,
//
//    foo <- function(alpha = 1, beta, gamma) {}
//
// This function should fill a formals map, with mappings such as:
//
//    alpha -> "1"
//    beta  -> <empty>
//    gamma -> <empty>
//
bool extractInfoFromFunctionDefinition(
      RTokenCursor cursor,
      FunctionInformation* pInfo)
{
   do
   {
      if (isFunctionKeyword(cursor))
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
   
   while (cursor.isType(RToken::ID))
      extractFormal(cursor, pInfo);
   
   // TODO: Extract body information as well, so we can figure out
   // whether a particular formal is used or not.
   
   return true;
}


// Extract the named, and unnamed, arguments supplied in a function call.
// For example, in the following call:
//
//    foo(1 + 2, a = 3)
//
// we want to return:
//
//    Named Arguments: {a: "3"}
//  Unnamed Arguments: ["1 + 2"]
//
void getNamedUnnamedArguments(RTokenCursor cursor,
                              std::map<std::string, std::string>* pNamedArguments,
                              std::vector<std::string>* pUnnamedArguments)
{
   if (cursor.nextSignificantToken().isType(RToken::LPAREN))
      if (!cursor.moveToNextSignificantToken())
         return;
   
   if (!cursor.isType(RToken::LPAREN))
      return;
   
   if (!cursor.moveToNextSignificantToken())
      return;
   
   if (cursor.isType(RToken::RPAREN))
      return;
   
   // Special case: if the function call is as so:
   //
   //    foo(,)
   //
   // then we just have two empty (missing) arguments.
   if (cursor.isType(RToken::COMMA) &&
       cursor.nextSignificantToken().isType(RToken::RPAREN))
   {
      pUnnamedArguments->push_back(std::string());
      pUnnamedArguments->push_back(std::string());
      return;
   }
   
   do
   {
      std::string argName;
      bool isNamedArgument = false;
      std::wstring::const_iterator begin = cursor.begin();

      if (cursor.isLookingAtNamedArgumentInFunctionCall())
      {
         isNamedArgument = true;
         argName = getSymbolName(cursor);
         
         if (!cursor.moveToNextSignificantToken())
            return;
         
         if (!cursor.moveToNextSignificantToken())
            return;
         
         begin = cursor.begin();
      }

      do
      {
         if (cursor.fwdToMatchingToken())
            continue;

         if (cursor.isType(RToken::COMMA) || isRightBracket(cursor))
            break;

      } while (cursor.moveToNextSignificantToken());

      if (isNamedArgument)
      {
         (*pNamedArguments)[argName] =
               string_utils::wideToUtf8(std::wstring(begin, cursor.begin()));
      }
      else
      {
         pUnnamedArguments->push_back(
                  string_utils::wideToUtf8(std::wstring(begin, cursor.begin())));
      }

   } while (cursor.isType(RToken::COMMA) && cursor.moveToNextSignificantToken());
   
   
}


// Extract formals from the underlying object mapped by the symbol, or expression,
// at the cursor. This involves (potentially) evaluating the expression forming
// the function object, e.g.
//
//     foo$bar(baz, bat)
//     ^^^^^^^
//
// This code will attempt to resolve `foo$bar` (which likely requires evaluation),
// and then, if it's a function will extract the formals associated with that function.
FunctionInformation getInfoAssociatedWithFunctionAtCursor(
      RTokenCursor cursor,
      ParseStatus& status)
{
   // If this is a direct call to a symbol, then first attempt to
   // find this function in the current document.
   if (cursor.isSimpleCall())
   {
      if (cursor.isType(RToken::LPAREN))
         if (!cursor.moveToPreviousSignificantToken())
            return FunctionInformation();
      
      DEBUG("***** Attempting to resolve source function: '" << cursor.contentAsUtf8() << "'");
      const ParseNode* pNode;
      if (status.node()->findFunction(
             cursor.contentAsUtf8(),
             cursor.currentPosition(),
             &pNode))
      {
         DEBUG("***** Found function: '" << pNode->name() << "' at: " << pNode->position());

         // TODO: When we infer a function from the source code, and that
         // function is a top-level source function, should we give it an
         // 'origin' name equal to the current package's name?
         const std::string& name = pNode->name();
         std::string origin = "<root>";
         if (pNode->getParent())
            origin = pNode->getParent()->name();

         FunctionInformation info(origin, name);
         RTokenCursor clone = cursor.clone();
         if (clone.moveToPosition(pNode->position()))
         {
            DEBUG("***** Moved to position");
            if (extractInfoFromFunctionDefinition(clone, &info))
            {
               DEBUG("Extracted arguments");
               return info;
            }
         }
      }
      
      // Try seeing if a symbol of this name has already been defined in scope,
      // to protect against instances of the form e.g.
      //
      //    pf <- identity
      //    pf
      //
      // In these cases, we will (for now) simply fail to resolve the function,
      // to ensure that we don't supply incorrect lint for that function call.
      //
      // Note that this behaviour is quite conservative; however, the alternative
      // would involve implementing a pseudo-evaluator to actually figure out what
      // 'pf' is now actually bound to; this could be doable in some simple cases
      // but the pattern is uncommon enough that it's better that we just don't
      // supply incorrect diagnostics, rather than attempt to supply correct diagnostics.
      if (status.node()->findVariable(cursor.contentAsUtf8(), cursor.currentPosition()))
      {
         DEBUG("***** Found variable masking definition; giving up");
         return FunctionInformation();
      }
      
      // If we're within a package project, then attempt searching the
      // source index for the formals associated with this function.
      const std::string& fnName = cursor.contentAsUtf8();
      if (projects::projectContext().isPackageProject())
      {
         std::string pkgName = projects::projectContext().packageInfo().name();
         DEBUG("***** Checking if package '" << pkgName << "' knows about function '" << fnName << "'");
         if (RSourceIndex::hasFunctionInformation(fnName, pkgName))
         {
            DEBUG("***** Found function definition in source index");
            return RSourceIndex::getFunctionInformation(fnName, pkgName);
         }
         else
         {
            DEBUG("***** Couldn't find information on function '" << fnName << "' from package '" << pkgName << "'");
         }
      }
      
      // Try looking up the symbol by name.
      bool lookupFailed = false;
      std::vector<std::string> inferredPkgs;
      if (status.filePath().exists())
      {
         boost::shared_ptr<RSourceIndex> pIndex =
               code_search::rSourceIndex().get(status.filePath());

         if (pIndex)
            inferredPkgs = pIndex->getInferredPackages();
      }
      
      FunctionInformation info =
            RSourceIndex::getFunctionInformationAnywhere(fnName, inferredPkgs, &lookupFailed);
      
      if (!lookupFailed)
      {
         DEBUG("**** Found function definition via fallback");
         return info;
      }
      
   }
   
   // If the above failed, we'll fall back to evaluating and looking up
   // the symbol on the search path.
   r::sexp::Protect protect;
   SEXP functionSEXP = resolveFunctionAssociatedWithCall(cursor, status, &protect);
   if (functionSEXP == R_UnboundValue || !Rf_isFunction(functionSEXP))
   {
      DEBUG("***** Function definition is not available on search path; giving up");
      return FunctionInformation();
   }
   
   // Get the formals associated with this function.
   FunctionInformation info(
            string_utils::wideToUtf8(cursor.getEvaluationAssociatedWithCall()),
            r::sexp::environmentName(functionSEXP));
   
   Error error = r::sexp::extractFunctionInfo(
            functionSEXP,
            &info,
            true,
            true);
   
   if (error)
   {
      DEBUG("***** Couldn't resolve function information from search path definition");
   }
   else
   {
      if (info.binding())
      {
         DEBUG("***** Using definition from search path (" << info.binding()->name << " from " << info.binding()->origin << ")");
      }
      else
      {
         DEBUG("***** Using definition from search path");
      }
   }
   
   return info;
   
}


// This class represents a matched call, similar to the result from R's
// 'match.call()'. We maintain:
//
//    1. The actual formals (+ default values, as string) for a particular function,
//    2. The function call made by the user,
//    3. The actual matched call that would be executed.
//
// The goal is to create an object that is easily lintable by us downstream.
class MatchedCall
{
public:
   
   static MatchedCall match(RTokenCursor cursor,
                            ParseStatus& status)
   {
      MatchedCall call;
      
      // Get the information associated with the underlying function
      // (formals, does it perform NSE, etc.)
      FunctionInformation info =
            getInfoAssociatedWithFunctionAtCursor(cursor, status);
      
      // Get the named, unnamed arguments supplied in the function call.
      std::map<std::string, std::string> namedArguments;
      std::vector<std::string> unnamedArguments;
      getNamedUnnamedArguments(cursor, &namedArguments, &unnamedArguments);
      
      // Figure out if this function call is being made as part of a magrittr
      // chain. If so, then we implicitly set the first argument as that object.
      RToken prevToken = cursor.previousSignificantToken();
      if (token_utils::isNamespaceExtractionOperator(prevToken))
      {
         RTokenCursor clone = cursor.clone();
         if (clone.moveToPreviousSignificantToken() &&
             clone.moveToPreviousSignificantToken() &&
             clone.moveToPreviousSignificantToken())
         {
            prevToken = clone.currentToken();
         }
      }
      
      if (isPipeOperator(prevToken))
      {
         // For a magrittr style pipe, if magrittr sees a '.' at the top level (ie: used standalone as
         // an argument) it treats that as a request to move the 'lhs' to
         // that position. (This is not true when '.' is used as part of
         // a more complicated expression)

         // For a native R style pipe |>, this is only true if we see a '_' at the top level

         bool usesTopLevelPlaceholder;
         if (prevToken.contentEquals(L"|>"))
            usesTopLevelPlaceholder = core::algorithm::contains(unnamedArguments, "_");
         else
            usesTopLevelPlaceholder = core::algorithm::contains(unnamedArguments, ".");
         if (!usesTopLevelPlaceholder)
         {
            for (auto&& item : namedArguments)
            {
               if (item.second == ".")
               {
                  usesTopLevelPlaceholder = true;
                  break;
               }
            }
         }

         std::string chainHead = cursor.getHeadOfPipeChain();
         if (!chainHead.empty() && !usesTopLevelPlaceholder)
            unnamedArguments.insert(unnamedArguments.begin(), chainHead);
      }

      DEBUG_BLOCK("Named, Unnamed Arguments")
      {
         LOG_OBJECT(namedArguments);
         LOG_OBJECT(unnamedArguments);
      }
      
      // Generate a matched call -- figure out what underlying call will
      // actually be made. We'll get the set of formals, and match them in
      // the same order that R would.
      //
      // Because order matters, we perform the matching by maintaining a set
      // of indices which we make multiple passes through on each match course,
      // and trim from those indices as we form matches.
      std::vector<std::size_t> formalIndices =
            core::algorithm::seq(info.formals().size());
      
      std::vector<std::string> matchedArgNames;
      std::vector<std::string> userSuppliedArgNames = core::algorithm::map_keys(namedArguments);
      std::map<std::string, boost::optional<std::string> > matchedCall;
      const std::vector<std::string>& formalNames = info.getFormalNames();
      DEBUG_BLOCK("Formal names")
      {
         LOG_OBJECT(formalNames);
      }
      
      /*
       * 1. Identify perfect matches in the set of formals to search.
       */
      std::vector<std::size_t> matchedIndices;
      for (const std::string& argName : userSuppliedArgNames)
      {
         for (std::size_t index : formalIndices)
         {
            const std::string& formalName = formalNames[index];
            if (argName == formalName)
            {
               DEBUG("-- Adding perfect match '" << formalName << "'");
               matchedArgNames.push_back(argName);
               matchedCall[formalName] = namedArguments[formalName];
               matchedIndices.push_back(index);
               break;
            }
         }
      }
      
      // Trim
      formalIndices = core::algorithm::set_difference(formalIndices, matchedIndices);
      userSuppliedArgNames = core::algorithm::set_difference(userSuppliedArgNames, matchedArgNames);
      
      matchedIndices.clear();
      matchedArgNames.clear();
      
      /*
       * 2. Identify prefix matches in the set of remaining formals.
       */
      std::vector<std::pair<std::string, std::string> > prefixMatchedPairs;
      for (const std::string& userSuppliedArgName : userSuppliedArgNames)
      {
         for (std::size_t index : formalIndices)
         {
            const std::string& formalName = formalNames[index];
            if (boost::algorithm::starts_with(formalName, userSuppliedArgName))
            {
               DEBUG("-- Adding prefix match: '" << userSuppliedArgName << "' -> '" << formalName << "'");
               matchedArgNames.push_back(userSuppliedArgName);
               matchedCall[formalName] = namedArguments[userSuppliedArgName];
               matchedIndices.push_back(index);
               prefixMatchedPairs.push_back(std::make_pair(userSuppliedArgName, formalName));
               break;
            }
         }
      }
      
      // Trim
      formalIndices = core::algorithm::set_difference(formalIndices, matchedIndices);
      userSuppliedArgNames = core::algorithm::set_difference(userSuppliedArgNames, matchedArgNames);
      
      matchedIndices.clear();
      matchedArgNames.clear();
      
      /*
       * 3. Match other formals positionally.
       */
      std::size_t index = 0;
      for (const std::string& argument : unnamedArguments)
      {
         if (index == formalIndices.size())
            break;

         std::size_t formalIndex = formalIndices[index];
         std::string formalName = formalNames[formalIndex];
         DEBUG("-- Adding positional match: " << formalName);
         
         matchedCall[formalName] = argument;
         index++;
      }
      
      // Trim
      formalIndices = core::algorithm::set_difference(formalIndices, matchedIndices);
      matchedIndices.clear();
      
      /*
       * 4. Fill default argument values. Anything in our matched call that
       *    has not yet been filled, but does have an available default value
       *    from our formals, will be set.
       */
      for (std::size_t i = 0; i < formalIndices.size(); ++i)
      {
         std::size_t index = formalIndices[i];
         const std::string& formalName = formalNames[index];
         if (!matchedCall.count(formalName))
         {
            DEBUG("-- Inserting default value for '" << formalName << "'");
            matchedCall[formalName] = info.defaultValueForFormal(formalName);
            matchedIndices.push_back(index);
         }
      }
      
      // Trim
      formalIndices = core::algorithm::set_difference(formalIndices, matchedIndices);
      matchedIndices.clear();
      
      // Now, we examine the end state and see if we successfully matched
      // all available arguments.
      call.namedArguments_ = namedArguments;
      call.unnamedArguments_ = unnamedArguments;
      call.matchedCall_ = matchedCall;
      call.prefixMatchedPairs_ = prefixMatchedPairs;
      call.unmatchedArgNames_ = core::algorithm::set_difference(
               userSuppliedArgNames, matchedArgNames);
      call.info_ = info;
      
      applyFixups(cursor, &call);
      applyCustomWarnings(call, status);
      return call;
   }
   
   // Fixups for things that we cannot reasonably infer.
   static void applyFixups(RTokenCursor cursor,
                           MatchedCall* pCall)
   {
      // the 'object' argument to `UseMethod()` is optional
      if (cursor.contentEquals(L"UseMethod"))
         pCall->functionInfo().infoForFormal("object").setMissingnessHandled(true);
      
      // the 'env' argument to `substitute()` is optional
      if (cursor.contentEquals(L"substitute"))
         pCall->functionInfo().infoForFormal("env").setMissingnessHandled(true);
      
      // the 'x' argument to `invisible()` is implicitly NULL
      if (cursor.contentEquals(L"invisible"))
         pCall->functionInfo().infoForFormal("x").setMissingnessHandled(true);

      // `old.packages()` delegates the 'method' formal even when missing
      // same with `available.packages()`
      if (cursor.contentEquals(L"old.packages") ||
          cursor.contentEquals(L"available.packages"))
      {
         pCall->functionInfo().infoForFormal("method").setMissingnessHandled(true);
      }
      
      // `file_test` allows 'y' to be missing, and is only used when
      // 'op' is a 'binary-accepting' operator
      if (cursor.contentEquals(L"file_test"))
         pCall->functionInfo().infoForFormal("y").setMissingnessHandled(true);
      
      // 'globalVariables' doesn't need 'package' argument
      if (cursor.contentEquals(L"globalVariables"))
      {
         pCall->functionInfo().infoForFormal("package").setMissingnessHandled(true);
      }
      
      if (cursor.contentEquals(L"vignetteEngine"))
      {
         pCall->functionInfo().infoForFormal("name").setMissingnessHandled(true);
         pCall->functionInfo().infoForFormal("weave").setMissingnessHandled(true);
         pCall->functionInfo().infoForFormal("tangle").setMissingnessHandled(true);
      }
      
      if (cursor.contentEquals(L"as.lazy_dots"))
      {
          pCall->functionInfo().infoForFormal("env").setMissingnessHandled(true);
      }
      
      if (cursor.contentEquals(L"trace"))
      {
         std::vector<FormalInformation>& formals = pCall->functionInfo().formals();
         for (FormalInformation& formal : formals)
         {
            formal.setMissingnessHandled(true);
         }
      }
      
      if (cursor.contentEquals(L"txtProgressBar"))
      {
         pCall->functionInfo().infoForFormal("label").setMissingnessHandled(true);
         pCall->functionInfo().infoForFormal("title").setMissingnessHandled(true);
      }
      
      if (cursor.contentEquals(L"spin"))
         pCall->functionInfo().infoForFormal("hair").setMissingnessHandled(true);
      
      if (cursor.contentEquals(L"read_chunk"))
         pCall->functionInfo().infoForFormal("path").setMissingnessHandled(true);
      
      if (cursor.contentEquals(L"fig_path") ||
          cursor.contentEquals(L"fig_chunk"))
      {
         pCall->functionInfo().infoForFormal("number").setMissingnessHandled(true);
      }
      
      if (cursor.contentEquals(L"need"))
         pCall->functionInfo().infoForFormal("label").setMissingnessHandled(true);
      
      if (cursor.contentEquals(L"quo"))
         pCall->functionInfo().infoForFormal("expr").setMissingnessHandled(true);
   }
   
   static void applyCustomWarnings(const MatchedCall& call,
                                   ParseStatus& status)
   {
   }

   // Accessors
   const std::map<std::string, std::string>& namedArguments() const
   {
      return namedArguments_;
   }

   const std::vector<std::string>& unnamedArguments() const
   {
      return unnamedArguments_;
   }
   
   std::map<std::string, boost::optional<std::string> >& matchedCall()
   {
      return matchedCall_;
   }
   
   const std::vector<std::pair<std::string, std::string> >& prefixMatches() const
   {
      return prefixMatchedPairs_;
   }
   
   const std::vector<std::string>& unmatchedArgNames() const
   {
      return unmatchedArgNames_;
   }
   
   FunctionInformation& functionInfo()
   {
      return info_;
   }
   
private:
   
   // Function call: named arguments to values, and unnamed (to be matched)
   // values
   std::map<std::string, std::string> namedArguments_;
   std::vector<std::string> unnamedArguments_;
   
   // Matched call: map matched formals to values (as string). We include
   // mapping of default values for formals here.
   std::map<std::string, boost::optional<std::string> > matchedCall_;
   
   // Prefix matches: used for warning later
   std::vector<std::pair<std::string, std::string> > prefixMatchedPairs_;
   
   // Unmatched argument names: populated if any argument names in the
   // function call are not matched to a formal name.
   std::vector<std::string> unmatchedArgNames_;
   
   // Information about the function itself
   FunctionInformation info_;
};

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
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, moveToNextToken)

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __STATUS__)                 \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, moveToNextSignificantToken)

#define FWD_OVER_WHITESPACE(__CURSOR__, __STATUS__)                            \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, fwdOverWhitespace)

#define FWD_OVER_WHITESPACE_AND_COMMENTS(__CURSOR__, __STATUS__)               \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, fwdOverWhitespaceAndComments)

#define FWD_OVER_BLANK(__CURSOR__, __STATUS__)                                 \
   RSTUDIO_PARSE_ACTION(__CURSOR__, __STATUS__, fwdOverBlank)

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

#define ENSURE_TYPE(__CURSOR__, __STATUS__, __TYPE__, __RETURN__)              \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.isType(__TYPE__))                                        \
      {                                                                        \
         DEBUG("(" << __LINE__ << "): Expected "                               \
                   << string_utils::wideToUtf8(typeToWideString(__TYPE__)));   \
         __STATUS__.lint().unexpectedToken(                                    \
             __CURSOR__, L"'" + typeToWideString(__TYPE__) + L"'");            \
         if (__RETURN__) return;                                               \
      }                                                                        \
   } while (0)

#define ENSURE_TYPE_NOT(__CURSOR__, __STATUS__, __TYPE__, __RETURN__)          \
   do                                                                          \
   {                                                                           \
      if (__CURSOR__.isType(__TYPE__))                                         \
      {                                                                        \
         DEBUG("(" << __LINE__ << "): Unexpected "                             \
                   << string_utils::wideToUtf8(typeToWideString(__TYPE__)));   \
         __STATUS__.lint().unexpectedToken(__CURSOR__);                        \
         if (__RETURN__) return;                                               \
      }                                                                        \
   } while (0)


#define UNEXPECTED_TOKEN(__CURSOR__, __STATUS__)                               \
   do                                                                          \
   {                                                                           \
      DEBUG("(" << __LINE__ << "): Unexpected token (" << __CURSOR__ << ")");  \
      __STATUS__.lint().unexpectedToken(__CURSOR__);                           \
   } while (0)

#define BEGIN_EXPRESSION(__CURSOR__, __STATUS__, __STATE__, __REASON__)        \
   do                                                                          \
   {                                                                           \
      if (__CURSOR__.isType(RToken::LBRACE))                                   \
      {                                                                        \
         __STATUS__.pushState(__STATE__ ## Expression);                        \
         __STATUS__.pushBracket(__CURSOR__);                                   \
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __STATUS__);               \
      }                                                                        \
      else if (canStartExpression(__CURSOR__))                                 \
      {                                                                        \
         __STATUS__.pushState(__STATE__ ## Statement);                         \
      }                                                                        \
      else                                                                     \
      {                                                                        \
         const RToken& prevToken = __CURSOR__.previousSignificantToken();      \
         __STATUS__.lint().addLintItem(prevToken, LintTypeError, __REASON__);  \
      }                                                                        \
   } while (0)

#define BEGIN_FUNCTION_EXPRESSION(__CURSOR__, __STATUS__)                      \
   do                                                                          \
   {                                                                           \
      if (__CURSOR__.isType(RToken::LBRACE))                                   \
      {                                                                        \
         __STATUS__.pushState(ParseStatus::ParseStateFunctionExpression);      \
         __STATUS__.pushBracket(__CURSOR__);                                   \
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __STATUS__);               \
      }                                                                        \
      else if (canStartExpression(__CURSOR__))                                 \
      {                                                                        \
         __STATUS__.pushState(ParseStatus::ParseStateFunctionStatement);       \
      }                                                                        \
      else                                                                     \
      {                                                                        \
         const RToken& prevToken = __CURSOR__.previousSignificantToken();      \
         const char* reason = "missing function definition";                   \
         __STATUS__.lint().addLintItem(prevToken, LintTypeError, reason);      \
         __STATUS__.setParentAsCurrent();                                      \
      }                                                                        \
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
      if (isFunctionKeyword(clone))
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
         
         if (clone.isAtEndOfStatement(status.isInParentheticalScope()))
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
      
      // NOTE: We'll be conservative on lookahead and assume non-parenthetical
      // scope (which is more restrictive)
      if (clone.isAtEndOfStatement(false))
         return;
      
   } while (clone.moveToNextSignificantToken());
}



void handleIdentifier(RTokenCursor& cursor,
                      ParseStatus& status)
{
   // Check to see if we are defining a symbol at this location.
   // Note that both string and id are valid for assignments.
   bool skipReferenceChecks =
         status.isInArgumentList() && !status.parseOptions().lintRFunctions();
   bool inNseFunction = status.isWithinNseCall();
   
   // Don't cache identifiers if:
   //
   // 1. The previous token is an extraction operator, e.g. `$`, `@`, `::`, `:::`,
   // 2. The following token is a namespace operator `::`, `:::`
   //
   // TODO: Handle namespaced symbols (e.g. for package lookup) and
   // provide lint appropriately.
   if (isExtractionOperator(cursor.previousSignificantToken()) ||
       isNamespaceExtractionOperator(cursor.nextSignificantToken()))
   {
      DEBUG("--- Symbol preceded by extraction op; not adding");
      return;
   }
   
   // If we're in a call to e.g. 'data(foo)', assume that this
   // call will succeed, and add a reference to a symbol 'foo'.
   if (cursor.previousSignificantToken(1).isType(RToken::LPAREN))
   {
      const RToken& callToken = cursor.previousSignificantToken(2);
      if (callToken.isType(RToken::ID) && callToken.contentEquals(L"data"))
         status.node()->addDefinedSymbol(cursor);
   }
   
   // Don't add references to '.' -- in most situations where it's used,
   // it's for NSE (e.g. magrittr pipes)
   if (status.isInArgumentList() && cursor.contentEquals(L"."))
      return;

   // Ignore pipe-bind placeholder.
   if (cursor.contentEquals(L"_"))
      return;
   
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
      if (status.parseOptions().warnIfNoSuchVariableInScope() &&
          !skipReferenceChecks &&
          isLeftAssign(cursor.nextSignificantToken()) &&
          !status.node()->symbolHasDefinitionInTree(cursor.contentAsUtf8(), cursor.currentPosition()))
      {
         RTokenCursor clone = cursor.clone();
         if (clone.moveToNextSignificantToken() &&
             clone.moveToNextSignificantToken() &&
             !clone.isAtEndOfStatement(status.isInParentheticalScope()))
         {
            lookAheadAndWarnOnUsagesOfSymbol(cursor, clone, status);
         }
      }
      
      // Check that parent assignments reference a variable within scope
      if (isParentLeftAssign(cursor.nextSignificantToken()))
      {
         std::string symbolName = token_utils::getSymbolName(cursor);
         if (!status.node()->symbolHasDefinitionInTree(symbolName, cursor.currentPosition()))
            status.lint().noExistingDefinitionForParentAssignment(cursor);
      }
      
      // Add a definition for this symbol
      if (isParentLeftAssign(cursor.nextSignificantToken()) ||
          isParentRightAssign(cursor.previousSignificantToken()))
      {
         
         // A parent assignment eithers modifies a binding in a parent scope,
         // or adds a binding at the top level. Check and see if this symbol
         // has already been defined; if not, add a top-level definition.
         auto symbolName = token_utils::getSymbolName(cursor);
         for (auto node = status.node()->getParent();
              node != nullptr;
              node = node->getParent())
         {
            if (node->isRootNode())
            {
               node->addDefinedSymbol(cursor);
               break;
            }
            
            if (node->getReferencedSymbols().count(symbolName))
            {
               break;
            }
         }
      }
      else if (isLeftAssign(cursor.nextSignificantToken()) ||
               isRightAssign(cursor.previousSignificantToken()))
      {
         DEBUG("--- Adding definition for symbol");
         status.node()->addDefinedSymbol(cursor);
      }
      
   }
   
   // If this is truly an identifier, add a reference.
   if (cursor.isType(RToken::ID))
   {
      if (inNseFunction)
         status.node()->addNseReferencedSymbol(cursor);
      else
         status.node()->addReferencedSymbol(cursor);
   }
}

void handleString(RTokenCursor& cursor,
                  ParseStatus& status)
{
}

} // anonymous namespace

ParseResults parse(const FilePath& filePath,
                   const std::wstring& rCode,
                   const ParseOptions& parseOptions)
{
   if (rCode.empty() || rCode.find_first_not_of(L" \r\n\t\v") == std::string::npos)
      return ParseResults();
   
   RTokens rTokens(rCode, RTokens::StripComments);
   if (rTokens.empty())
      return ParseResults();
   
   RTokenCursor cursor(rTokens);
   ParseStatus status(filePath, parseOptions);
   
   doParse(cursor, status);
   
   if (status.node()->getParent() != nullptr)
   {
      DEBUG("** Parent is not null (not at top level): failed to close all scopes?");
      status.lint().unexpectedEndOfDocument(cursor.currentToken());
   }
   
   status.addLintIfBracketStackNotEmpty();
   
   return ParseResults(status.root(), status.lint(), parseOptions.globals());
}

ParseResults parse(const std::string& rCode,
                   const ParseOptions& parseOptions)
{
   return parse(
            FilePath(),
            string_utils::utf8ToWide(rCode),
            parseOptions);
}

ParseResults parse(const std::wstring& rCode,
                   const ParseOptions& parseOptions)
{
   return parse(
            FilePath(),
            rCode,
            parseOptions);
}

ParseResults parse(const FilePath &filePath, const ParseOptions &parseOptions)
{
   std::string contents;
   Error error = readStringFromFile(filePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return ParseResults();
   }
   
   return parse(
            filePath,
            string_utils::utf8ToWide(contents),
            parseOptions);
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

// Used to check for variable assignments that occur within
// argument lists, e.g.
//
//    list(a = 1, b <- 2)
//                ^^^^^^
//
// Most likely, the user intended to write 'b = 2' but accidentally
// used '<-' instead.
void checkVariableAssignmentInArgumentList(RTokenCursor cursor,
                                           ParseStatus& status)
{
   if (!cursor.isType(RToken::ID))
      return;
   
   std::string identifier = cursor.contentAsUtf8();
   if (identifier == ".")
      return;
   
   if (!cursor.moveToNextSignificantToken())
      return;
   
   if (!cursor.contentEquals(L"<-"))
      return;
   
   // too noisy
   // https://github.com/rstudio/rstudio/issues/14870
   // status.lint().unexpectedAssignmentInArgumentList(cursor);
}

// NOTE: Intentionally passed by value to accept a copy
void checkDottyAssignment(RTokenCursor cursor,
                          ParseStatus& status)
{
   // First, check that we're sitting on top of an assignment
   // into a dotty expression. This should be something of the form
   //
   //    .[x, y, z] <-
   //
   // with the cursor currently sitting on the left assignment.
   bool isDottyAssignment =
         isLeftAssign(cursor) &&
         cursor.moveToPreviousSignificantToken() &&
         cursor.isType(RToken::RBRACKET) &&
         cursor.bwdToMatchingToken() &&
         cursor.isType(RToken::LBRACKET) &&
         cursor.moveToPreviousSignificantToken() &&
         cursor.isType(RToken::ID) &&
         cursor.contentEquals(L".");
   
   if (!isDottyAssignment)
      return;
   
   if (cursor.moveToNextSignificantToken() &&
       cursor.moveToNextSignificantToken() &&
       cursor.isType(RToken::ID))
   {
      status.node()->addDefinedSymbol(cursor);
   }
   
   do
   {
      if (cursor.fwdToMatchingToken())
      {
         continue;
      }
      else if (cursor.isType(RToken::COMMA) &&
               cursor.moveToNextSignificantToken() &&
               cursor.isType(RToken::ID))
      {
         status.node()->addDefinedSymbol(cursor);
      }
      else if (isRightBracket(cursor))
      {
         break;
      }
   }
   while (cursor.moveToNextSignificantToken());
}

void checkUnexpectedEqualsAssignment(RTokenCursor& cursor,
                                     ParseStatus& status)
{
   std::string context;

   switch (status.currentState())
   {

   case ParseStatus::ParseStateIfCondition:
      context = "if";
      break;

   case ParseStatus::ParseStateForCondition:
      context = "for";
      break;

   case ParseStatus::ParseStateWhileCondition:
      context = "while";
      break;

   default:
      return;
   }

   if (cursor.contentEquals(L"="))
      status.lint().unexpectedAssignmentInConditional(cursor, context);
}

void checkBinaryOperatorWhitespace(RTokenCursor& cursor,
                                   ParseStatus& status)
{
   
   // Allow both whitespace styles for certain binary operators, but
   // ensure that the whitespace around is consistent.
   if (cursor.contentEquals(L'/') ||
       cursor.contentEquals(L'*') ||
       cursor.contentEquals(L'^') ||
       cursor.contentEquals(L'?') ||
       cursor.contentEquals(L"**"))
   {
      bool lhsWhitespace = isWhitespace(cursor.previousToken());
      bool rhsWhitespace = isWhitespace(cursor.nextToken());
      
      if (lhsWhitespace != rhsWhitespace)
         status.lint().inconsistentWhitespaceAroundOperator(cursor);
      return;
   }
   
   // There should not be whitespace around extraction operators.
   //
   //    x $ foo
   //
   // is bad style.
   bool isExtraction = isExtractionOperator(cursor);
   bool isColon = cursor.contentEquals(L':');
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

void addExtraScopedSymbolsForCall(RTokenCursor startCursor,
                                  ParseStatus& status)
{
   if (startCursor.isType(RToken::LPAREN))
      if (!startCursor.moveToPreviousSignificantToken())
         return;
   
   if (startCursor.contentEquals(L"setRefClass"))
   {
      RTokenCursor endCursor = startCursor.clone();
      if (!endCursor.moveToNextSignificantToken())
         return;
      
      if (!endCursor.fwdToMatchingToken())
         return;
      
      std::set<std::string> symbols;
      r::exec::RFunction getSetRefClassCall(".rs.getSetRefClassSymbols");
      getSetRefClassCall.addParam(
               string_utils::wideToUtf8(
                  std::wstring(startCursor.begin(), endCursor.end())));
      
      Error error = getSetRefClassCall.call(&symbols);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      status.makeSymbolsAvailableInRange(
               symbols,
               startCursor.currentPosition(),
               endCursor.currentPosition());
   }
   
   if (startCursor.contentEquals(L"R6Class"))
   {
      RTokenCursor endCursor = startCursor.clone();
      if (!endCursor.moveToNextSignificantToken())
         return;
      
      if (!endCursor.fwdToMatchingToken())
         return;
      
      std::set<std::string> symbols;
      r::exec::RFunction getR6ClassSymbols(".rs.getR6ClassSymbols");
      getR6ClassSymbols.addParam(
               string_utils::wideToUtf8(std::wstring(startCursor.begin(), endCursor.end())));
      
      Error error = getR6ClassSymbols.call(&symbols);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
      
      status.makeSymbolsAvailableInRange(
               symbols,
               startCursor.currentPosition(),
               endCursor.currentPosition());
   }
   
}

void validateGlueCallImpl(RTokenCursor cursor,
                          ParseStatus& status,
                          const std::string& open,
                          const std::string& close,
                          const std::vector<std::string>& dataMask)
{
   // A helper macro for debugging.
#ifdef RSTUDIO_ENABLE_GLUE_DEBUG
# define GLUE(...)                                                 \
   do                                                              \
   {                                                               \
      const char* fmt = "glue({}; {}; '{}'):";                     \
      std::string prefix = fmt::format(fmt, state, it, value[it]); \
      std::string suffix = fmt::format(__VA_ARGS__);               \
      fmt::print("{} {}\n", prefix, suffix);                       \
   }                                                               \
   while (0)
#else
# define GLUE(...) do {} while (0)
#endif
   
   auto cursorPosition = cursor.currentPosition();
   
   // Get the value of the current string.
   // Note that we're working with the _raw_ token value, which
   // implies that embedded escapes may be included.
   std::string value = cursor.contentAsUtf8();
   
   // Helper function for processing code once we've successfully found it.
   auto processCode = [&](std::size_t start, std::size_t end)
   {
      // Compute the token position.
      auto position = cursorPosition;
      position.column += start;

      // Extract the code.
      std::string rCode = value.substr(start, end - start);

      // Un-escape escaped delimiters within the code.
      // This is necessary to handle glue expressions of the form:
      //
      //    glue("Value: { \"Embedded string\" }")
      //
      // where the code we want to actually parse is just "Embedded string".
      //
      // We use this somewhat hacky approach to ensure that we don't need
      // to try and recompute token offsets.
      text::TextCursor cursor(rCode);
      do
      {
         // Look for an escape.
         if (!cursor.consumeUntil('\\'))
            break;

         // Check for a delimiter following.
         char ch = cursor.peek(1);
         if (ch != '"' && ch != '\'' && ch != '`')
            continue;

         // Make it parsable.
         rCode[cursor.offset()] = ' ';
      }
      while (cursor.advance());

      RTokens rTokens(string_utils::utf8ToWide(rCode), position, RTokens::StripComments);
      if (rTokens.empty())
         return false;

      RTokenCursor subCursor(rTokens);

      // glue expressions can provide data to be formatted; for example,
      //
      //     glue("{x}", x = 42)
      //
      // so we need to make those symbols implicitly available while parsing
      status.addChildAndSetAsCurrentNode(ParseNode::createNode("(glue)"), position);
      for (const std::string& mask : dataMask)
         status.node()->addDefinedSymbol(mask, cursorPosition);
      doParse(subCursor, status);
      status.setParentAsCurrent();

      return true;
   };
   
   enum State { Top, Code, String, EmbeddedString, };
   
   // The string delimiter used for an inner string.
   char stringDelimiter = 0;
   
   // The 'start' + 'end' markers for an inner piece of code.
   std::size_t start = 0;
   std::size_t end = 0;
   
   // The string iterator. We start at index 1, so that we can
   // skip the initial quoting character.
   std::size_t it = 1;
   
   // The length of the string. We subtract 1 so we can skip
   // the closing quoting characters.
   std::size_t n = value.size() - 1;
   
   // The depth (number of opening delimiters) within an R string.
   int depth = 0;
   
   // The previous state; used for processing inner strings.
   State previousState = Top;
   
   // The current state.
   State state = Top;
   
   GLUE("Parsing string: {}", value);
   
   // Move to RESTART label, to avoid initial iterator advancement.
   goto RESTART;
   
   while (true)
   {
      // Advance the iterator.
      ++it;
      
      // The 'RESTART' loop is used for cases where we don't need
      // automatic advance of the iterator position.
      RESTART:
      
      // Check for end of string.
      if (it >= n)
         break;
      
      GLUE("Current character: '{}'", value[it]);
      
      switch (state)
      {
      
      case Top:
      {
         // Check for open delimiter.
         if (string_utils::hasSubstringAtOffset(value, open, it))
         {
            // Consume the delimiter.
            it += open.size();
            
            // Save the start state.
            start = it;
            
            // If it's escaped, restart.
            if (string_utils::hasSubstringAtOffset(value, open, it))
            {
               GLUE("Found escaped opening delimiter {}.", open);
               it += open.size();
               goto RESTART;
            }
            else
            {
               GLUE("Found opening delimiter {}.", open);
            }
            
            // Otherwise, enter the code state.
            state = Code;
            goto RESTART;
         }
         
         continue;
      }
         
      case Code:
      {
         char ch = value[it];
         
         // Check for escape.
         if (ch == '\\')
         {
            ch = value[it + 1];
            if (ch == '"' || ch == '\'' || ch == '`')
            {
               ++it;
               stringDelimiter = ch;
               previousState = Code;
               state = EmbeddedString;
               continue;
            }
         }
         
         // Consume strings.
         if (ch == '`' || ch == '"' || ch == '\'')
         {
            GLUE("Consuming string with delimiter {}.", ch);
            stringDelimiter = ch;
            previousState = Code;
            state = EmbeddedString;
            continue;
         }
         
         // Check for close delimiter.
         if (string_utils::hasSubstringAtOffset(value, close, it))
         {
            if (depth > 0)
            {
               GLUE("Found closing delimiter {}; decreasing depth.", close);
               
               // This matches an opening delimiter; consume it and keep going.
               it += close.size();
               depth -= 1;
               goto RESTART;
            }
            else
            {
               GLUE("Found matching delimiter {}; parsing code.", close);
                     
               // We found our closing delimiter; save the location.
               end = it;
               
               // Consume the delimiter.
               it += close.size();
               
               // Parse the associated code.
               processCode(start, end);
               
               // Go back to the Top state.
               state = Top;
               goto RESTART;
            }
         }
         else if (string_utils::hasSubstringAtOffset(value, open, it))
         {
            GLUE("Found opening delimiter {}; increasing depth.", open);
            depth += 1;
            it += open.size();
            goto RESTART;
         }
         
         continue;
      }
         
      case EmbeddedString:
      {
         if (value[it] == stringDelimiter)
         {
            state = previousState;
         }
         else if (value[it] == '\\')
         {
            it += 1;
            
            char ch = value[it];
            if (ch == stringDelimiter)
               state = previousState;
         }
         
         continue;
      }
         
      case String:
      {
         // Skip escapes.
         if (value[it] == '\\')
         {
            GLUE("Consuming escape in string.");
            it += 1;
            continue;
         }
         
         // Check for closing delimiter.
         if (value[it] == stringDelimiter)
         {
            GLUE("Found end of string; returning to previous state.");
            state = previousState;
            continue;
         }
         
         continue;
      }
         
      }
   }

#undef GLUE
}

// Validate a call to 'glue()' of the form:
//
//    glue("The value is {foo} + {bar}", foo = 42)
//
// One challenge for us here is that values for bindings
// can come either from the call to glue() itself, or from
// existing bindings in the current scope.
void validateGlueCall(RTokenCursor cursor,
                      ParseStatus& status,
                      const std::string& open,
                      const std::string& close,
                      const std::vector<std::string>& dataMask)
{
   
   // Move off of 'glue' and '('
   if (!cursor.moveToNextSignificantToken())
      return;
   
   if (!cursor.moveToNextSignificantToken())
      return;
   
   do
   {
      // If this is a string, validate it
      if (cursor.isType(RToken::STRING))
         validateGlueCallImpl(cursor, status, open, close, dataMask);
      
      // Skip strings following named arguments, e.g. '= "foo"'
      if (cursor.contentEquals(L"=") &&
          cursor.nextSignificantToken().isType(RToken::STRING))
      {
         if (!cursor.moveToNextSignificantToken())
            break;
         
         if (!cursor.moveToNextSignificantToken())
            break;
      }
      
      // If we've reached a closing parenthesis, break
      if (cursor.isType(RToken::RPAREN))
         break;
      
      // Otherwise, skip over other blocks
      if (cursor.fwdToMatchingToken())
         continue;
      
   }
   while (cursor.moveToNextSignificantToken());
   
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
   
   RTokenCursor startCursor = cursor.clone();
   startCursor.moveToStartOfEvaluation();
   
   MatchedCall matched = MatchedCall::match(cursor, status);
   
   // If this is a call to 'glue()', handle it specially here.
   if (isSymbolNamed(cursor, L"glue"))
   {
      DEBUG("Validating glue call");
      std::vector<std::string> keys;
      for (const auto& entry : matched.namedArguments())
      {
         DEBUG("Adding data mask for glue: " << entry.first);
         keys.push_back(entry.first);
      }

      std::string open = "{";
      if (matched.namedArguments().count(".open"))
      {
         std::string value = matched.namedArguments().at(".open");
         Error error = string_utils::jsonLiteralUnescape(value, &open);
      }
      
      std::string close = "}";
      if (matched.namedArguments().count(".close"))
      {
         std::string value = matched.namedArguments().at(".close");
         Error error = string_utils::jsonLiteralUnescape(value, &close);
      }
      
      validateGlueCall(cursor, status, open, close, keys);
   }
   
   // Bail if the function has no formals (e.g. certain primitives),
   // or if it contains '...'
   //
   // TODO: Wire up argument validation + full matching for '...'
   const std::vector<std::string>& formalNames = 
         matched.functionInfo().getFormalNames();
   
   if (formalNames.empty() || core::algorithm::contains(formalNames, "..."))
      return;
   
   // Warn on partial matches.
   const std::vector< std::pair<std::string, std::string> >& prefixMatchedPairs
         = matched.prefixMatches();
   
   if (!prefixMatchedPairs.empty())
   {
      std::string prefix = prefixMatchedPairs.size() == 1 ?
               "partially matched argument: " :
               "partially matched arguments: ";
      
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
   
   // Error on too many arguments.
   std::size_t numUserArguments =
         matched.unnamedArguments().size() +
         matched.namedArguments().size();
   
   // Consider '...' as a zero-sized argument for purposes of counting
   // the number of arguments the user is passing down.
   numUserArguments -=
         core::algorithm::contains(matched.unnamedArguments(), "...");
   
   std::size_t numFormals = matched.functionInfo().formals().size();
   if (numUserArguments > numFormals)
   {
      std::stringstream ss;
      ss << "too many arguments in call to '"
         << string_utils::wideToUtf8(cursor.getEvaluationAssociatedWithCall())
         << "'";
      
      status.lint().add(
               startCursor.row(),
               startCursor.column(),
               endCursor.row(),
               endCursor.column() + endCursor.length(),
               LintTypeError,
               ss.str());
   }
   
   DEBUG_BLOCK("Checking whether missingness is handled")
   {
      debug::print(matched.matchedCall());
   }
   
   // Error on missing arguments to call. If the user is passing down '...',
   // we'll avoid this check and assume it's being inherited from the parent
   // function.
   if (!core::algorithm::contains(matched.unnamedArguments(), "..."))
   {
      for (std::size_t i = 0, n = formalNames.size(); i < n; ++i)
      {
         const std::string& formalName = formalNames[i];
         const FormalInformation& info =
               matched.functionInfo().infoForFormal(formalName);

         std::map<std::string, boost::optional<std::string> >& matchedCall =
               matched.matchedCall();

         if (!matchedCall[formalName] &&
             !info.hasDefault() &&
             !info.isMissingnessHandled())
         {
            status.lint().add(
                     startCursor.row(),
                     startCursor.column(),
                     endCursor.row(),
                     endCursor.column() + endCursor.length(),
                     LintTypeWarning,
                     "argument '" + formalName + "' is missing, with no default");
         }
      }
   }

   // Error on unmatched arguments.
   const std::vector<std::string>& unmatched = matched.unmatchedArgNames();
   if (!unmatched.empty())
   {
      std::string prefix = unmatched.size() == 1 ?
               "unmatched arguments: " :
               "unmatched argument: ";
      
      std::string message = prefix +
            "'" + boost::algorithm::join(unmatched, "', '") + "'";

      status.lint().add(
               startCursor.row(),
               startCursor.column(),
               endCursor.row(),
               endCursor.column() + endCursor.length(),
               LintTypeError,
               message);
   }
}

// Skipping formulas is tricky! It may require an arbitrary amount of
// lookahead, as the semantics of formulas are as such:
//
//    (expression) ~ (expression)
//
// which means beasts like this are valid formulas:
//
//    foo$bar$log(1 + y) + log(2 + y) ~ ({1 ~ 2}) ^ 5
//
// and so, effectively, this needs to be a mini-parser of R
// code.
bool skipFormulas(RTokenCursor& origin, ParseStatus& status)
{
   // don't skip in 'case' function calls
   for (auto&& name : status.functionNames())
      if (name == L"case")
         return false;
   
   RTokenCursor cursor = origin.clone();
   bool foundTilde = false;

   while (!cursor.isAtEndOfDocument())
   {
      // Initial unary operators
      while (isValidAsUnaryOperator(cursor))
      {
         if (cursor.contentEquals(L"~"))
            foundTilde = true;

         if (!cursor.moveToNextSignificantToken())
            break;
      }

      // Stand-alone bracketed scopes
      if (isLeftBracket(cursor))
         if (!cursor.fwdToMatchingToken())
            return false;
      
      // If the cursor is lying upon a right parenthesis, then implicitly
      // pop that state (examine the parent state).
      ParseStatus::ParseState state = cursor.isType(RToken::RPAREN)
            ? status.peekState(1)
            : status.peekState(0);

      // Check for end of statement
      if (cursor.isAtEndOfStatement(status.isInParentheticalScope(state)))
         break;

      // Expecting a symbol or right bracket
      if (!cursor.moveToNextSignificantToken())
         break;

      // Function calls
      while (isLeftBracket(cursor))
      {
         if (!cursor.fwdToMatchingToken())
            return false;

         if (cursor.isAtEndOfStatement(status.isInParentheticalScope()))
            break;
         
         if (!cursor.moveToNextSignificantToken())
            break;
      }

      // Binary operator or bust
      if (!isBinaryOp(cursor))
         break;

      if (cursor.contentEquals(L"~"))
         foundTilde = true;

      // Step over the operator and start again
      if (!cursor.moveToNextSignificantToken())
         break;
   }

   if (foundTilde)
   {
      DEBUG("Skipped formula: " << origin.currentToken() << " --> " << cursor.currentToken());
      origin.setOffset(cursor.offset());
   }

   return foundTilde;
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
       isFunctionKeyword(cursor) &&
       cursor.moveToPreviousSignificantToken() &&
       isLeftAssign(cursor))
   {
      if (status.isInArgumentList() && cursor.contentEquals(L"="))
      {
         // skip; it looks like this '=' is binding a value to a
         // named function argument rather than defining a symbol
      }
      else if (cursor.moveToPreviousSignificantToken())
      {
         symbol = string_utils::wideToUtf8(cursor.getEvaluationAssociatedWithCall());
         position = cursor.currentPosition();
      }
   }
   
   status.enterFunctionScope(symbol, position);
}

void checkForMissingComma(const RTokenCursor& cursor,
                          ParseStatus& status)
{
   const RToken& next = cursor.nextSignificantToken();
   if (status.isInParentheticalScope() && isValidAsIdentifier(next))
      status.lint().expectedCommaFollowingToken(cursor);
}

void checkIncorrectComparison(const RTokenCursor& origin,
                              ParseStatus& status)
{
   const RToken& prev = origin.previousSignificantToken();
   if (!(prev.contentEquals(L"==") || prev.contentEquals(L"!=")))
      return;
   
   bool isNULL = origin.contentEquals(L"NULL");
   bool isNA   = isNaKeyword(origin);
   bool isNaN  = origin.contentEquals(L"NaN");
   
   bool needsSpecialHandling =
         isNULL || isNA || isNaN;
   
   if (!needsSpecialHandling)
      return;
   
   std::string content = origin.contentAsUtf8();
   std::string suggestion;
   if (isNULL)
      suggestion = "is.null";
   else if (isNA)
      suggestion = "is.na";
   else if (isNaN)
      suggestion = "is.nan";
   
   // Clone a cursor and put it at the start of the statement
   // prior to the '==' token
   RTokenCursor startCursor = origin.clone();
   if (!startCursor.moveToPreviousSignificantToken())
      return;

   if (!startCursor.moveToPreviousSignificantToken())
      return;

   if (!startCursor.moveToStartOfEvaluation())
      return;

   status.lint().incorrectEqualityComparison(
            content,
            suggestion,
            startCursor.currentPosition(),
            origin.currentPosition(true));
}

bool handleElseToken(RTokenCursor& cursor,
                     ParseStatus& status)
{
   // Checking for whether an else token is valid here is a little tricky:
   // For example, all of these are valid:
   //
   //    if (1) 1 else 2
   //    if (1) function() function() {} else 2
   //    if (1) if (2) while (3) 4 else 5
   //    if (1) function() while(for(i in 1) 1) 1 else 2
   //
   // The net result is that an 'else' token must:
   //
   //    (1) Consume all statements (up to an if statement),
   //    (2) Verify that there is an if statement to consume.
   if (!cursor.nextSignificantToken().contentEquals(L"else"))
      return false;
   
   // Move on to the 'else' token.
   if (!cursor.moveToNextSignificantToken())
      return false;
   
   // The 'else' can consume all non-if control flow statements.
   while (status.isInControlFlowStatement())
   {
      if (status.isInIfStatementOrExpression())
         break;
      
      status.popState();
   }
   
   // TODO: Can we validate that this 'else' was valid?
   
   // Now, pop the 'if' state.
   status.popState();
   
   // Interestingly, this construct is _not_ valid at the top level:
   //
   //    if (1) 2
   //    else 3
   //
   // but this is okay:
   //
   //    {
   //       if (1) 2
   //       else 3
   //    }
   //
   // So, if we're now at the top level, this is an error!
   if (status.isAtTopLevel() && cursor.isFirstSignificantTokenOnLine())
      status.lint().unexpectedToken(cursor);
   
   // Move after the 'else' token.
   if (!cursor.moveToNextSignificantToken())
      return false;
   
   return true;
}

bool makeSymbolsAvailableInCallFromObjectNames(RTokenCursor cursor,
                                               ParseStatus& status)
{
   RTokenCursor startCursor = cursor.clone();
   if (!startCursor.findTokenFwd(boost::bind(isLeftBracket, _1)))
      return false;
   
   RTokenCursor endCursor = cursor.clone();
   if (!endCursor.fwdToMatchingToken())
      return false;
   
   r::sexp::Protect protect;
   SEXP objectSEXP = resolveObjectAssociatedWithCall(startCursor, status, &protect);
   if (objectSEXP != R_UnboundValue)
   {
      r::exec::RFunction getNames(".rs.getNames");
      getNames.addParam(objectSEXP);
      
      std::vector<std::string> names;
      Error error = getNames.call(&names);
      if (error)
         LOG_ERROR(error);
      
      status.makeSymbolsAvailableInRange(
               names,
               startCursor.currentPosition(),
               endCursor.currentPosition(true));
      
      return true;
   }
   
   return false;
}

} // anonymous namespace

#define GOTO_INVALID_TOKEN(__CURSOR__)                                         \
   do                                                                          \
   {                                                                           \
      DEBUG("Invalid token: " << __CURSOR__);                                  \
      goto INVALID_TOKEN;                                                      \
   } while (0)

void doParse(RTokenCursor& cursor, ParseStatus& status)
{
   DEBUG("Beginning parse (" << cursor << ")");
   
   cursor.fwdOverWhitespaceAndComments();
   bool startedWithUnaryOperator = false;
   
   goto START;
   
   while (true)
   {
      
START:
      
      DEBUG("== Current state: " << status.currentStateAsString());
      DEBUG("== Cursor: " << cursor);
      
      checkIncorrectComparison(cursor, status);

      // We want to skip over formulas if necessary.
      while (skipFormulas(cursor, status))
      {
         if (cursor.isAtEndOfDocument())
            return;
         
         if (!cursor.moveToNextSignificantToken())
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
         
         // Explicitly consume a '!!' or '!!!', to avoid warnings
         // about whitespace used with unquote and unquote-splice
         // operators.
         if (cursor.contentEquals(L"!") &&
             cursor.nextSignificantToken().contentEquals(L"!"))
         {
            do
            {
               MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
            }
            while (cursor.contentEquals(L"!"));
         }
         else if (cursor.contentEquals(L"~"))
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         }
         else
         {
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
         }
      }
      
      // Check for keywords.
      if (isFunctionKeyword(cursor))
         goto FUNCTION_START;
      else if (cursor.contentEquals(L"for"))
         goto FOR_START;
      else if (cursor.contentEquals(L"while"))
         goto WHILE_START;
      else if (cursor.contentEquals(L"if"))
         goto IF_START;
      else if (cursor.contentEquals(L"repeat"))
         goto REPEAT_START;
      
      // Left parenthesis.
      if (cursor.isType(RToken::LPAREN))
      {
         status.pushState(ParseStatus::ParseStateWithinParens);
         status.pushBracket(cursor);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         if (cursor.isType(RToken::RPAREN))
            status.lint().unexpectedToken(cursor);
         goto START;
      }
      
      // Left brace.
      if (cursor.isType(RToken::LBRACE))
      {
         status.pushState(ParseStatus::ParseStateWithinBraces);
         status.pushBracket(cursor);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
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
            // ARGUMENT_START will handle moving over this token
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
            status.popBracket(cursor);
            
            if (cursor.isAtEndOfDocument())
               return;
            
            checkForMissingComma(cursor, status);
            
            // Handle an 'else' following when we are closing an 'if' statement,
            // e.g.
            //
            //    if (1) if (2) (3) else 4
            //                    ^
            //
            if (handleElseToken(cursor, status))
               goto START;
            
            // Check to see if this paren closes the current statement.
            // If so, it effectively closes any parent conditional statements.
            if (cursor.isAtEndOfStatement(status.isInParentheticalScope()))
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
         status.popBracket(cursor);
         
         if (handleElseToken(cursor, status))
            goto START;
         
         status.popState();
         
         if (startedWithUnaryOperator)
            GOTO_INVALID_TOKEN(cursor);
         
         while (status.isInControlFlowStatement())
            status.popState();
         
         if (cursor.isAtEndOfDocument())
            return;
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         if (isBinaryOp(cursor))
            goto BINARY_OPERATOR;

         if (canOpenArgumentList(cursor))
            goto ARGUMENT_LIST;

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
         
         if (cursor.isType(RToken::ID))
         {
            handleIdentifier(cursor, status);
         }
         else if (cursor.isType(RToken::STRING))
         {
            handleString(cursor, status);
         }
         
         if (cursor.isAtEndOfDocument())
         {
            while (status.isInControlFlowStatement())
               status.popState();
            return;
         }
         
         // Identifiers following identifiers on the same line is
         // illegal (except for else), e.g.
         //
         //    a b c                      /* illegal */
         //    if (foo) bar else baz      /*  legal  */
         //    if (1) while (1) 1 else 2  /*  legal  */
         //
         // Check for the 'else' special case first, then the
         // other cases.
         if (handleElseToken(cursor, status))
            goto START;
         
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
         
         // If we encounter an operator, we need to figure out whether it's
         // a unary operator, or a binary operator. For example:
         //
         //    foo
         //    -1
         //
         // parses with '-' as a unary operator, but
         //
         //    (foo
         //     -1)
         //
         // parses with '-' as a binary operator.
         if (isBinaryOp(next))
         {
            // handle '\n' within the token
            std::size_t row =
                  cursor.row() +
                  std::count(cursor.begin(), cursor.end(), '\n');
            
            if (!status.isInParentheticalScope() && next.row() > row)
            {
               DEBUG("----- Not binding binary operator to statement" << next);
               MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
               goto START;
            }
            else
            {
               DEBUG("----- Binary operator: " << next);
               MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
               goto BINARY_OPERATOR;
            }
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
         
         // If we're within a parenthetical scope, it's an
         // error for an identifier to follow.
         checkForMissingComma(cursor, status);
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         goto START;
         
      }

      // End of document. Let's go home.
      if (cursor.isAtEndOfDocument())
         return;
      
      GOTO_INVALID_TOKEN(cursor);
      
BINARY_OPERATOR:

      checkDottyAssignment(cursor, status);
      checkUnexpectedEqualsAssignment(cursor, status);
      checkBinaryOperatorWhitespace(cursor, status);
      if (!cursor.isAtEndOfDocument() && !canFollowBinaryOperator(cursor.nextSignificantToken()))
         status.lint().unexpectedToken(cursor.nextSignificantToken());
      
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
      if (status.parseOptions().checkArgumentsToRFunctionCalls())
         validateFunctionCall(cursor, status);
      
      addExtraScopedSymbolsForCall(cursor, status);
      
      // Update the current state.
      switch (cursor.type())
      {
      
      case RToken::LPAREN:
      {
         status.pushFunctionCallState(
                  ParseStatus::ParseStateParenArgumentList,
                  cursor.previousSignificantToken().content(),
                  status.isWithinNseCall() ||
                     mightPerformNonstandardEvaluation(cursor, status));
         break;
      }
         
      case RToken::LBRACKET:
      {
         status.pushFunctionCallState(
                  ParseStatus::ParseStateSingleBracketArgumentList,
                  cursor.previousSignificantToken().content(),
                  status.isWithinNseCall());
         break;
      }
         
      case RToken::LDBRACKET:
      {
         status.pushFunctionCallState(
                  ParseStatus::ParseStateDoubleBracketArgumentList,
                  cursor.previousSignificantToken().content(),
                  status.isWithinNseCall());
         break;
      }
         
      default:
         GOTO_INVALID_TOKEN(cursor);
      }
      
      // Skip over data.table `[` calls
      if (isDataTableSingleBracketCall(cursor))
         makeSymbolsAvailableInCallFromObjectNames(cursor, status);
      
      status.pushBracket(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
ARGUMENT_START:
      
      if (cursor.isType(RToken::COMMA))
      {
         if (status.parseOptions().checkArgumentsToRFunctionCalls())
         {
            if (cursor.previousSignificantToken().isType(RToken::LPAREN))
               status.lint().missingArgumentToFunctionCall(cursor);
         }
         
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         while (cursor.isType(RToken::COMMA))
         {
            if (status.parseOptions().checkArgumentsToRFunctionCalls())
            {
               if (status.isWithinParenFunctionCall())
                  status.lint().missingArgumentToFunctionCall(cursor);
            }
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         }
      }
      
      if (status.parseOptions().checkUnexpectedAssignmentInFunctionCall())
         checkVariableAssignmentInArgumentList(cursor, status);
      
      if (closesArgumentList(cursor, status))
      {
         // TODO: we previously warned about commas found before a closing
         // parenthesis, e.g.
         //
         //    rnorm(a, b, )
         //               ^
         //
         // but we relax that now as many tidyverse functions now permit
         // an empty trailing argument
         goto ARGUMENT_LIST_END;
      }
      
      // Step over named arguments. Note that it is legal to quote named
      // arguments, with any of [`'"].
      if (cursor.isLookingAtNamedArgumentInFunctionCall())
      {
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
         checkBinaryOperatorWhitespace(cursor, status);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      
      goto START;
      
ARGUMENT_LIST_END:
      
      DEBUG("Argument list end: " << cursor);
      DEBUG("== State: " << status.currentStateAsString());
      
      status.popState();
      status.popBracket(cursor);
      
      DEBUG("== State: " << status.currentStateAsString());
      
      if (handleElseToken(cursor, status))
         goto START;
      
      // Pop out of control flow statements if this ends the statement.
      if (cursor.isAtEndOfStatement(status.isInParentheticalScope()))
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
      
      checkForMissingComma(cursor, status);
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      
      // Check for a 'chain' of function calls, e.g.
      //
      //     x <- foo()(bar)[baz]
      //
      // We need to double-check a couple of things to
      // get this parse correct -- either one of these
      // conditions needs to hold.
      //
      //    1. The '(' token is on the same line, or
      //    2. We're within a 'parenthetical' context.
      if (cursor.isType(RToken::LPAREN) ||
          cursor.isType(RToken::LBRACKET) ||
          cursor.isType(RToken::LDBRACKET))
      {
         if (cursor.row() == cursor.previousSignificantToken().row() ||
             status.isInParentheticalScope())
         {
            goto ARGUMENT_LIST;
         }
      }
      
      if (isBinaryOp(cursor))
         goto BINARY_OPERATOR;
      else
         goto START;
      
FUNCTION_START:
      
      DEBUG("** Function start ** " << cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN, true);
      status.pushBracket(cursor);
      enterFunctionScope(cursor, status);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      if (cursor.isType(RToken::RPAREN))
         goto FUNCTION_ARGUMENT_LIST_END;
      
FUNCTION_ARGUMENT_START:
      
      DEBUG("** Function argument start");
      
      checkVariableAssignmentInArgumentList(cursor, status);
      
      if (cursor.isType(RToken::ID) &&
          (cursor.nextSignificantToken().contentEquals(L"=") ||
           cursor.nextSignificantToken().contentEquals(L"<-")))
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
      
      ENSURE_TYPE(cursor, status, RToken::RPAREN, false);
      status.popState();
      status.popBracket(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      BEGIN_FUNCTION_EXPRESSION(cursor, status);
      goto START;
      
FOR_START:
      
      DEBUG("For start: " << cursor);
      ENSURE_CONTENT(cursor, status, L"for");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN, true);
      status.pushBracket(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::ID, false);
      status.node()->addDefinedSymbol(cursor, cursor.currentPosition());
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      ENSURE_CONTENT(cursor, status, L"in");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      ENSURE_TYPE_NOT(cursor, status, RToken::RPAREN, false);
      status.pushState(ParseStatus::ParseStateForCondition);
      goto START;
      
FOR_CONDITION_END:
      
      DEBUG("** For condition end ** " << cursor);
      ENSURE_TYPE(cursor, status, RToken::RPAREN, false);
      status.popState();
      status.popBracket(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      BEGIN_EXPRESSION(cursor, status, ParseStatus::ParseStateFor, "missing expression following `for (...)`");
      goto START;
      
WHILE_START:
      
      DEBUG("** While start **");
      ENSURE_CONTENT(cursor, status, L"while");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN, true);
      status.pushBracket(cursor);
      status.pushState(ParseStatus::ParseStateWhileCondition);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_BLANK(cursor, status);
      ENSURE_TYPE_NOT(cursor, status, RToken::RPAREN, false);
      DEBUG("** Entering while condition: " << cursor);
      goto START;
      
WHILE_CONDITION_END:
      
      DEBUG("** While condition end ** " << cursor);
      ENSURE_TYPE(cursor, status, RToken::RPAREN, false);
      status.popState();
      status.popBracket(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      BEGIN_EXPRESSION(cursor, status, ParseStatus::ParseStateWhile, "missing expression following `while (...)`");
      goto START;
 
IF_START:
      
      DEBUG("** If start ** " << cursor);
      ENSURE_CONTENT(cursor, status, L"if");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      ENSURE_TYPE(cursor, status, RToken::LPAREN, true);
      status.pushBracket(cursor);
      status.pushState(ParseStatus::ParseStateIfCondition);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      ENSURE_TYPE_NOT(cursor, status, RToken::RPAREN, false);
      if (cursor.isType(RToken::RPAREN))
         goto IF_CONDITION_END;
      goto START;
      
IF_CONDITION_END:
      
      DEBUG("** If condition end ** " << cursor);
      ENSURE_TYPE(cursor, status, RToken::RPAREN, false);
      status.popState();
      status.popBracket(cursor);
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      BEGIN_EXPRESSION(cursor, status, ParseStatus::ParseStateIf, "missing expression following `if (...)`");
      goto START;
      
REPEAT_START:
      
      DEBUG("** Repeat start ** " << cursor);
      ENSURE_CONTENT(cursor, status, L"repeat");
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      BEGIN_EXPRESSION(cursor, status, ParseStatus::ParseStateRepeat, "missing expression following `repeat`");
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
