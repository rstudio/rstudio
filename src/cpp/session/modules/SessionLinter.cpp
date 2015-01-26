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

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/r_util/RTokenizer.hpp>
#include <core/FileUtils.hpp>
#include <core/collection/Tree.hpp>

#define RSTUDIO_ENABLE_DEBUG_MACROS
#define RSTUDIO_DEBUG_LABEL "linter"
#include <core/Macros.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

using namespace rstudio::core;
using namespace rstudio::core::r_util;
using namespace rstudio::core::r_util::token_utils;

class AnnotatedRToken
{
public:
   
   AnnotatedRToken(std::size_t row,
                   std::size_t column,
                   const RToken& token)
      : token_(token), row_(row), column_(column) {}
   
   const RToken& get() const
   {
      return token_;
   }
   
   const std::size_t row() const
   {
      return row_;
   }
   
   const std::size_t column() const
   {
      return column_;
   }
   
   wchar_t type() const { return token_.type(); }
   bool isType(wchar_t type) const { return token_.isType(type); }
   std::wstring content() const { return token_.content(); }
   std::string contentAsUtf8() const { return token_.contentAsUtf8(); }
   
   bool contentEquals(const std::wstring& text) const
   {
      return std::equal(token_.begin(), token_.end(), text.begin());
   }
   
   const RToken& token() const
   {
      return token_;
   }
   
   operator const RToken&() const
   {
      return token_;
   }
   
   std::string asString() const
   {
      std::stringstream ss;
      ss << "("
         << row_
         << ", "
         << column_
         << ", '"
         << string_utils::jsonLiteralEscape(token_.contentAsUtf8())
         << "')";
      
      return ss.str();
   }
   
private:
   RToken token_;
   std::size_t row_;
   std::size_t column_;
};

class AnnotatedRTokens
{
public:
   
   // NOTE: Must be constructed from tokens that have not
   // stripped whitespace
   explicit AnnotatedRTokens(const RTokens& rTokens)
      : dummyToken_(AnnotatedRToken(-1, -1, RToken()))
   {
      std::size_t row = 0;
      std::size_t column = 0;
      
      std::size_t n = rTokens.size();
      for (std::size_t i = 0; i < n; ++i)
      {
         // Add the token if it's not whitespace
         const RToken& token = rTokens.at(i);
         tokens_.push_back(
                  AnnotatedRToken(row, column, token));
         
         // Update the current row, column
         std::wstring content = token.content();
         std::size_t numNewLines =
               string_utils::countNewLines(content);
         
         if (numNewLines > 0)
         {
            row += numNewLines;
            column = content.length() - content.find_last_of(L"\r\n") - 1;
         }
         else
         {
            column += content.length();
         }
      }
   }
   
   const AnnotatedRToken& at(std::size_t index) const
   {
      if (index >= tokens_.size())
         return dummyToken_;
      
      return tokens_[index];
   }
   
   const std::size_t size() const
   {
      return tokens_.size();
   }
   
   void dump()
   {
      std::cerr
            << "Dumping " << tokens_.size() << " tokens:" << std::endl;
      
      for (std::size_t i = 0; i < tokens_.size(); ++i)
      {
         const AnnotatedRToken& token = tokens_[i];
         std::cerr
               << "{"
               << token.row()
               << ", "
               << token.column()
               << ", "
               << "'" << string_utils::jsonLiteralEscape(token.contentAsUtf8()) << "'"
               << "}"
               << std::endl;
      }
   }
   
private:
   std::vector<AnnotatedRToken> tokens_;
   AnnotatedRToken dummyToken_;
   
};

enum LintType
{
   LintTypeStyle,
   LintTypeInfo,
   LintTypeWarning,
   LintTypeError
};

std::string asString(LintType type)
{
   switch (type)
   {
   case LintTypeStyle: return "style";
   case LintTypeInfo: return "info";
   case LintTypeWarning: return "warning";
   case LintTypeError: return "error";
   }
   
   return std::string();
}

struct LintItem
{

   LintItem (int startRow,
             int startColumn,
             int endRow,
             int endColumn,
             LintType type,
             const std::string message)
      : startRow(startRow),
        startColumn(startColumn),
        endRow(endRow),
        endColumn(endColumn),
        type(type),
        message(message) {}

