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

}

struct Position
{
   
   Position(std::size_t row = 0, std::size_t column = 0)
      : row(row), column(column) {}
   
   friend bool operator <(const Position& lhs,
                          const Position& rhs)
   {
      if (lhs.row == rhs.row)
         return lhs.column < rhs.column;
      else
         return lhs.row < rhs.row;
   }
   
   friend bool operator <=(const Position& lhs,
                           const Position& rhs)
   {
      if (lhs.row == rhs.row)
         return lhs.column <= rhs.column;
      return lhs.row < rhs.row;
   }
   
   friend bool operator ==(const Position& lhs,
                           const Position& rhs)
   {
      return lhs.row == rhs.row && lhs.column == rhs.column;
   }
   
   std::string toString() const
   {
      std::stringstream ss;
      ss << "(" << row << ", " << column << ")";
      return ss.str();
   }
   
   std::size_t row;
   std::size_t column;
};

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
      return token_.contentEquals(text);
   }
   
   bool contentContains(wchar_t character) const
   {
      return token_.contentContains(character);
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

// Forward decl
class ParseNode;

struct ParseItem
{
   explicit ParseItem(const std::string& symbol,
                      const Position& position,
                      const ParseNode* pNode)
      : symbol(symbol),
        position(position),
        pNode(pNode) {}
   
   bool operator <(const ParseItem& other) const
   {
      return position < other.position;
   }
   
   std::string symbol;
   Position position;
   const ParseNode* pNode;
   
};

class LintItems
{
public:
   
   LintItems()
      : errorCount_(0) {}
   
   // default ctors: copyable members
   
   void add(int startRow,
            int startColumn,
            int endRow,
            int endColumn,
            LintType type,
            const std::string& message)
   {
      LintItem lint(startRow,
                    startColumn,
                    endRow,
                    endColumn,
                    type,
                    message);
      
      lintItems_.push_back(lint);
   }
   
   void addUnexpectedToken(const AnnotatedRToken& token,
                           const std::string& expected = std::string())
   {
      std::string content = token.contentAsUtf8();
      std::string message = "Unexpected token '" + content + "'";
      if (!expected.empty())
         message = message + ", expected " + expected;
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + content.length(),
                    LintTypeError,
                    message);
      
      lintItems_.push_back(lint);
      ++errorCount_;
   }
   
   void addUnexpectedToken(const AnnotatedRToken& token,
                           const std::wstring& expected)
   {
      addUnexpectedToken(token, string_utils::wideToUtf8(expected));
   }
   
   void addUnmatchedToken(const AnnotatedRToken& token)
   {
      std::string content = token.contentAsUtf8();
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + content.length(),
                    LintTypeError,
                    "unmatched bracket '" + content + "'");
      
      lintItems_.push_back(lint);
      ++errorCount_;
   }
   
   void addUnexpectedEndOfDocument(const AnnotatedRToken& token)
   {
      LintItem lint(token.row(),
                    token.column() + token.contentAsUtf8().length(),
                    token.row(),
                    token.column() + token.contentAsUtf8().length(),
                    LintTypeError,
                    "unexpected end of document");
      lintItems_.push_back(lint);
   }
   
   void addUnreferencedSymbol(const ParseItem& item,
                              const std::string& candidate)
   {
      std::string message("no symbol named '" + item.symbol + "' in scope");
      if (!candidate.empty())
         message += "; did you mean '" + candidate + "'?";
      
      LintItem lint(item.position.row,
                    item.position.column,
                    item.position.row,
                    item.position.column + item.symbol.size(),
                    LintTypeWarning,
                    message);
      
      lintItems_.push_back(lint);
   }
   
   void addSymbolDefinedAhead(const ParseItem& item,
                              const Position& position)
   {
      LintItem lint(position.row,
                    position.column,
                    position.row,
                    position.column + item.symbol.length(),
                    LintTypeInfo,
                    "'" + item.symbol + "' is defined after it is used");
      
      lintItems_.push_back(lint);
   }
   
   void addShouldUseWhitespace(const AnnotatedRToken& token)
   {
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column(),
                    LintTypeStyle,
                    "expected whitespace");
      lintItems_.push_back(lint);
   }
   
   void addShouldRemoveWhitespace(const AnnotatedRToken& token)
   {
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column(),
                    LintTypeStyle,
                    "unnecessary whitespace");
      lintItems_.push_back(lint);
   }
   
   void addTooManyErrors(const Position& position)
   {
      LintItem lint(position.row,
                    position.column,
                    position.row,
                    position.column,
                    LintTypeError,
                    "too many errors emitted; stopping now");
      lintItems_.push_back(lint);
      ++errorCount_;
   }
   
   
   const std::vector<LintItem>& get() const
   {
      return lintItems_;
   }
   
   void push_back(const LintItem& item)
   {
      lintItems_.push_back(item);
   }
   
   typedef std::vector<LintItem>::iterator iterator;
   typedef std::vector<LintItem>::const_iterator const_iterator;
   
   iterator begin() { return lintItems_.begin(); }
   iterator end() { return lintItems_.end(); }
   
   std::size_t errorCount()
   {
      return errorCount_;
   }
   
