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

struct ParseItem;
typedef std::vector<ParseItem> BraceStack;

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
   
   LintItem(const ParseItem& item,
            LintType type,
            const std::string& message)
      : startRow(item.position.row),
        startColumn(item.position.column),
        endRow(item.position.row),
        endColumn(item.position.column + item.symbol.length()),
        type(type),
        message(message) {}

   int startRow;
   int startColumn;
   int endRow;
   int endColumn;
   LintType type;
   std::string message;
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
   
   void unexpectedToken(const AnnotatedRToken& token,
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
   
   void unexpectedToken(const AnnotatedRToken& token,
                           const std::wstring& expected)
   {
      unexpectedToken(token, string_utils::wideToUtf8(expected));
   }
   
   void unexpectedClosingBracket(const AnnotatedRToken& token,
                                 const BraceStack& braceStack)
   {
      std::string content = token.contentAsUtf8();
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + content.length(),
                    LintTypeError,
                    "unexpected closing bracket '" + content + "'");
      
      lintItems_.push_back(lint);
      ++errorCount_;
      
      if (!braceStack.empty())
      {
         const ParseItem& topOfStack = braceStack[braceStack.size() - 1];
         LintItem info(topOfStack,
                       LintTypeInfo,
                       "unmatched bracket '" + topOfStack.symbol + "' here");
         lintItems_.push_back(info);
      }
   }
   
   void unexpectedEndOfDocument(const AnnotatedRToken& token)
   {
      LintItem lint(token.row(),
                    token.column() + token.contentAsUtf8().length(),
                    token.row(),
                    token.column() + token.contentAsUtf8().length(),
                    LintTypeError,
                    "unexpected end of document");
      lintItems_.push_back(lint);
   }
   
   void noSymbolNamed(const ParseItem& item,
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
   
   void symbolDefinedAfterUsage(const ParseItem& item,
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
   
   void expectedWhitespace(const AnnotatedRToken& token)
   {
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column(),
                    LintTypeStyle,
                    "expected whitespace");
      lintItems_.push_back(lint);
   }
   
   void unnecessaryWhitespace(const AnnotatedRToken& token)
   {
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column(),
                    LintTypeStyle,
                    "unnecessary whitespace");
      lintItems_.push_back(lint);
   }
   
   void tooManyErrors(const Position& position)
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
   pLint->noSymbolNamed(item, candidate);
   
   // Check to see if there is a symbol in that node of
   // the parse tree (but defined later)
   ParseNode::SymbolPositions& symbols =
         const_cast<ParseNode::SymbolPositions&>(pNode->getDefinedSymbols());
   
   if (symbols.count(item.symbol))
   {
      ParseNode::Positions positions = symbols[item.symbol];
      BOOST_FOREACH(const Position& position, positions)
      {
         pLint->symbolDefinedAfterUsage(item, position);
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
   
   operator ParseItem() const
   {
      return ParseItem(contentAsUtf8(),
                       currentPosition(),
                       NULL);
   }
   
   std::string contentAsUtf8() const
   {
      return currentToken().contentAsUtf8();
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
   
   bool isType(wchar_t type)
   {
      return rTokens_.at(offset_).isType(type);
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
      const AnnotatedRToken& token = currentToken();
      bool isSemi = token.isType(RToken::SEMI);
      bool isComma = token.isType(RToken::COMMA);
      bool hasNewline = token.isType(RToken::WHITESPACE) &&
            token.contentContains(L'\n');
      bool isRightParen = isRightBrace(token);
      bool isFinalToken = offset_ == n_ - 1;

      return isSemi || hasNewline || isFinalToken || isRightParen || isComma;
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
   bool moveFromStartToEndOfExpression(ParseNode* pNode,
                                       LintItems* pLintItems,
                                       BraceStack* pBraceStack)
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
                  pLintItems->unexpectedEndOfDocument(cursor);
               return false;
            }
         }
         
         // symbol
         if (!isValidAsIdentifier(cursor))
         {
            if (pLintItems)
               pLintItems->unexpectedToken(cursor, "'identifier'");
            
            return false;
         }
         
         if (pNode && cursor.isType(RToken::ID))
         {
            if (!isLeftAssign(cursor.nextSignificantToken()) &&
                !cursor.contentEquals(L"(")) // TODO: Allow functions
               pNode->addReferencedSymbol(cursor);
            
            if (cursor.nextSignificantToken().contentEquals(L"<-"))
               pNode->addDefinedSymbol(cursor);
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
               pLintItems->unexpectedEndOfDocument(cursor);
            return false;
         }
         
         if (!cursor.fwdOverBlank())
         {
            if (pLintItems)
               pLintItems->unexpectedEndOfDocument(cursor);
            return false;
         }
         
         // An identifier in an expression is followed by either:
         //
         // 1. A binary operator (continuing the expression),
         // 2  An open brace (e.g. for function calls)
         // 3. A newline (ending the expression),
         // 4. A semicolon (ending the expression)
         // 5. A closing brace (closing an enclosing expression,
         //    and implicitly (!) closing this one)
         if (cursor.finishesExpression())
         {
            if (pBraceStack && isRightBrace(cursor))
               pBraceStack->pop_back();
            else
               cursor.moveToNextSignificantToken();
            
            offset_ = cursor.offset_;
            return true;
         }
         
         if (isLeftBrace(cursor))
            goto OPEN_BRACE;
         
         if (isBinaryOp(cursor))
            goto BINARY_OPERATOR;
         
         if (pLintItems)
            pLintItems->unexpectedToken(cursor);
         
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
                  pLintItems->unexpectedEndOfDocument(cursor);
               return false;
            }
            
            if (isBinaryOp(cursor))
               goto BINARY_OPERATOR;
         }
         
         if (pLintItems)
            pLintItems->unexpectedToken(cursor);
         
         return false;
         
         BINARY_OPERATOR:
         
         if (cursor.moveToNextSignificantToken())
         {
            if (isLeftBrace(cursor))
               goto OPEN_BRACE;
            
            goto UNARY_OPERATOR_OR_IDENTIFIER;
         }
         
         if (pLintItems)
            pLintItems->unexpectedToken(cursor);
         return false;
      }
   }
   
   bool moveFromStartToEndOfExpression()
   {
      return moveFromStartToEndOfExpression(NULL, NULL, NULL);
   }
   
   friend std::ostream& operator <<(std::ostream& os,
                                    const TokenCursor& cursor)
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