   int startRow;
   int startColumn;
   int endRow;
   int endColumn;
   LintType type;
   std::string message;
};

struct ParseItem
{
   ParseItem(int row, int column, const std::string& name)
      : row(row), column(column), name(name) {}
   
   explicit ParseItem(const AnnotatedRToken& rToken)
      : row(rToken.row()), column(rToken.column())
   {
      name = rToken.contentAsUtf8();
   }
   
   bool operator <(const ParseItem& other) const
   {
      if (row < other.row)
         return true;
      
      if (row > other.row)
         return false;
      
      if (column < other.column)
         return true;
      
      if (column > other.column)
         return false;
      
      // shouldn't be reached, but whatever
      return name < other.name;
   }
   
   int row;
   int column;
   std::string name;
};

class LintItems
{
private:
   
public:
   // default ctors: copyable members
   
   void add(int startRow,
            int startColumn,
            int endRow,
            int endColumn,
            LintType type,
            const std::string& message)
   {
      LintItem item(startRow,
                    startColumn,
                    endRow,
                    endColumn,
                    type,
                    message);
      
      lintItems_.push_back(item);
   }
   
   void addUnexpectedToken(const AnnotatedRToken& token,
                           const std::string& expected = std::string())
   {
      std::string content = token.contentAsUtf8();
      std::string message = "Unexpected token '" + content + "'";
      if (!expected.empty())
         message = message + ", expected " + expected;
      
      LintItem item(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + content.length(),
                    LintTypeError,
                    message);
      
      lintItems_.push_back(item);
   }
   
   void addUnexpectedToken(const AnnotatedRToken& token,
                           const std::wstring& expected)
   {
      addUnexpectedToken(token, string_utils::wideToUtf8(expected));
   }
   
   void addUnmatchedToken(const AnnotatedRToken& token)
   {
      std::string content = token.contentAsUtf8();
      
      LintItem item(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + content.length(),
                    LintTypeError,
                    "unmatched bracket '" + content + "'");
      
      lintItems_.push_back(item);
   }
   
   void addUnexpectedEndOfDocument(const AnnotatedRToken& token)
   {
      LintItem item(token.row(),
                    token.column() + token.contentAsUtf8().length(),
                    token.row(),
                    token.column() + token.contentAsUtf8().length(),
                    LintTypeError,
                    "unexpected end of document");
      lintItems_.push_back(item);
   }
   
   void addUnreferencedSymbol(const ParseItem& item)
   {
      LintItem lint(item.row,
                    item.column,
                    item.row,
                    item.column + item.name.size(),
                    LintTypeWarning,
                    "no reference for symbol '" + item.name + "'");
      lintItems_.push_back(lint);
   }
   
   const std::vector<LintItem>& get() const
   {
      return lintItems_;
   }
   
private:
   std::vector<LintItem> lintItems_;
};


class ParseNode
{
private:
   
   // private constructor: root node should be created through
   // 'createRootNode()', with future nodes appended to that node;
   // child nodes with 'createChildNode()'
   explicit ParseNode(ParseNode* pParent,
                      const std::string& name)
      : pParent_(pParent), name_(name) {}
   
public:
   
   ~ParseNode()
   {
   }
   
   static boost::shared_ptr<ParseNode> createRootNode()
   {
      return boost::shared_ptr<ParseNode>(new ParseNode(NULL, "<root>"));
   }
   
   static boost::shared_ptr<ParseNode> createNode(
         const std::string& name)
   {
      return boost::shared_ptr<ParseNode>(
               new ParseNode(NULL, name));
   }
   
   bool isRootNode() const
   {
      return pParent_ == NULL;
   }

   void addDefinedVariable(int row,
                           int column,
                           const std::string& name)
   {
      definedVariables_.insert(
               ParseItem(row, column, name));
   }

   void addDefinedVariable(const AnnotatedRToken& rToken)
   {
      definedVariables_.insert(
               ParseItem(rToken));
   }
   
   void addReferencedVariable(int row,
                           int column,
                           const std::string& name)
   {
      referencedVariables_.insert(
               ParseItem(row, column, name));
   }