private:
   std::vector<LintItem> lintItems_;
   std::size_t errorCount_;
};


class ParseNode
{
public:
   
   typedef std::vector<Position> Positions;
   typedef std::map<std::string, Positions> SymbolPositions;
   
private:
   
   // private constructor: root node should be created through
   // 'createRootNode()', with future nodes appended to that node;
   // child nodes with 'createChildNode()'.
   //
   // 'start' refers to the location of the opening brace opening
   // the node (or, [0, 0] for the root node)
   ParseNode(ParseNode* pParent,
             const std::string& name,
             Position position)
      : pParent_(pParent), name_(name), position_(position) {}
   
public:
   
   static boost::shared_ptr<ParseNode> createRootNode()
   {
      return boost::shared_ptr<ParseNode>(
               new ParseNode(NULL, "<root>", Position(0, 0)));
   }
   
   static boost::shared_ptr<ParseNode> createNode(
         const std::string& name)
   {
      return boost::shared_ptr<ParseNode>(
               new ParseNode(NULL, name, Position(0, 0)));
   }
   
   bool isRootNode() const
   {
      return pParent_ == NULL;
   }
   
   const SymbolPositions& getDefinedSymbols() const
   {
      return definedSymbols_;
   }
   
   const SymbolPositions& getReferencedSymbols() const
   {
      return referencedSymbols_;
   }

   void addDefinedSymbol(int row,
                           int column,
                           const std::string& name)
   {
      DEBUG("--- Adding defined variable '" << name << "' (" << row << ", " << column << ")");
      definedSymbols_[name].push_back(Position(row, column));
   }

   void addDefinedSymbol(const AnnotatedRToken& rToken)
   {
      DEBUG("--- Adding defined variable '" << rToken.contentAsUtf8() << "'");
      definedSymbols_[rToken.contentAsUtf8()].push_back(
            Position(rToken.row(), rToken.column()));
   }
   
   void addReferencedSymbol(int row,
                              int column,
                              const std::string& name)
   {
      referencedSymbols_[name].push_back(Position(row, column));
   }

