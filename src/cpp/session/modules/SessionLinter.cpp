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

std::string complement(const std::string& string)
{
   return s_complements[string];
}

} // anonymous namespace

struct ParseItem;
typedef Stack<ParseItem> BraceStack;

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
   
   char type() const { return token_.type(); }
   bool isType(char type) const { return token_.isType(type); }
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
                                 BraceStack* pBraceStack)
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
      
      if (!pBraceStack->empty())
      {
         const ParseItem& topOfStack = pBraceStack->peek();
         
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

// The ParseStatus contains:
//
// 1. The parse tree,
// 2. The current active node,
// 3. The brace stack,
// 4. The lint items collected while parsing.
class ParseStatus {
   
public:
   
   ParseStatus()
      : pRoot_(ParseNode::createRootNode())
   {
      *ppNode_ = pRoot_.get();
   }
   
   ParseNode* node() { return *ppNode_; }
   BraceStack& stack() { return stack_; }
   LintItems& lint() { return lint_; }
   
   void addChildAndSetAsCurrentNode(const boost::shared_ptr<ParseNode>& pChild,
                                    const Position& position)
   {
      node()->addChild(pChild, position);
      *ppNode_ = pChild.get();
   }
   
   void setParentAsCurrent()
   {
      *ppNode_ = (*ppNode_)->getParent();
   }
   
private:
   boost::shared_ptr<ParseNode> pRoot_;
   ParseNode** ppNode_;
   BraceStack stack_;
   LintItems lint_;
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
   
   const AnnotatedRTokens& tokens() const
   {
      return rTokens_;
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
      return currentToken().contentEquals(content);
   }
   
   char type() const
   {
      return currentToken().type();
   }
   
   bool isType(char type) const
   {
      return currentToken().isType(type);
   }

   bool contentContains(wchar_t character) const
   {
      return currentToken().contentContains(character);
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
   
   friend std::ostream& operator <<(std::ostream& os,
                                    const TokenCursor& cursor)
   {
      os << cursor.currentToken().asString();
      return os;
   }
   
private:
   
   bool doFwdToMatchingToken(char leftTokenType,
                             char rightTokenType)
   {
      if (!isType(leftTokenType))
         return false;
      
      TokenCursor cursor = clone();
      int stack = 1;
      
      while (cursor.moveToNextToken())
      {
         stack += cursor.isType(leftTokenType);
         stack -= cursor.isType(rightTokenType);
         
         if (stack == 0)
         {
            offset_ = cursor.offset_;
            return true;
         }
      }
      
      return false;
   }
   
   bool doBwdToMatchingToken(char leftTokenType,
                             char rightTokenType)
   {
      if (!isType(rightTokenType))
         return false;
      
      TokenCursor cursor = clone();
      int stack = 1;
      
      while (cursor.moveToPreviousToken())
      {
         stack += cursor.isType(rightTokenType) ? 1 : 0;
         stack -= cursor.isType(leftTokenType) ? 1 : 0;
         
         if (stack == 0)
         {
            offset_ = cursor.offset_;
            return true;
         }
      }
      return false;
   }
   
   static std::map<char, char> makeComplementMap()
   {
      std::map<char, char> map;

#define RSTUDIO_ADD_COMPLEMENT_2(__MAP__, __X__, __Y__)                        \
   do                                                                          \
   {                                                                           \
      __MAP__[__X__] = __Y__;                                                  \
      __MAP__[__Y__] = __X__;                                                  \
   } while (0)

#define RSTUDIO_ADD_COMPLEMENT(__MAP__, __BRACKET__)                                    \
   RSTUDIO_ADD_COMPLEMENT_2(__MAP__, RToken::L##__BRACKET__, RToken::R##__BRACKET__)

      RSTUDIO_ADD_COMPLEMENT(map, PAREN);
      RSTUDIO_ADD_COMPLEMENT(map, BRACKET);
      RSTUDIO_ADD_COMPLEMENT(map, BRACE);
      RSTUDIO_ADD_COMPLEMENT(map, DBRACKET);

#undef RSTUDIO_ADD_COMPLEMENT_2
#undef RSTUDIO_ADD_COMPLEMENT

      return map;
   }
   
   static std::map<char, char> complements()
   {
      static std::map<char, char> map = makeComplementMap();
      return map;
   }

public:
  bool fwdToMatchingToken()
  {
     return doFwdToMatchingToken(type(),
                                 complements()[type()]);
  }

  bool bwdToMatchingToken()
  {
     return doBwdToMatchingToken(type(),
                                 complements()[type()]);
  }

private:
   
   const AnnotatedRTokens& rTokens_;
   std::size_t offset_;
   std::size_t n_;
};

namespace {

std::wstring typeToString(char type)
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

#undef RS_TYPE_TO_STRING_CASE

#define RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, __ACTION__)                \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.__ACTION__())                                            \
      {                                                                        \
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

#define FWD_OVER_BLANK(__CURSOR__, __STATUS__)                                 \
   RSTUDIO_LINT_ACTION(__CURSOR__, __STATUS__, fwdOverBlank);

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(__CURSOR__,          \
                                                          __STATUS__)          \
   do                                                                          \
   {                                                                           \
      MOVE_TO_NEXT_TOKEN(__CURSOR__, __STATUS__);                              \
      if (isWhitespace(__CURSOR__))                                            \
         __STATUS__.lint().unnecessaryWhitespace(__CURSOR__);                  \
      FWD_OVER_WHITESPACE(__CURSOR__, __STATUS__);                             \
   } while (0)

#define MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(__CURSOR__,       \
                                                             __STATUS__)       \
   do                                                                          \
   {                                                                           \
      MOVE_TO_NEXT_TOKEN(__CURSOR__, __STATUS__);                              \
      if (!isWhitespace(__CURSOR__))                                           \
         __STATUS__.lint().unnecessaryWhitespace(__CURSOR__);                  \
      FWD_OVER_WHITESPACE(__CURSOR__, __STATUS__);                             \
   } while (0)

#define ENSURE_CONTENT(__CURSOR__, __STATUS__, __CONTENT__)                    \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.contentEquals(__CONTENT__))                              \
      {                                                                        \
         __STATUS__.lint().unexpectedToken(__CURSOR__, __CONTENT__);           \
         return;                                                               \
      }                                                                        \
   } while (0)

#define ENSURE_TYPE(__CURSOR__, __STATUS__, __TYPE__)                          \
   do                                                                          \
   {                                                                           \
      if (!__CURSOR__.isType(__TYPE__))                                        \
      {                                                                        \
         __STATUS__.lint().unexpectedToken(__CURSOR__,                         \
                                           typeToString(__TYPE__));            \
         return;                                                               \
      }                                                                        \
   } while (0)

// We define a number of 'handlers' that take a token cursor,
// and a parse status, and handle different stages of the parse
// process.
//
// All 'handler's take their items by non-const reference just
// for brevity; because this pattern is common throughout the
// handlers it shouldn't be too surprising that the handlers
// may modify the objects passed in.

// Handle a statement or an expression. An expression must start
// with a '{' (with the exception being the whole document).
void beginParse(TokenCursor& cursor, ParseStatus& status);
void handleControlFlow(TokenCursor& cursor, ParseStatus& status);
void handleStatementOrExpression(TokenCursor& cursor, ParseStatus& status);
void handleStatement(TokenCursor& cursor, ParseStatus& status);
void handleExpression(TokenCursor& cursor, ParseStatus& status);
void handleFor(TokenCursor& cursor, ParseStatus& status);
void handleWhile(TokenCursor& cursor, ParseStatus& status);
void handleIf(TokenCursor& cursor, ParseStatus& status);
void handleArgument(TokenCursor& cursor, ParseStatus& status);
void handleArgumentList(TokenCursor& cursor, ParseStatus& status, bool cacheIdentifiers);
void handleFunction(TokenCursor& cursor, ParseStatus& status);
void handleRepeat(TokenCursor& cursor, ParseStatus& status);
void handleIdentifier(TokenCursor& cursor, ParseStatus& status);

void beginParse(TokenCursor& cursor,
                ParseStatus& status)
{
   handleControlFlow(cursor, status);
   handleStatementOrExpression(cursor, status);
   
   // If we have no handlers, move to the next token and
   // repeat (just to ensure that we always progress)
   MOVE_TO_NEXT_TOKEN(cursor, status);
   beginParse(cursor, status);
}

void handleControlFlow(TokenCursor& cursor,
                       ParseStatus& status)
{
   if (cursor.contentEquals(L"if"))
      handleIf(cursor, status);
   else if (cursor.contentEquals(L"for"))
      handleFor(cursor, status);
   else if (cursor.contentEquals(L"while"))
      handleWhile(cursor, status);
   else if (cursor.contentEquals(L"repeat"))
      handleRepeat(cursor, status);
   else if (cursor.contentEquals(L"function"))
      handleFunction(cursor, status);
}

// A 'for-in' loop is of the form:
//
//    for (<id> in <statement-or-expression>) <statement-or-expression>
//
void handleFor(TokenCursor& cursor,
               ParseStatus& status)
{
   ENSURE_CONTENT(cursor, status, L"for");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   handleIdentifier(cursor, status);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_CONTENT(cursor, status, L"in");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   beginParse(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   beginParse(cursor, status);
}

// A 'while' loop is of the form:
//
//    while (<statment-or-expression>) <statement-or-expression>
//
void handleWhile(TokenCursor& cursor,
                 ParseStatus& status)
{
   ENSURE_CONTENT(cursor, status, L"while");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   beginParse(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   beginParse(cursor, status);
}

// Handle an 'if-else' statement, e.g.
//
//    if (<statement-or-expression) <statement-or-expression>
//    
// which is followed by optional 'else-if', or final 'else'.
void handleIf(TokenCursor& cursor,
              ParseStatus& status)
{
   ENSURE_CONTENT(cursor, status, L"if");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   beginParse(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::RPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   beginParse(cursor, status);
   
   // After parsing the 'if () ...' block, we may see either a:
   //
   //     1. else if (...)
   //     2. else ...
   //
   if (cursor.contentEquals(L"else"))
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      if (cursor.contentEquals(L"if"))
         handleIf(cursor, status);
      else
         beginParse(cursor, status);
   }
}

// Repeat is simply
//
//    repeat <statement-or-expression>
void handleRepeat(TokenCursor& cursor,
                  ParseStatus& status)
{
   ENSURE_CONTENT(cursor, status, L"repeat");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
}

// An expression is a collection of statements, within
// a '{}' block. Note that we separate the notion of an
// expression from that of a statement; that is, an
// expression is a collection of statements, separated
// by newlines and semicolons.
void handleExpression(TokenCursor& cursor,
                      ParseStatus& status)
{
   if (!cursor.isType(RToken::LBRACE))
      status.lint().unexpectedToken(cursor, L"{");
   
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   
   while (!cursor.isType(RToken::RBRACE))
      handleStatement(cursor, status);
}

void handleStatement(TokenCursor& cursor,
                     ParseStatus& status)
{
   // Check for control flow work.
   handleControlFlow(cursor, status);
   
   // Move over a unary operator
   if (isValidAsUnaryOperator(cursor))
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   
   // We should now see a symbol (or string).
   if (!isValidAsIdentifier(cursor))
   {
      status.lint().unexpectedToken(cursor, "'symbol'");
      return;
   }
   
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
   MOVE_TO_NEXT_TOKEN(cursor, status);
   FWD_OVER_BLANK(cursor, status);
   
   if (cursor.isType(RToken::SEMI) ||
       cursor.contentContains(L'\n')) // note: handles '\r\n' too
   {
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      return beginParse(cursor, status);
   }
   
   // If we encounter a '(' or '[', we want to
   // handle pairs -- these are argument lists.
   //
   // Note that we do not return from here -- the
   // next token may be a binary operator, and so we
   // want to continue parsing as part of this statement.
   while (isLeftBrace(cursor))
   {
      if (cursor.isType(RToken::LPAREN) ||
          cursor.isType(RToken::LBRACKET) ||
          cursor.isType(RToken::LDBRACKET))
      {
         handleArgumentList(cursor, status);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
      
      if (cursor.isType(RToken::LBRACE))
      {
         handleExpression(cursor, status);
         MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
      }
   }
   
   // If we hit a comma, or a closing bracket, then this might
   // be closing an argument list, thereby implicitly ending this
   // statement. In such a case, it is the responsibility of the caller
   // to move the cursor.
   if (cursor.isType(RToken::COMMA) ||
       isRightBrace(cursor))
   {
      return;
   }
   
   // A binary operator continues the statement.
   if (isBinaryOp(cursor))
   {
      // Warn about surrounding whitespace now. Note that
      // we did not warn about the previous whitespace because
      // we were not yet sure if this were a binary op.
      if (!isWhitespace(cursor.previousToken()))
         status.lint().expectedWhitespace(cursor.previousToken());
      
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      return beginParse(cursor, status);
   }
   
   // Shouldn't get here!
   status.lint().unexpectedToken(cursor);
}

void handleStatementOrExpression(TokenCursor& cursor,
                                 ParseStatus& status)
{
   FWD_OVER_WHITESPACE(cursor, status);
   
   if (cursor.isType(RToken::LBRACE))
      return handleExpression(cursor, status);
   else
      handleStatement(cursor, status);
}

// An argument list is of the form:
//
//    <l-bracket> <r-bracket>
//    <l-bracket> <arg-expr> <r-bracket>
//    <l-bracket> <arg-expr> (comma arg-expr)* <r-bracket>
//
// This is true for '(', '[' and '[['.
void handleArgumentList(TokenCursor& cursor,
                        ParseStatus& status,
                        bool cacheIdentifiers = true)
{
   if (!isLeftBrace(cursor))
      status.lint().unexpectedToken(cursor);
   
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
      handleArgument(cursor, status);
   
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
                    ParseStatus& status,
                    bool cacheIdentifiers = false)
{
   if (isId(cursor) &&
       cursor.nextSignificantToken().contentEquals(L"="))
   {
      // Move on to '='
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      
      // Move to start of expression
      MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
      
      // It is possible to leave the token to the right of '=' as missing;
      // in such a case we will already see a comma or right paren. In
      // such a case, return early (and let the parent caller move forward)
      if (cursor.isType(RToken::COMMA) ||
          isRightBrace(cursor))
         return;
   }
   
   // Handle a generic expression, which should be followed by a
   // comma or a closing bracket.
   beginParse(cursor, status);
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
   status.addChildAndSetAsCurrentNode(ParseNode::createNode(fnName),
                                      cursor.currentPosition());
   
   ENSURE_CONTENT(cursor, status, L"function");
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_ON_WHITESPACE(cursor, status);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   handleArgumentList(cursor, status, true);
   ENSURE_TYPE(cursor, status, RToken::LPAREN);
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN_WARN_IF_NO_WHITESPACE(cursor, status);
   
   // Here, we need to be careful -- are we parsing a single statement
   // following a function declaration, or a whole expression? This
   // is important as we need to know when to close the current scope
   // (so we know what arguments are available to the expression, or
   // statement)
   if (cursor.isType(RToken::LBRACE))
      handleExpression(cursor, status);
   else
      handleStatement(cursor, status);
   
   MOVE_TO_NEXT_SIGNIFICANT_TOKEN(cursor, status);
   
   // Close the function scope.
   status.setParentAsCurrent();
   
   // Continue parsing!
   return beginParse(cursor, status);
   
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
   
   // The parse status is a collection of lint, plus a stack
   // of braces (to check as we parse)
   ParseStatus status;
   
   // The token cursor is used to walk through the tokens
   // and update lint.
   TokenCursor cursor(rTokens);
   
   // An R script is really just a big R expression to evaluate,
   // so that's where we start!
   handleExpression(&cursor, &status);
   
   do
   {
      DEBUG("Current token: " << cursor);
      DEBUG("Current type:  " << cursor.type());
      
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
         braceStack.push(cursor);
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
            const ParseItem& topOfStack = braceStack.peek();

            // ... and the cursor is on a '}'
            if (isRightBrace)
            {
               // ... and the expression stack has a '{',
               // then close the expression
               if (topOfStack.symbol == "{")
               {
                  DEBUG("* Encountered '" << cursor << "'; closing expression body");
                  braceStack.pop();
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
                  braceStack.pop();
                  
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