   void addReferencedVariable(const AnnotatedRToken& rToken)
   {
      referencedVariables_.insert(
               ParseItem(rToken));
   }
   
   void addInternalPackageSymbol(const std::string& package,
                                 const std::string& name)
   {
      internalSymbols_[package].insert(name);
   }
   
   void addExportedPackageSymbol(const std::string& package,
                                 const std::string& name)
   {
      exportedSymbols_[package].insert(name);
   }
   
   const std::string& getName() const
   {
      return name_;
   }
   
   // Tree-related operations
   
   ParseNode* getParent() const
   {
      return pParent_;
   }
   
   ParseNode* getRoot() const
   {
      ParseNode* pNode = const_cast<ParseNode*>(this);
      while (pNode->pParent_ != NULL)
         pNode = pNode->pParent_;
      
      return pNode;
   }
   
   std::vector< boost::shared_ptr<ParseNode> > getChildren() const
   {
      return children_;
   }
   
   void addChild(boost::shared_ptr<ParseNode> pChild)
   {
      pChild->pParent_ = this;
      children_.push_back(pChild);
   }
   
   friend void findAllUnresolvedSymbols(
         const ParseNode& node,
         const std::set<std::string> objects,
         LintItems* pLintItems)
   {
      // Get the unresolved symbols at this node
      std::set<ParseItem> unresolved = node.getUnresolvedSymbols(objects);
      
      // Add them to the set of lint items
      for (std::set<ParseItem>::iterator it = unresolved.begin();
           it != unresolved.end();
           ++it)
      {
         pLintItems->addUnreferencedSymbol(*it);
      }
      
      // Apply this over all children on the node
      typedef std::vector< boost::shared_ptr<ParseNode> > Children;
      Children children = node.getChildren();
      
      for (Children::iterator it = children.begin();
           it != children.end();
           ++it)
      {
         findAllUnresolvedSymbols(**it, objects, pLintItems);
      }
      
   }
   
private:
   
   std::set<ParseItem> getUnresolvedSymbols(
         const std::set<std::string>& objects) const
   {
      // Start by assuming everything is unresolved, and then resolve
      // as we find references.
      std::set<ParseItem> items(referencedVariables_.begin(),
                                referencedVariables_.end());
      
      // Remove any parse items which are found on the
      // search path
      BOOST_FOREACH(const std::string& object, objects)
      {
         std::set<ParseItem>::const_iterator it =
               std::find_if(items.begin(),
                            items.end(),
                            HasSymbolNamed(object));
         if (it != items.end())
            items.erase(it);
      }

      // Now, check the list against variables defined
      // in the parse tree
      doGetUnresolvedSymbols(*this, &items);
      return items;
   }
   
   struct HasSymbolNamed
   {
      
      explicit HasSymbolNamed(const std::string& name)
         : name_(name) {}
      
      bool operator()(const ParseItem& item) const
      {
         return item.name == name_;
      }
      
      std::string name_;
   };
   
   void doGetUnresolvedSymbols(const ParseNode& node,
                               std::set<ParseItem>* pItems) const
   {
      typedef std::set<ParseItem>::iterator iterator;
      
      // Iterate over all defined variables
      for (iterator it = node.definedVariables_.begin();
           it != node.definedVariables_.end();
           ++it)
      {
         iterator found = std::find_if(
                  pItems->begin(),
                  pItems->end(),
                  HasSymbolNamed(it->name));

         if (found != pItems->end())
            pItems->erase(found);
      }
      
      // If we have a parent closure, check there as well
      if (node.pParent_ != NULL)
         doGetUnresolvedSymbols(*(node.pParent_), pItems);
   }
   
private:
   
   // tree reference -- children and parent
   ParseNode* pParent_;
   std::vector< boost::shared_ptr<ParseNode> > children_;
   
   // member variables
   std::string name_; // name of scope (usually function name)
   std::set<ParseItem> definedVariables_; // variables defined in this scope
   std::set<ParseItem> referencedVariables_; // variables referenced in this scope
   
   typedef std::map<
      std::string,
      std::set<std::string>
   > PackageSymbols;
   