   void addReferencedSymbol(const AnnotatedRToken& rToken)
   {
      referencedSymbols_[rToken.contentAsUtf8()].push_back(
            Position(rToken.row(), rToken.column()));
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
   
   const std::vector< boost::shared_ptr<ParseNode> >& getChildren() const
   {
      return children_;
   }
   
   void addChild(boost::shared_ptr<ParseNode> pChild,
                 const Position& position)
   {
      pChild->pParent_ = this;
      pChild->position_ = position;
      children_.push_back(pChild);
   }
   
   void findAllUnresolvedSymbols(std::vector<ParseItem>* pItems) const
   {
      // Get the unresolved symbols at this node
      std::set<ParseItem> unresolved = getUnresolvedSymbols();
      pItems->insert(pItems->end(), unresolved.begin(), unresolved.end());
      
      // Apply this over all children on the node
      Children children = getChildren();
      
      for (Children::iterator it = children.begin();
           it != children.end();
           ++it)
      {
         (**it).findAllUnresolvedSymbols(pItems);
      }
   }
   
private:
   
   bool symbolExistsInSearchPath(const std::string& symbol,
                                 const std::set<std::string>& searchPathObjects) const
   {
      return searchPathObjects.count(symbol) != 0;
   }
   
   bool symbolHasDefinitionInTree(const std::string& symbol,
                                  const Position& position) const
   {
      if (definedSymbols_.count(symbol) != 0)
      {
         DEBUG("- Checking for symbol '" << symbol << "' in node");
         const Positions& positions = const_cast<ParseNode*>(this)->definedSymbols_[symbol];
         for (Positions::const_iterator it = positions.begin();
              it != positions.end();
              ++it)
         {
            // NOTE: '<=' because 'defined' variables are both referenced
            // and defined (at least for now?)
            if (*it <= position)
               return true;
         }
      }
      
      if (pParent_)
         return pParent_->symbolHasDefinitionInTree(symbol, position);
      
      return false;
   }
   
   std::set<ParseItem> getUnresolvedSymbols() const
   {
      std::set<ParseItem> unresolvedSymbols;
      
      for (SymbolPositions::const_iterator it = referencedSymbols_.begin();
           it != referencedSymbols_.end();
           ++it)
      {
         const std::string& symbol = it->first;
         BOOST_FOREACH(const Position& position, it->second)
         {
            DEBUG("-- Checking for symbol '" << symbol << "' " << position.toString());
            if (!symbolHasDefinitionInTree(symbol, position))
            {
               DEBUG("--- No definition for symbol '" << symbol << "'");
               unresolvedSymbols.insert(
                        ParseItem(symbol, position, this));
            }
            else
            {
               DEBUG("--- Found definition for symbol '" << symbol << "'");
            }
         }
      }

      return unresolvedSymbols;
   }
   
public:
   
   std::string suggestSimilarSymbolFor(const ParseItem& item) const
   {
      std::string nameLower = boost::algorithm::to_lower_copy(item.symbol);
      for (SymbolPositions::const_iterator it = definedSymbols_.begin();
           it != definedSymbols_.end();
           ++it)
      {
         DEBUG("-- '" << it->first << "'");
         if (nameLower == boost::algorithm::to_lower_copy(it->first))
         {
            BOOST_FOREACH(const Position& position, it->second)
            {
               if (position < item.position)
               {
                  return it->first;
               }
            }
         }
      }

      if (getParent())
         return getParent()->suggestSimilarSymbolFor(item);

      return std::string();
   }
   
private:
   
   // tree reference -- children and parent
   ParseNode* pParent_;
   
   typedef std::vector< boost::shared_ptr<ParseNode> > Children;
   Children children_;
   
   // member variables
   std::string name_; // name of scope (usually function name)
   Position position_; // location of opening '{' for scope
   
   // variables defined in this scope, e.g. with 'x <- ...'.
   // map variable names to locations (row, column)
   SymbolPositions definedSymbols_;
   
   // variables referenced in this scope
   // map variable names to locations(row, column)
   SymbolPositions referencedSymbols_;
   
   // for e.g. <pkg>::<foo>, we keep a cache of those symbols
   // in case we want to verify that e.g. <foo> really is an
   // exported function from <pkg>. TODO: keep position
   typedef std::string PackageName;
   typedef std::set<std::string> Symbols;
   typedef std::map<PackageName, Symbols> PackageSymbols;
   
   PackageSymbols internalSymbols_; // <pkg>::<foo>
   PackageSymbols exportedSymbols_; // <pgk>:::<bar>
};

namespace {

void addUnreferencedSymbol(const ParseItem& item,
                           LintItems* pLint)
{
   const ParseNode* pNode = item.pNode;
   if (!pNode)
      return;
   
   // Attempt to find a similarly named candidate in scope
   std::string candidate = pNode->suggestSimilarSymbolFor(item);
   pLint->addUnreferencedSymbol(item, candidate);
   
   // Check to see if there is a symbol in that node of
   // the parse tree (but defined later)
   ParseNode::SymbolPositions& symbols =
         const_cast<ParseNode::SymbolPositions&>(pNode->getDefinedSymbols());
   
   if (symbols.count(item.symbol))
   {
      ParseNode::Positions positions = symbols[item.symbol];
      BOOST_FOREACH(const Position& position, positions)
      {
         pLint->addSymbolDefinedAhead(item, position);
      }
   }
}

} // end anonymous namespace

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
   