// Utility macros for moving the cursor and logging lint items on failure
#define RS_LINT_COMMAND(__CURSOR__, __LINT_ITEMS__, __FUNCTION__)              \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.__FUNCTION__())                                          \
      {                                                                        \
         DEBUG_BLOCK                                                           \
         {                                                                     \
            DEBUG("* Encountered unexpected end of document");                 \
            std::string file = std::string(__FILE__);                          \
            std::string shortFileName = file.substr(file.rfind('/') + 1);      \
            DEBUG("*** (" << shortFileName << ":" << __LINE__ << ")");         \
         }                                                                     \
         __LINT_ITEMS__.unexpectedEndOfDocument(__CURSOR__);                   \
         return;                                                               \
      }                                                                        \
   } while (0)

#define MOVE_TO_NEXT_TOKEN(__CURSOR__, __LINT_ITEMS__)                         \
   RS_LINT_COMMAND(__CURSOR__, __LINT_ITEMS__, moveToNextToken)

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN(__CURSOR__, __LINT_ITEMS__)             \
   RS_LINT_COMMAND(__CURSOR__, __LINT_ITEMS__, moveToNextSignificantToken)

#define FWD_TO_MATCHING_TOKEN(__CURSOR__, __LINT_ITEMS__)                      \
   RS_LINT_COMMAND(__CURSOR__, __LINT_ITEMS__, fwdToMatchingToken)

#define FWD_OVER_WHITESPACE(__CURSOR__, __LINT_ITEMS__)                        \
   RS_LINT_COMMAND(__CURSOR__, __LINT_ITEMS__, fwdOverWhitespace)

#define FWD_OVER_BLANK(__CURSOR__, __LINT_ITEMS__)                             \
   RS_LINT_COMMAND(__CURSOR__, __LINT_ITEMS__, fwdOverBlank)