   PackageSymbols internalSymbols_; // <pkg>::<foo>
   PackageSymbols exportedSymbols_; // <pgk>:::<bar>
};

class TokenCursor
{
private:
   
   TokenCursor(const AnnotatedRTokens &rTokens,
               std::size_t offset,
               std::size_t n)
      : rTokens_(rTokens), offset_(offset), n_(n) {}
   
public:
   
   explicit TokenCursor(const AnnotatedRTokens& rTokens)
      : rTokens_(rTokens), offset_(0), n_(rTokens.size()) {}
   
   TokenCursor(const AnnotatedRTokens &rTokens,
               std::size_t offset)
      : rTokens_(rTokens), offset_(offset), n_(rTokens.size()) {}
   
   TokenCursor clone() const
   {
      return TokenCursor(rTokens_, offset_, n_);
   }
   
   bool moveToNextToken()
   {
      if (UNLIKELY(offset_ == n_ - 1))
         return false;
      
      ++offset_;
      return true;
   }
   
   bool moveToPreviousToken()
   {
      if (UNLIKELY(offset_ == 0))
         return false;
      
      --offset_;
      return true;
   }
   
   const AnnotatedRToken& currentToken() const
   {
      return rTokens_.at(offset_);
   }
   
   const AnnotatedRToken& nextToken() const
   {
      return rTokens_.at(offset_ + 1);
   }
   
   const AnnotatedRToken& previousToken() const
   {
      return rTokens_.at(offset_ - 1);
   }
   
   const AnnotatedRToken& nextSignificantToken(std::size_t times = 1) const
   {
      int offset = 0;
      while (times != 0)
      {
         ++offset;
         while (isWhitespace(rTokens_.at(offset_ + offset)))
            ++offset;
         
         --times;
      }
      
      return rTokens_.at(offset_ + offset);
   }
   
   const AnnotatedRToken& previousSignificantToken(std::size_t times = 1) const
   {
      int offset = 0;
      while (times != 0)
      {
         ++offset;
         while (isWhitespace(rTokens_.at(offset_ - offset)))
            ++offset;
         
         --times;
      }
      
      return rTokens_.at(offset_ - offset);
   }
   
   operator const AnnotatedRToken&() const
   {
      return rTokens_.at(offset_);
   }
   
   operator const RToken&() const
   {
      return rTokens_.at(offset_).token();
   }
   
   bool moveToNextSignificantToken()
   {
      if (!moveToNextToken())
         return false;
      
      if (!fwdOverWhitespace())
         return false;
      
      return true;
   }
   
   bool moveToPreviousSignificantToken()
   {
      if (!moveToPreviousToken())
         return false;
      
      if (!bwdOverWhitespace())
         return false;
      
      return true;
   }
   
   bool contentEquals(const std::wstring& content) const
   {
      return rTokens_.at(offset_).contentEquals(content);
   }
   
   bool fwdOverWhitespace()
   {
      while (currentToken().isType(RToken::WHITESPACE))
         if (!moveToNextToken())
            return false;
      return true;
   }
   
   bool bwdOverWhitespace()
   {
      while (currentToken().isType(RToken::WHITESPACE))
         if (!moveToPreviousToken())
            return false;
      return true;
   }
   