   const Position currentPosition() const
   {
      const AnnotatedRToken& token = currentToken();
      return Position(token.row(), token.column());
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
   
   bool contentContains(wchar_t character) const
   {
      return rTokens_.at(offset_).contentContains(character);
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
   
   // Move over whitespace tokens (that do not contain newlines)
   bool fwdOverBlank()
   {
      while (currentToken().isType(RToken::WHITESPACE) &&
             !currentToken().contentContains(L'\n'))
         if (!moveToNextToken())
            return false;
      return true;
   }
   
   bool bwdOverBlank()
   {
      while (currentToken().isType(RToken::WHITESPACE) &&
             !currentToken().contentContains(L'\n'))
         if (!moveToPreviousToken())
            return false;
      return true;
   }
   
   bool finishesExpression()
   {
      bool isSemi = currentToken().isType(RToken::SEMI);
      bool hasNewline = currentToken().isType(RToken::WHITESPACE) &&
            currentToken().contentContains(L'\n');
      bool isFinalToken = offset_ == n_ - 1;
      
      return isSemi || hasNewline || isFinalToken;
   }
   
   // Move over a full R expression.
   //
   // The cursor is placed on the first token following the full
   // expression, e.g.
   //
   //    x + foo[[1]]()()$bar - -y; x
   //    ^~~~~~~~~~~~~~~~~~~~~~~~~~~^
   //
   // or, if no token exists after the end of the expression, we
   // set the cursor at the end of the document.
   bool moveFromStartToEndOfExpression(LintItems* pLintItems)
   {
      // We move a cloned cursor so that the current cursor stays
      // put on failure.
      TokenCursor cursor = clone();
      
      // Otherwise, we walk over pairs of 'symbols' and
      // binary operators.
      while (true)
      {
         UNARY_OPERATOR_OR_IDENTIFIER:
         
         if (isValidAsUnaryOperator(cursor))
         {
            if (!cursor.moveToNextSignificantToken())
            {
               if (pLintItems)
                  pLintItems->addUnexpectedEndOfDocument(cursor);
               return false;
            }
         }
         
         // symbol
         if (!isValidAsIdentifier(cursor))
         {
            pLintItems->addUnexpectedToken(cursor, "'identifier'");
            return false;
         }
         
         // An expression may end following an identifier,
         // and this can be a little tricky to figure out.
         // For example, we might have the line
         //
         //    a + b    ;
         //        ^----^
         //
         // so we need to:
         //
         //  1. Move over _blanks_ (not newlines! those can end an expression),
         //  2. Check to see if we finish the expression normally.
         if (!cursor.moveToNextToken())
         {
            if (pLintItems)
               pLintItems->addUnexpectedEndOfDocument(cursor);
            return false;
         }
         
         if (!cursor.fwdOverBlank())
         {
            if (pLintItems)
               pLintItems->addUnexpectedEndOfDocument(cursor);
            return false;
         }
         
         // An identifier in an expression is followed by either:
         //
         // 1. A binary operator (continuing the expression),
         // 2  An open brace (e.g. for function calls)
         // 3. A newline (ending the expression),
         // 4. A semicolon (ending the expression)
         if (cursor.finishesExpression())
         {
            cursor.moveToNextSignificantToken();
            offset_ = cursor.offset_;
            return true;
         }
         
         if (isLeftBrace(cursor))
            goto OPEN_BRACE;
         
         if (isBinaryOp(cursor))
            goto BINARY_OPERATOR;
         
         if (pLintItems)
            pLintItems->addUnexpectedToken(cursor);
         
         return false;
         
         OPEN_BRACE:
         
         while (cursor.fwdToMatchingToken())
         {
            if (cursor.finishesExpression())
            {
               cursor.moveToNextSignificantToken();
               offset_ = cursor.offset_;
               return true;
            }
            
            if (!cursor.moveToNextSignificantToken())
            {
               if (pLintItems)
                  pLintItems->addUnexpectedEndOfDocument();
               return false;
            }
            
            if (isBinaryOp(cursor))
               goto BINARY_OPERATOR;
         }
         
         if (pLintItems)
            pLintItems->addUnexpectedToken(cursor);
         
         return false;
         
         BINARY_OPERATOR:
         
         if (cursor.moveToNextSignificantToken())
         {
            if (isLeftBrace(cursor))
               goto OPEN_BRACE;
            
            goto UNARY_OPERATOR_OR_IDENTIFIER;
         }
         
         if (pLintItems)
            pLintItems->addUnexpectedToken(cursor);
         return false;
      }
   }
   
   bool moveFromStartToEndOfExpression()
   {
      return moveFromStartToEndOfExpression(NULL);
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

// Utility macros for inspecting and handling tokens
#define FWD_TO_MATCHING_TOKEN(__CURSOR__, __LINT_ITEMS__)                      \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.fwdToMatchingToken())                                    \
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
      if (isWhitespace(cursor.nextToken()))
         lintItems.addShouldRemoveWhitespace(cursor.nextToken());
      
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
      else if (nsToken.contentEquals(L":::"))
         pNode->addInternalPackageSymbol(
                  pkgToken.contentAsUtf8(),
                  exportedToken.contentAsUtf8());

      return;
   }
   
   // If the previous symbol is a '$', '@', or namespace symbol then we don't
   // touch it for now.
   //
   // Note that namespace symbols are handled (in one go) in an above block.
   //
   // TODO: Add another map for 'containers' in a scope?
   const AnnotatedRToken& prevToken = cursor.previousSignificantToken();
   if (isAt(prevToken) ||
       isDollar(prevToken) ||
       isNamespace(prevToken))
      return;

   // Check to see if there is a left assign following the token.
   // If so, add that symbol to this scope.
   const AnnotatedRToken& nextToken = cursor.nextSignificantToken();
   DEBUG("Next token: " << nextToken.asString());
   
   if (isLocalLeftAssign(nextToken))
   {
      // style: spaces around assignment tokens
      if (!isWhitespace(cursor.nextToken()))
         lintItems.addShouldUseWhitespace(cursor.nextToken());
      
      DEBUG("Adding defined variable '" << cursor.currentToken().contentAsUtf8() << "'");
      pNode->addDefinedSymbol(cursor);
   }

   // Similarily for previous assign
   if (isLocalRightAssign(prevToken))
   {
      // style: spaces around assignment tokens
      if (!isWhitespace(cursor.previousToken()))
         lintItems.addShouldUseWhitespace(cursor.previousToken());
      
      DEBUG("Adding defined variable '" << cursor.currentToken().contentAsUtf8() << "'");
      pNode->addDefinedSymbol(cursor);
   }
   
   // In a 'for (<var> in ...)', the 'in' is an implicit definition of a variable.
   if (nextToken.contentEquals(L"in"))
   {
      DEBUG("Adding 'for-in' defined variable: '" << cursor.currentToken().contentAsUtf8() << "'");
      pNode->addDefinedSymbol(cursor);
   }
   
   // TODO: Handle 'global' assign. This implies searching
   // parent scopes to see if the variable is defined locally
   // or defined in a parent scope. We likely want to warn if
   // a 'global' assign sees no variables in the parent scope.
   
   // TODO: Handle function tokens. Resolving these will require more work,
   // especially for e.g. packages (need to know all of the top level functions
   // available for that package; not to mention parsing of NAMESPACE imports
   // and exports...)
   //
   // For now, to avoid spurious warnings, we just ignore function tokens.
   if (cursor.nextSignificantToken().contentEquals(L"("))
   {
      // Move the cursor over functions that perform non-standard evaluation;
      // that is, functions that accept raw symbols as arguments.
      std::string content = cursor.currentToken().contentAsUtf8();
      
      if (std::find(s_nseFunctions.begin(),
                    s_nseFunctions.end(),
                    content) != s_nseFunctions.end())
      {
         DEBUG("- Skipping arguments within function that performs NSE");
         FWD_TO_MATCHING_TOKEN(cursor, lintItems);
      }
      
      return;
   }
   
   // Add a reference to this variable
   DEBUG("Adding reference to variable '" << cursor.currentToken().contentAsUtf8() << "'");
   pNode->addReferencedSymbol(cursor);
   
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
   
   // style: no spaces between `function` and `(`
   if (isWhitespace(cursor))
      lintItems.addShouldRemoveWhitespace(cursor);
   FWD_OVER_WHITESPACE(cursor, lintItems);
   
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
   
   // Warn on whitespace for e.g.
   //
   //     foo <- function(  a,
   //                     ^^
   //
   if (isWhitespace(cursor))
      lintItems.addShouldRemoveWhitespace(cursor);
   FWD_OVER_WHITESPACE(cursor, lintItems);
   
   // Start running through the function arguments. This loop should
   // begin at the start of a new function block; ie, on an identifier.
   // We break out when a ')' or other unmatched closing brace is identified.
   DEBUG("Parsing argument list for function");
   do
   {
      START:
      
      FWD_OVER_WHITESPACE(cursor, lintItems);
      DEBUG("Current token: " << cursor.currentToken().asString());
      
      // we might have moved on to a comma, e.g. for an 'empty' argument
      if (isComma(cursor))
         continue;
      
      // start on identifier
      if (!isId(cursor))
         lintItems.addUnexpectedToken(cursor, "id");
      else
      {
         DEBUG("Adding definition: '" << cursor.currentToken().contentAsUtf8() << "'");
         pChild->addDefinedSymbol(cursor);
      }
      
      MOVE_TO_NEXT_TOKEN(cursor, lintItems);
      FWD_OVER_WHITESPACE(cursor, lintItems);
      
      // expect either a ',' or '=' (for default arg)
      if (isComma(cursor))
      {
         // style: should follow ',' with whitespace
         if (!isWhitespace(cursor.nextToken()))
            lintItems.addShouldUseWhitespace(cursor.nextToken());
         
         DEBUG("Found comma; parsing next argument");
         continue;
      }
      else if (cursor.contentEquals(L"="))
      {
         // warn if no whitespace before '='
         if (!isWhitespace(cursor.previousToken()))
            lintItems.addShouldUseWhitespace(cursor.previousToken());
         
         // warn if no whitespace after '='
         if (!isWhitespace(cursor.nextToken()))
            lintItems.addShouldUseWhitespace(cursor.nextToken());
         
         // if the next significant token is a 'function' keyword,
         // we need to recurse
         if (cursor.nextSignificantToken().contentEquals(L"function"))
         {
            DEBUG("-- Encountered function token -- recursing!");
            
            // Move on to the 'function' token.
            cursor.moveToNextSignificantToken();
            
            // Clone a cursor and recurse.
            TokenCursor clone = cursor.clone();
            handleFunctionToken(clone, ppNode, lintItems);
            
            // There should be a '(' following the function token.
            if (!cursor.nextSignificantToken().contentEquals(L"("))
            {
               lintItems.addUnexpectedToken(cursor.nextSignificantToken(), L"(");
               return;
            }
            
            // There shouldn't be whitespace following the 'function' token.
            if (isWhitespace(cursor.nextToken()))
               lintItems.addShouldRemoveWhitespace(cursor.nextToken());
            
            // We want to move the token cursor onto the next argument in the list,
            // or hit the end of the argument list.
            
            // Move on to the '('
            if (!cursor.moveToNextSignificantToken())
               return;
            
            // Move to matching ')' (end of this function's argument list)
            if (!cursor.fwdToMatchingToken())
               return;
            
            // For this function, we either expect a '{' body, or a single expression.
            // Move over that.
            
            // Suggest whitespace following.
            if (!isWhitespace(cursor.nextToken()))
               lintItems.addShouldUseWhitespace(cursor.nextToken());
            
            // Move over the expression.
            DEBUG("-- About to move over expression...");
            DEBUG("-- Cursor before: " + cursor.currentToken().asString());
            if (!cursor.moveFromStartToEndOfExpression(&lintItems))
            {
               DEBUG("--- FAILED: Couldn't move to end of expression!");
               return;
            }
            
            DEBUG("-- Cursor after: " + cursor.currentToken().asString());
            if (!cursor.moveToNextSignificantToken())
               return;
            
            // We should now be on either a ',' or a ')' closing the argument list.
            if (cursor.contentEquals(L","))
               continue;
            else if (cursor.contentEquals(L")"))
               goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
            else
            {
               lintItems.addUnexpectedToken(cursor, "',' or ')'");
               return;
            }
         }
         
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
   
   // style: prefer space between e.g.
   //
   //    foo <- function() { ... }
   //                     ^
   //
   if (!isWhitespace(cursor))
      lintItems.addShouldUseWhitespace(cursor);
   
   FWD_OVER_WHITESPACE(cursor, lintItems);
   
   if (cursor.contentEquals(L"{"))
   {
      DEBUG("Found '{' opening function body");
      pNode->addChild(pChild, cursor.currentPosition());
      *ppNode = pChild.get();
   }
   
}

void handleStringToken(TokenCursor& cursor,
                       ParseNode* pNode,
                       LintItems& lintItems)
{
   if (isLocalLeftAssign(cursor.nextSignificantToken()))
      pNode->addDefinedSymbol(cursor);
}

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
   std::size_t maxErrors = 5;
   
   std::string contents = file_utils::readFile(filePath);
   if (contents.empty() || contents.find_first_not_of(" \n\t\v") == std::string::npos)
      return LintResults();
   
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
      
      // bail if too many errors
      if (lintItems.errorCount() > maxErrors)
      {
         lintItems.addTooManyErrors(cursor.currentPosition());
         break;
      }
      
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
         
         // If the brace stack (the stack of non-closure-associated '{') is empty,
         // then we must be closing a function body.
         if (braceStack.empty())
         {
            if (cursor.contentEquals(L"}") && pNode->getParent() != NULL)

            {
               DEBUG("Encountered '}' closing function block");
               pNode = pNode->getParent();
               continue;
            }
            else
            {
               DEBUG("Unexpected token " << cursor.currentToken().asString());
               lintItems.addUnexpectedToken(cursor);
               continue;
            }
         }
         
         // Otherwise, we are closing an expression block.
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
         
         // TODO: Nodes should also have a 'local' brace stack, which keeps track
         // of 'if/else' blocks. With this, we could warn on code like:
         //
         //    if (foo) {
         //       x <- 1
         //    }
         //
         // if the variable 'x' does not exist previously in that scope -- this
         // implies conditionally creating a variable 'x', which is normally a
         // code smell.
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
   std::vector<ParseItem> unresolvedItems;
   root->findAllUnresolvedSymbols(&unresolvedItems);
   
   // Finally, prune the set of lintItems -- we exclude any symbols
   // that were on the search path.
   BOOST_FOREACH(const ParseItem& item, unresolvedItems)
   {
      if (objects.count(item.symbol) == 0)
      {
         addUnreferencedSymbol(item, &lintItems);
      }
   }
   
   return std::make_pair(root, lintItems);
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