void handleIdToken(TokenCursor& cursor,
                   ParseNode* pNode,
                   LintItems& lintItems)
{
   // If the following token is a '::' or ':::', then
   // we add to the set of namespace entries used.
   if (isNamespace(cursor.nextSignificantToken()))
   {
      if (isWhitespace(cursor.nextToken()))
         lintItems.unnecessaryWhitespace(cursor.nextToken());
      
      // Validate that the entries before and after the '::' are either
      // strings, symbols or identifiers
      const AnnotatedRToken& pkgToken = cursor;
      const AnnotatedRToken& nsToken = cursor.nextSignificantToken();
      const AnnotatedRToken& exportedToken = cursor.nextSignificantToken(2);
      
      if (!(isString(pkgToken) ||
            isId(pkgToken)))
      {
         lintItems.unexpectedToken(pkgToken, "'string' or 'id'");
         return;
      }
      
      if (!(isString(exportedToken) ||
            isId(exportedToken)))
      {
         lintItems.unexpectedToken(exportedToken, "'string' or 'id'");
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
         lintItems.expectedWhitespace(cursor.nextToken());
      
      DEBUG("Adding defined variable '" << cursor.currentToken().contentAsUtf8() << "'");
      pNode->addDefinedSymbol(cursor);
   }

   // Similarily for previous assign
   if (isLocalRightAssign(prevToken))
   {
      // style: spaces around assignment tokens
      if (!isWhitespace(cursor.previousToken()))
         lintItems.expectedWhitespace(cursor.previousToken());
      
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
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, lintItems);
         FWD_TO_MATCHING_TOKEN(cursor, lintItems);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, lintItems);
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
                         LintItems& lintItems,
                         BraceStack& braceStack)
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
      lintItems.unnecessaryWhitespace(cursor);
   FWD_OVER_WHITESPACE(cursor, lintItems);
   
   if (!cursor.contentEquals(L"("))
   {
      lintItems.unexpectedToken(
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
      lintItems.unnecessaryWhitespace(cursor);
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
         lintItems.unexpectedToken(cursor, "id");
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
            lintItems.expectedWhitespace(cursor.nextToken());
         
         DEBUG("Found comma; parsing next argument");
         continue;
      }
      else if (cursor.contentEquals(L"="))
      {
         DEBUG_BLOCK
         {
            std::cerr << "-- CURSOR: " << cursor << "--" << std::endl;
         }
         // warn if no whitespace before '='
         if (!isWhitespace(cursor.previousToken()))
            lintItems.expectedWhitespace(cursor.previousToken());
         
         // warn if no whitespace after '='
         if (!isWhitespace(cursor.nextToken()))
            lintItems.expectedWhitespace(cursor.nextToken());
         
         // if the next significant token is a 'function' keyword,
         // we need to recurse
         if (cursor.nextSignificantToken().contentEquals(L"function"))
         {
            DEBUG("-- Encountered function token -- recursing!");
            
            // Move on to the 'function' token.
            cursor.moveToNextSignificantToken();
            
            // Clone a cursor and recurse.
            TokenCursor clone = cursor.clone();
            handleFunctionToken(clone, ppNode, lintItems, braceStack);
            
            // There should be a '(' following the function token.
            if (!cursor.nextSignificantToken().contentEquals(L"("))
            {
               lintItems.unexpectedToken(cursor.nextSignificantToken(), L"(");
               return;
            }
            
            // There shouldn't be whitespace following the 'function' token.
            if (isWhitespace(cursor.nextToken()))
               lintItems.unnecessaryWhitespace(cursor.nextToken());
            
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
               lintItems.expectedWhitespace(cursor.nextToken());
            
            MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, lintItems);
            
            // Move over the expression.
            DEBUG("-- About to move over expression...");
            DEBUG("-- Cursor before: " + cursor.currentToken().asString());
            if (!cursor.moveFromStartToEndOfExpression())
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
               lintItems.unexpectedToken(cursor, "',' or ')'");
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
                  lintItems.unexpectedClosingBracket(cursor, braceStack);
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
                  lintItems.unexpectedToken(cursor, ")");
               
               goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
            }
         }
      }
      else if (isRightBrace(cursor))
      {
         if (!cursor.contentEquals(L")"))
            lintItems.unexpectedToken(cursor, ")");
         
         goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
      }
      {
         DEBUG("Unexpected token: expected ',', '=' or ')' following identifier");
         lintItems.unexpectedToken(cursor, "',' or '='");
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
      lintItems.expectedWhitespace(cursor);
   
   FWD_OVER_WHITESPACE(cursor, lintItems);
   pNode->addChild(pChild, cursor.currentPosition());
   *ppNode = pChild.get();
   
   if (!cursor.contentEquals(L"{"))
   {
      DEBUG("*** Before moving over expression: " << cursor);
      cursor.moveFromStartToEndOfExpression(*ppNode, &lintItems, &braceStack);
      DEBUG("*** After moving over expression: " << cursor);
      *ppNode = pChild->getParent();
   }
   
}