   friend std::ostream& operator <<(std::ostream& os, const TokenCursor& cursor)
   {
      os << cursor.currentToken().asString();
      return os;
   }

#define RSTUDIO_FWD_TO_MATCHING_TOKEN_2(__LEFT_OP__, __RIGHT_OP__)             \
   do                                                                          \
   {                                                                           \
      if (currentToken().type() == __LEFT_OP__)                                \
      {                                                                        \
         while (currentToken().type() != __RIGHT_OP__ && moveToNextToken())    \
            ;                                                                  \
         return currentToken().type() == __RIGHT_OP__;                         \
      }                                                                        \
   } while (0)

#define RSTUDIO_FWD_TO_MATCHING_TOKEN(__TOKEN_NAME__)                          \
   RSTUDIO_FWD_TO_MATCHING_TOKEN_2(RToken::L##__TOKEN_NAME__,                  \
                                   RToken::R##__TOKEN_NAME__);

#define RSTUDIO_BWD_TO_MATCHING_TOKEN_2(__LEFT_OP__, __RIGHT_OP__)             \
   do                                                                          \
   {                                                                           \
      if (currentToken().type() == __RIGHT_OP__)                               \
      {                                                                        \
         while (currentToken().type() != __LEFT_OP__ && moveToPreviousToken()) \
            ;                                                                  \
         return currentToken().type() == __LEFT_OP__;                          \
      }                                                                        \
   } while (0)

#define RSTUDIO_BWD_TO_MATCHING_TOKEN(__TOKEN_NAME__)                          \
   RSTUDIO_BWD_TO_MATCHING_TOKEN_2(RToken::L##__TOKEN_NAME__,                  \
                                   RToken::R##__TOKEN_NAME__);

   bool fwdToMatchingToken()
   {
      RSTUDIO_FWD_TO_MATCHING_TOKEN(BRACE);
      RSTUDIO_FWD_TO_MATCHING_TOKEN(PAREN);
      RSTUDIO_FWD_TO_MATCHING_TOKEN(BRACKET);
      RSTUDIO_FWD_TO_MATCHING_TOKEN(DBRACKET);
      return false;
   }
   
   bool bwdToMatchingToken()
   {
      RSTUDIO_BWD_TO_MATCHING_TOKEN(BRACE);
      RSTUDIO_BWD_TO_MATCHING_TOKEN(PAREN);
      RSTUDIO_BWD_TO_MATCHING_TOKEN(BRACKET);
      RSTUDIO_BWD_TO_MATCHING_TOKEN(DBRACKET);
      return false;
   }

#undef RSTUDIO_BWD_TO_MATCHING_TOKEN
#undef RSTUDIO_FWD_TO_MATCHING_TOKEN
#undef RSTUDIO_MOVE_TO_MATCHING_TOKEN

private:
   
   const AnnotatedRTokens& rTokens_;
   std::size_t offset_;
   std::size_t n_;
};

namespace {

// Utility macros for inspecting and handling tokens
#define MOVE_TO_NEXT_TOKEN(__CURSOR__, __LINT_ITEMS__)                         \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.moveToNextToken())                                       \
      {                                                                        \
         DEBUG("Encountered unexpected end of document");                      \
         __LINT_ITEMS__.addUnexpectedEndOfDocument(__CURSOR__.currentToken()); \
         return;                                                               \
      }                                                                        \
   } while (0)

#define FWD_OVER_WHITESPACE(__CURSOR__, __LINT_ITEMS__)                        \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.fwdOverWhitespace())                                     \
      {                                                                        \
         DEBUG("Encountered unexpected end of document");                      \
         __LINT_ITEMS__.addUnexpectedEndOfDocument(__CURSOR__.currentToken()); \
         return;                                                               \
      }                                                                        \
   } while (0)

void handleIdToken(TokenCursor& cursor,
                   ParseNode* pNode,
                   LintItems& lintItems)
{
   // If the following token is a '::' or ':::', then
   // we add to the set of namespace entries used.
   if (isNamespace(cursor.nextSignificantToken()))
   {
      // Validate that the entries before and after the '::' are either
      // strings, symbols or identifiers
      const AnnotatedRToken& pkgToken = cursor;
      const AnnotatedRToken& nsToken = cursor.nextSignificantToken();
      const AnnotatedRToken& exportedToken = cursor.nextSignificantToken(2);
      
      if (!(isString(pkgToken) ||
            isId(pkgToken)))
      {
         lintItems.addUnexpectedToken(pkgToken, "'string' or 'id'");
         return;
      }
      
      if (!(isString(exportedToken) ||
            isId(exportedToken)))
      {
         lintItems.addUnexpectedToken(exportedToken, "'string' or 'id'");
         return;
      }
      
      if (nsToken.contentEquals(L"::"))
         pNode->addExportedPackageSymbol(
                  pkgToken.contentAsUtf8(),
                  exportedToken.contentAsUtf8());
      else
         pNode->addInternalPackageSymbol(
                  pkgToken.contentAsUtf8(),
                  exportedToken.contentAsUtf8());

      return;
   }

   // If the previous symbol is a '$' or an '@', then we don't
   // touch it for now.
   //
   // TODO: Add another map for 'containers' in a scope?
   const AnnotatedRToken& prevToken = cursor.previousSignificantToken();
   if (isAt(prevToken) ||
       isDollar(prevToken))
      return;

   // Check to see if there is a left assign following the token.
   // If so, add that symbol to this scope.
   const AnnotatedRToken& nextToken = cursor.nextSignificantToken();
   DEBUG("Next token: " << nextToken.asString());
   
   if (isLocalLeftAssign(nextToken))
   {
      DEBUG("Adding defined variable '" << cursor.currentToken().contentAsUtf8() << "'");
      pNode->addDefinedVariable(cursor);
   }

   // Similarily for previous assign
   if (isLocalRightAssign(prevToken))
   {
      DEBUG("Adding defined variable '" << cursor.currentToken().contentAsUtf8() << "'");
      pNode->addDefinedVariable(cursor);
   }

   // TODO: Handle 'global' assign. This implies searching
   // parent scopes to see if the variable is defined locally
   // or defined in a parent scope.

   // Add a reference to this variable
   DEBUG("Adding reference to variable '" << cursor.currentToken().contentAsUtf8() << "'");
   pNode->addReferencedVariable(cursor);
   
}

// Take a token stream of the form:
//
//    x <- function(a, b, c) {
//         ^
// and:
// 1. Populates the current scope with variables in the argument list,
// 2. Creates a new node for the function.
void handleFunctionToken(TokenCursor& cursor,
                         ParseNode** ppNode,
                         LintItems& lintItems)
{
   // ensure we're on a function token
   DEBUG_BLOCK
   {
      if (!cursor.contentEquals(L"function"))
      {
         DEBUG("ASSERTION ERROR: Expected token cursor to lie on 'function' token");
         return;
      }
   }
   
   ParseNode* pNode = *ppNode;
   
   // there should be a '(' following the function keyword
   MOVE_TO_NEXT_TOKEN(cursor, lintItems);
   
   if (!cursor.contentEquals(L"("))
   {
      lintItems.addUnexpectedToken(
               cursor,
               "(");
      return;
   }
   
   boost::shared_ptr<ParseNode> pChild =
         ParseNode::createNode("TODO");
   
   // Check to see if we have no arguments for this function.
   MOVE_TO_NEXT_TOKEN(cursor, lintItems);
   
   if (cursor.contentEquals(L")"))
   {
      DEBUG("Argument list for function is empty; skipping");
      goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
   }
   
   // Start running through the function arguments. This loop should
   // begin at the start of a new function block; ie, on an identifier.
   // We break out when a ')' or other unmatched closing brace is identified.
   DEBUG("Parsing argument list for function");
   do
   {
      START:
      
      FWD_OVER_WHITESPACE(cursor, lintItems);
      DEBUG("Current token: " << cursor.currentToken().asString());
      
      // start on identifier
      if (!isId(cursor))
         lintItems.addUnexpectedToken(cursor, "id");
      else
      {
         DEBUG("Adding definition: '" << cursor.currentToken().contentAsUtf8() << "'");
         pChild->addDefinedVariable(cursor);
      }
      
      MOVE_TO_NEXT_TOKEN(cursor, lintItems);
      FWD_OVER_WHITESPACE(cursor, lintItems);
      
      // expect either a ',' or '=' (for default arg)
      if (isComma(cursor))
      {
         DEBUG("Found comma; parsing next argument");
         continue;
      }
      else if (cursor.contentEquals(L"="))
      {
         DEBUG("Found '='; looking for next argument name");
         
         // TODO: Parse expression following '='. 
         // For now, we just look for a comma (starting a new argument)
         // or a ')' (closing the argument list)
         while (true)
         {
            MOVE_TO_NEXT_TOKEN(cursor, lintItems);
            
            // TODO: We skip whitespace but stylistically we might warn
            // about whitespace before / after '='
            FWD_OVER_WHITESPACE(cursor, lintItems);
            DEBUG("Current token: " << cursor);
            
            if (isLeftBrace(cursor))
            {
               if (cursor.fwdToMatchingToken())
                  continue;
               else
               {
                  lintItems.addUnmatchedToken(cursor);
                  return; // TODO: continue parsing?
               }
            }
            
            if (isComma(cursor))
            {
               // We found a comma; continue parsing other function arguments
               DEBUG("Found comma in function argument list; parsing next arguments");
               MOVE_TO_NEXT_TOKEN(cursor, lintItems);
               goto START;
            }
            
            if (isRightBrace(cursor))
            {
               // Found a closing brace; this should be a ')'.
               // Earlier branches should have handled walking over other matching
               // braces ('{' to '}', etc)
               if (!cursor.contentEquals(L")"))
                  lintItems.addUnexpectedToken(cursor, ")");
               
               goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
            }
         }
      }
      else if (isRightBrace(cursor))
      {
         if (!cursor.contentEquals(L")"))
            lintItems.addUnexpectedToken(cursor, ")");
         
         goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
      }
      {
         DEBUG("Unexpected token: expected ',', '=' or ')' following identifier");
         lintItems.addUnexpectedToken(cursor, "',' or '='");
      }
      
   } while (cursor.moveToNextToken());
      
   ENCOUNTERED_END_OF_ARGUMENT_LIST:
   
   // Following the paren that closes a function argument list, we may
   // either be opening a new block, or defining a function with a single
   // statement.
   //
   // TODO: Handle single-expression functions, e.g.
   // foo <- function() bar()
   DEBUG("Cursor at end of argument list: " << cursor.currentToken().contentAsUtf8());
   MOVE_TO_NEXT_TOKEN(cursor, lintItems);
   FWD_OVER_WHITESPACE(cursor, lintItems);
   
   if (cursor.contentEquals(L"{"))
   {
      DEBUG("Found '{' opening function body");
      pNode->addChild(pChild);
      *ppNode = pChild.get();
   }
   
}

void handleStringToken(TokenCursor& cursor,
                       ParseNode* pNode,
                       LintItems& lintItems)
{
   if (isLocalLeftAssign(cursor.nextSignificantToken()))
      pNode->addDefinedVariable(cursor);
}

std::map<std::wstring, std::wstring> makeComplementVector()
{
   std::map<std::wstring, std::wstring> m;
   m[L"["] = L"]";
   m[L"("] = L")";
   m[L"{"] = L"}";
   m[L"[["] = L"]]";
   m[L"]"] = L"[";
   m[L")"] = L"(";
   m[L"}"] = L"{";
   m[L"]]"] = L"[[";
   return m;
}

static std::map<std::wstring, std::wstring> s_complements =
      makeComplementVector();

std::wstring complement(const std::wstring string)
{
   return s_complements[string];
}

} // end anonymous namespace

