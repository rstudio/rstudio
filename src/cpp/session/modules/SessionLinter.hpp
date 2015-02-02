/*
 * SessionLinter.hpp
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

#ifndef SESSION_LINTER_HPP
#define SESSION_LINTER_HPP

#include <vector>
#include <string>
#include <iostream>
#include <sstream>
#include <map>
#include <set>

#include <core/r_util/RTokenizer.hpp>
#include <core/collection/Stack.hpp>
#include <core/StringUtils.hpp>

#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/range/adaptor/map.hpp>

#define RSTUDIO_ENABLE_DEBUG_MACROS
#include <core/Macros.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

using namespace core;
using namespace core::r_util;
using namespace core::r_util::token_utils;
using namespace core::collection;

std::string complement(const std::string& string);

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
      return text.size() && token_.contentEquals(text);
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
      : dummyText_(L"ERR"),
        dummyToken_(
            AnnotatedRToken(-1, -1, RToken(RToken::ERR, dummyText_.begin(),
                                           dummyText_.end(), rTokens.size())))
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
   std::wstring dummyText_;
   AnnotatedRToken dummyToken_;
   
};

enum LintType
{
   LintTypeStyle,
   LintTypeInfo,
   LintTypeWarning,
   LintTypeError
};

inline std::string asString(LintType type)
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
      
      if (!fwdOverWhitespaceAndComments())
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
   
   bool fwdOverWhitespaceAndComments()
   {
      while (currentToken().isType(RToken::WHITESPACE) ||
             currentToken().isType(RToken::COMMENT))
         if (!moveToNextToken())
            return false;
      return true;
   }
   
   bool bwdOverWhitespaceAndComments()
   {
      while (currentToken().isType(RToken::WHITESPACE) ||
             currentToken().isType(RToken::COMMENT))
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
   
   // We are at the end of the document if there are no
   // more significant tokens following.
   bool isAtEndOfDocument()
   {
      return !clone().moveToNextSignificantToken();
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
      std::string message = "unexpected token '" + content + "'";
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
         const ParseItem& topOfStack = braceStack.peek();
         
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
   
   void addChild(boost::shared_ptr<ParseNode>& pChild,
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

class ParseStatus {
   
public:
   
   ParseStatus()
      : pRoot_(ParseNode::createRootNode()),
        pNode_(pRoot_.get()),
        depth_(0),
        lastCursorPosition_(Position(0, 0))
   {
   }
   
   ParseNode* node() { return pNode_; }
   BraceStack& stack() { return stack_; }
   LintItems& lint() { return lint_; }
   boost::shared_ptr<ParseNode> root() { return pRoot_; }
   
   void addChildAndSetAsCurrentNode(boost::shared_ptr<ParseNode>& pChild,
                                    const Position& position)
   {
      node()->addChild(pChild, position);
      pNode_ = pChild.get();
   }
   
   void setParentAsCurrent()
   {
      pNode_ = pNode_->getParent();
   }
   
   void push(const TokenCursor& cursor)
   {
      DEBUG("*** Pushing " << cursor << " on to brace stack");
      stack_.push(ParseItem(
                     cursor.contentAsUtf8(),
                     cursor.currentPosition(),
                     pNode_));
   }
   
   void pop(const TokenCursor& cursor)
   {
      DEBUG("*** Popping " << cursor << " from brace stack");
      if (stack_.peek().symbol != complement(cursor.contentAsUtf8()))
         lint_.unexpectedClosingBracket(cursor, stack_);
      stack_.pop();
   }
   
   bool hasLint() const
   {
      return !lint_.get().empty();
   }
   
   void setDepth(std::size_t depth)
   {
      depth_ = depth;
   }
   
   std::size_t getDepth()
   {
      return depth_;
   }
   
   void setLastCursorPosition(const Position& position)
   {
      lastCursorPosition_ = position;
   }

   const Position& getLastCursorPosition() const
   {
      return lastCursorPosition_;
   }
   
private:
   boost::shared_ptr<ParseNode> pRoot_;
   ParseNode* pNode_;
   BraceStack stack_;
   LintItems lint_;
   std::size_t depth_;
   Position lastCursorPosition_;
};

typedef std::pair<
   boost::shared_ptr<ParseNode>,
   LintItems> ParseResults;

ParseResults parse(const std::string& string);
                   
core::Error initialize();

} // namespace linter
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_LNTER_HPP