void handleStringToken(TokenCursor& cursor,
                       ParseNode* pNode,
                       LintItems& lintItems)
{
   if (isLocalLeftAssign(cursor.nextSignificantToken()))
      pNode->addDefinedSymbol(cursor);
}

std::string complement(const std::string& string)
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
   BraceStack braceStack;
   
   std::size_t braceCount = 0;
   
   TokenCursor cursor(rTokens);
   
   do
   {
      DEBUG("Current token: " << cursor);
      
      // bail if too many errors
      if (lintItems.errorCount() > maxErrors)
      {
         lintItems.tooManyErrors(cursor.currentPosition());
         break;
      }
      
      // Update the brace stack
      if (isLeftBrace(cursor))
      {
         DEBUG("* Encountered '" << cursor << "'; adding to brace stack");
         braceStack.push_back(cursor);
         braceCount += cursor.currentToken().contentEquals(L"{") ? 1 : 0;
         continue;
      }
      
      // If we encounter a right brace, check to see if
      // its partner is on the token stack; if not, parse error
      if (isRightBrace(cursor))
      {
         bool isRightBrace = cursor.contentEquals(L"}");
         
         // If the brace stack is empty...
         if (braceStack.empty())
         {
            // ... but the cursor is a right brace, and there is
            // a function scope to close, then close it
            if (isRightBrace && pNode->getParent() != NULL)
            {
               DEBUG("* Encounterd '" << cursor << "'; closing function body");
               pNode = pNode->getParent();
               continue;
            }
            lintItems.unexpectedClosingBracket(cursor, braceStack);
         }
         
         // If the brace stack is not empty...
         else
         {
            const ParseItem& topOfStack =
                  braceStack[braceStack.size() - 1];

            // ... and the cursor is on a '}'
            if (isRightBrace)
            {
               // ... and the expression stack has a '{',
               // then close the expression
               if (topOfStack.symbol == "{")
               {
                  DEBUG("* Encountered '" << cursor << "'; closing expression body");
                  braceStack.pop_back();
                  continue;
               }
               
               // ... otherwise, this must be closing a
               // function body
               else if (pNode->getParent() != NULL)
               {
                  DEBUG("* Encountered '" << cursor << "'; closing function body");
                  pNode = pNode->getParent();
                  continue;
               }
               
               // ... otherwise, there was no expression or
               // function body to close
               else
               {
                  lintItems.unexpectedClosingBracket(cursor, braceStack);
               }
            }
            
            // ... just pop the brace stack
            else
            {
               if (complement(cursor.contentAsUtf8()) != topOfStack.symbol)
                  lintItems.unexpectedClosingBracket(cursor, braceStack);
               else
                  braceStack.pop_back();
                  
               continue;
            }
         }
         
      }
         
      // Handle a 'function' token.
      if (isFunction(cursor))
      {
         DEBUG("*** Handling function token");
         handleFunctionToken(cursor, &pNode, lintItems, braceStack);
         continue;
      }
      
      // Handle an identifier (implies we should check to see if it has a reference
      // in the parse tree)
      if (isId(cursor))
      {
         DEBUG("*** Handling ID token");
         
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
         DEBUG("*** Handling string token");
         handleStringToken(cursor, pNode, lintItems);
         continue;
      }
      
   } while (cursor.moveToNextToken());
   
   DEBUG("- Finished building parse tree");
   if (!braceStack.empty())
   {
      DEBUG("Unexpected end of document; unclosed brace(s)");
      lintItems.unexpectedEndOfDocument(cursor.currentToken());
      
      DEBUG_BLOCK
      {
         std::cerr << "Brace stack:" << std::endl;
         for (std::size_t i = 0; i < braceStack.size(); ++i)
         {
            std::cerr << "-- "
                      << braceStack[i].position.row
                      << ", "
                      << braceStack[i].position.column
                      << ", "
                      << "'" << braceStack[i].symbol << "'"
                      << std::endl;
         }
      }
   }
   
   // TODO: Should we report missing matches? Probably need a way
   // of specifying a 'full' lint vs. 'partial' lint; ie,
   // to differentiate between 'does it look okay so far' and
   // 'is this entire document okay'.
   if (pNode->getParent() != NULL)
   {
      DEBUG("Unexpected end of document; unclosed function brace");
      lintItems.unexpectedEndOfDocument(cursor.currentToken());
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