typedef std::pair<
   boost::shared_ptr<ParseNode>,
   LintItems> LintResults;

LintResults parseAndLintRFile(const FilePath& filePath,
                              const std::set<std::string>& objects)
{
   std::string contents = file_utils::readFile(filePath);
   
   RTokens tokens(string_utils::utf8ToWide(contents));
   AnnotatedRTokens rTokens(tokens);
   
   // Create an empty tree to populate
   boost::shared_ptr<ParseNode> root =
         ParseNode::createRootNode();
   
   // The pointer we use to walkt the tree is a raw pointer, to avoid
   // accidentally having multiple shared pointers to a parent node.
   ParseNode* pNode = root.get();
   
   // Create a vector of lint objects to fill as we parse
   LintItems lintItems;
   
   // Brace stack
   std::vector<std::wstring> braceStack;
   std::size_t braceCount = 0;
   
   TokenCursor cursor(rTokens);
   
   do
   {
      DEBUG("Current token: " << cursor);
      
      // Update the brace stack
      if (isLeftBrace(cursor))
      {
         DEBUG("Encountered '" << cursor.currentToken().contentAsUtf8() << "'; updating brace stack");
         braceStack.push_back(cursor.currentToken().content());
         braceCount += cursor.currentToken().contentEquals(L"{") ? 1 : 0;
         continue;
      }
      
      // If we encounter a right brace, check to see if
      // its partner is on the token stack; if not, parse error
      if (isRightBrace(cursor))
      {
         DEBUG("Encountered '" << cursor.currentToken().contentAsUtf8() << "', updating brace stack");
         if (cursor.contentEquals(L"}") && pNode->getParent() != NULL)
         {
            DEBUG("Encountered '}' closing function block");
            pNode = pNode->getParent();
            continue;
         }
         else if (braceStack.empty())
         {
            DEBUG("Unexpected token " << cursor.currentToken().asString());
            lintItems.addUnexpectedToken(cursor);
            continue;
         }
         else
         {
            const std::wstring& toBePopped =
                  braceStack.at(braceStack.size() - 1);

            if (complement(cursor.currentToken().content()) != toBePopped)
            {
               DEBUG("Unexpected token " << cursor);
               lintItems.addUnexpectedToken(cursor, complement(toBePopped));
            }
            else
            {
               DEBUG("Popping brace stack");
               braceStack.pop_back();
            }
            continue;
         }
      }
      
      // Handle a 'function' token.
      if (isFunction(cursor))
      {
         DEBUG("Handling function token");
         handleFunctionToken(cursor, &pNode, lintItems);
         continue;
      }
      
      // Handle an identifier (implies we should check to see if it has a reference
      // in the parse tree)
      if (isId(cursor))
      {
         DEBUG("Handling ID token");
         handleIdToken(cursor, pNode, lintItems);
         continue;
      }
      
      // Strings can be assigned to, too. One could do something like:
      //
      //    "abc" <- function() {}
      //    "abc"()
      if (isString(cursor))
      {
         DEBUG("Handling string token");
         handleStringToken(cursor, pNode, lintItems);
         continue;
      }
      
      // If we encounter a '{', add to the stack. Note that 'isFunction()'
      // will have moved over such an open brace and hence these open
      // braces are just opening expressions (not associated with a closure)
      if (cursor.contentEquals(L"{"))
      {
         DEBUG("Incrementing '{' count");
         ++braceCount;
         continue;
      }
      
      // If we encounter a '}', decrement the stack,
      // or close a function node.
      if (cursor.contentEquals(L"}"))
      {
         if (braceCount == 0)
         {
            // It is possible that this is a parse error (ie this '}' has no
            // scope to close). If that's the case the current node will be
            // the root and we should warn
            if (pNode->isRootNode())
            {
               DEBUG("Unexpected token '}' -- no function scope to close");
               lintItems.addUnexpectedToken(cursor);
            }
            else
            {
               pNode = pNode->getParent();
            }
         }
         else
            --braceCount;
         
         continue;
      }
   } while (cursor.moveToNextToken());
   
   DEBUG("- Finished building parse tree");
   if (!braceStack.empty())
   {
      DEBUG("Unexpected end of document; unclosed brace(s)");
      lintItems.addUnexpectedEndOfDocument(cursor.currentToken());
   }
   
   // TODO: Should we report missing matches? Probably need a way
   // of specifying a 'full' lint vs. 'partial' lint; ie,
   // to differentiate between 'does it look okay so far' and
   // 'is this entire document okay'.
   if (pNode->getParent() != NULL)
   {
      DEBUG("Unexpected end of document; unclosed function brace");
      lintItems.addUnexpectedEndOfDocument(cursor.currentToken());
   }
   
   // Now that we have parsed through the whole document and
   // built a lint tree, we now walk through each identifier and
   // ensure that they exist either on the search path, or a parent
   // closure.
   findAllUnresolvedSymbols(*root, objects, &lintItems);
   
   return std::make_pair(root, lintItems);
}

SEXP rs_parseAndLintRFile(SEXP pathSEXP,
                          SEXP searchPathObjectsSEXP)
{
   std::string path = r::sexp::asString(pathSEXP);
   FilePath filePath(module_context::resolveAliasedPath(path));
   
   // NOTE: Want to optimize for fast lookup.
   std::set<std::string> objects;
   bool success = r::sexp::fillSetString(searchPathObjectsSEXP, &objects);
   if (!success)
   {
      DEBUG("Failed to fill search path vector");
      return R_NilValue;
   }
   
   LintResults lintResults = parseAndLintRFile(filePath, objects);
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
