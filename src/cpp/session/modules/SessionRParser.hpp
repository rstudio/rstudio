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

// This class implements a simple (recursive-descent) R parser -- it does not
// build a full syntax tree, but instead builds a tree structure that we can
// use to efficiently look up and validate that variables within certain scopes
// are properly declared / defined before usage.

#ifndef SESSION_MODULES_RPARSER_HPP
#define SESSION_MODULES_RPARSER_HPP

// #define RSTUDIO_DEBUG_LABEL "parser"
// #define RSTUDIO_ENABLE_DEBUG_MACROS

#include <vector>
#include <map>
#include <set>
#include <iostream>
#include <iomanip>

#include <core/r_util/RTokenizer.hpp>
#include <core/collection/Position.hpp>
#include <core/collection/Stack.hpp>

#include <r/RSexp.hpp>
#include <r/RExec.hpp>

#include <boost/container/flat_set.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Macros.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rparser {

using namespace core::collection;

class ParseOptions
{
public:
   
   explicit ParseOptions(bool recordStyleLint = false,
                         bool lintRFunctions = false)
      : recordStyleLint_(recordStyleLint),
        lintRFunctions_(lintRFunctions)
   {}
   
   void setRecordStyleLint(bool record)
   {
      recordStyleLint_ = record;
   }
   
   bool recordStyleLint() const
   {
      return recordStyleLint_;
   }
   
   void setLintRFunctions(bool lint)
   {
      lintRFunctions_ = lint;
   }
   
   bool lintRFunctions() const
   {
      return lintRFunctions_;
   }

private:
   bool recordStyleLint_;
   bool lintRFunctions_;
};

struct ParseItem;

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

namespace linter {

enum LintType
{
   LintTypeStyle,
   LintTypeInfo,
   LintTypeWarning,
   LintTypeError
};

inline std::string lintTypeToString(LintType type)
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

using namespace core;
using namespace core::r_util;

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
   
   LintItem(const RToken& item,
            LintType type,
            const std::string& message)
      : startRow(item.row()),
        startColumn(item.column()),
        endRow(item.row()),
        endColumn(item.column() + item.length()),
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
      : errorCount_(0)
   {}
   
   explicit LintItems(const ParseOptions& parseOptions)
      : errorCount_(0),
        parseOptions_(parseOptions)
   {}
   
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
   
   void unexpectedToken(const RToken& token,
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
   
   void unexpectedToken(const RToken& token,
                           const std::wstring& expected)
   {
      unexpectedToken(token, string_utils::wideToUtf8(expected));
   }
   
   void unexpectedClosingBracket(const RToken& token)
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
   }
   
   void unexpectedEndOfDocument(const RToken& token)
   {
      LintItem lint(token.row(),
                    token.column() + token.length(),
                    token.row(),
                    token.column() + token.length(),
                    LintTypeError,
                    "unexpected end of document");
      lintItems_.push_back(lint);
   }
   
   void noSymbolNamed(const ParseItem& item,
                      const std::string& candidate = std::string())
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
   
   void expectedWhitespace(const RToken& token)
   {
      if (!parseOptions_.recordStyleLint())
         return;
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column(),
                    LintTypeStyle,
                    "expected whitespace");
      lintItems_.push_back(lint);
   }
   
   void unnecessaryWhitespace(const RToken& token)
   {
      if (!parseOptions_.recordStyleLint())
         return;
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + token.length(),
                    LintTypeStyle,
                    "unnecessary whitespace");
      lintItems_.push_back(lint);
   }
   
   void unexpectedWhitespaceAroundOperator(const RToken& token)
   {
      if (!parseOptions_.recordStyleLint())
         return;
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + token.length(),
                    LintTypeStyle,
                    "unexpected whitespace around extraction operator");
      lintItems_.push_back(lint);
   }
   
   void expectedWhitespaceAroundOperator(const RToken& token)
   {
      if (!parseOptions_.recordStyleLint())
         return;
      
      LintItem lint(token.row(),
                    token.column(),
                    token.row(),
                    token.column() + token.length(),
                    LintTypeStyle,
                    "expected whitespace around binary operator");
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
   
   void push_back(const LintItems& items)
   {
      for (std::size_t i = 0, n = items.size(); i < n; ++i)
         lintItems_.push_back(items.get()[i]);
   }
   
   typedef std::vector<LintItem>::iterator iterator;
   typedef std::vector<LintItem>::const_iterator const_iterator;
   
   const_iterator begin() const { return lintItems_.begin(); }
   const_iterator end() const { return lintItems_.end(); }
   std::size_t size() const { return lintItems_.size(); }
   std::size_t empty() const { return lintItems_.empty(); }
   
   std::size_t errorCount() const { return errorCount_; }
   bool hasErrors() const { return errorCount_ > 0; }
   void dump();
   
private:
   std::vector<LintItem> lintItems_;
   std::size_t errorCount_;
   ParseOptions parseOptions_;
};

}

using namespace linter;

std::string& complement(const std::string& bracket);

class ParseNode : public boost::noncopyable
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

   void addDefinedSymbol(const RToken& rToken)
   {
      DEBUG("--- Adding defined variable '" << rToken.contentAsUtf8() << "'");
      definedSymbols_[rToken.contentAsUtf8()].push_back(
            Position(rToken.row(), rToken.column()));
   }
   
   void addDefinedSymbol(const RToken& rToken,
                         const Position& position)
   {
      definedSymbols_[rToken.contentAsUtf8()].push_back(position);
   }
   
   void addReferencedSymbol(int row,
                              int column,
                              const std::string& name)
   {
      referencedSymbols_[name].push_back(Position(row, column));
   }

   void addReferencedSymbol(const RToken& rToken)
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
   
   ParseNode* const getParent() const
   {
      return pParent_;
   }
   
   ParseNode* const getRoot() const
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
   
   bool symbolExistsInSearchPath(const std::string& symbol,
                                 const std::set<std::string>& searchPathObjects) const
   {
      return searchPathObjects.count(symbol) != 0;
   }
   
   static bool doFindFunction(const ParseNode* pNode,
                              const std::string& name,
                              const Position& position,
                              const ParseNode** ppFoundNode)
   {
      if (!pNode) return false;
      
      if (pNode->name_ == name && pNode->position_ <= position)
      {
         *ppFoundNode = pNode;
         return true;
      }
      
      // We search the children in reverse order, to ensure we find
      // the first function with a particular name (in case multiple
      // functions with the same name exist)
      std::size_t n = pNode->children_.size();
      for (std::size_t i = 0; i < n; i++)
      {
         std::size_t index = n - i - 1;
         const boost::shared_ptr<ParseNode>& pChild = pNode->children_[index];
         if (pChild->name_ == name && pChild->position_ <= position)
         {
            *ppFoundNode = pChild.get();
            return true;
         }
      }
      
      return doFindFunction(pNode->pParent_,
                            name,
                            position,
                            ppFoundNode);
   }
   
   bool findFunction(const std::string& name,
                     const Position& position,
                     const ParseNode** ppFoundNode) const
   {
      return doFindFunction(
               this,
               name,
               position,
               ppFoundNode);
   }
   
   bool symbolHasDefinitionInTree(const std::string& symbol,
                                  const Position& position) const
   {
      if (definedSymbols_.count(symbol) != 0)
      {
         DEBUG("- Checking for symbol '" << symbol << "' in node");
         Positions& positions = const_cast<ParseNode*>(this)->definedSymbols_[symbol];
         for (Positions::reverse_iterator it = positions.rbegin();
              it != positions.rend();
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
   
public:
   
   const std::string& name() const { return name_; }
   const Position& position() const { return position_; }
   
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

class ParseStatus
{
   
public:
   
   explicit ParseStatus(const ParseOptions& parseOptions)
      : pRoot_(ParseNode::createRootNode()),
        pNode_(pRoot_.get()),
        lint_(parseOptions),
        parseOptions_(parseOptions)
   {
      parseStateStack_.push(ParseStateTopLevel);
      functionNames_.push(std::wstring(L""));
   }
   
   ParseNode* node() { return pNode_; }
   LintItems& lint() { return lint_; }
   boost::shared_ptr<ParseNode> root() { return pRoot_; }
   
   void addChildAndSetAsCurrentNode(boost::shared_ptr<ParseNode> pChild,
                                    const Position& position)
   {
      node()->addChild(pChild, position);
      pNode_ = pChild.get();
   }
   
   void setParentAsCurrent()
   {
      pNode_ = pNode_->getParent();
   }
   
   bool hasLint() const
   {
      return !lint_.get().empty();
   }
   
   enum ParseState {
      
      // top level
      ParseStateTopLevel,
      
      // within () but not an argument list
      ParseStateWithinParens,
      
      // within {}
      ParseStateWithinBraces,
      
      // if '(' <cond> ')' (<statement> | <expr>)
      ParseStateIfCondition,
      ParseStateIfStatement,
      ParseStateIfExpression,
      
      // while '(' <cond> ')' (<statement> | <expr>)
      ParseStateWhileCondition,
      ParseStateWhileStatement,
      ParseStateWhileExpression,
      
      // for '(' <id> in <cond> ')' (<statement> | <expr>)
      ParseStateForCondition,
      ParseStateForStatement,
      ParseStateForExpression,
      
      // repeat (<statement> | <expr>)
      ParseStateRepeatStatement,
      ParseStateRepeatExpression,
      
      // function(<arglist>) <expr>
      ParseStateFunctionArgumentList,
      ParseStateFunctionStatement,
      ParseStateFunctionExpression,
      
      // <id> ( <arglist> )
      ParseStateParenArgumentList,
      ParseStateSingleBracketArgumentList,
      ParseStateDoubleBracketArgumentList,
      
   };
   
   Stack<ParseState>& parseStateStack()
   {
      return parseStateStack_;
   }
   
   void pushFunctionCallState(ParseState state,
                              const std::wstring& functionName)
   {
      DEBUG("Pushing state: " << stateAsString(state));
      parseStateStack_.push(state);
      functionNames_.push(functionName);
   }
   
   const std::wstring& currentFunctionName() const
   {
      return functionNames_.peek();
   }
   
   void enterFunctionScope(const std::string& name,
                           const Position& position)
   {
      addChildAndSetAsCurrentNode(
               ParseNode::createNode(name),
               position);
      
      DEBUG("Entering function scope: '" << name << "' at " << position);
      pushState(ParseStateFunctionArgumentList);
   }
   
   void pushState(ParseState state)
   {
      DEBUG("Pushing state: " << stateAsString(state));
      parseStateStack_.push(state);
   }
   
   void popFunctionName()
   {
      if (functionNames_.peek().length())
         functionNames_.pop();
   }
   
   void popState()
   {
      switch (currentState())
      {
      case ParseStateFunctionExpression:
      case ParseStateFunctionStatement:
         setParentAsCurrent();
         break;
      case ParseStateParenArgumentList:
      case ParseStateSingleBracketArgumentList:
      case ParseStateDoubleBracketArgumentList:
         popFunctionName();
         break;
         
      // suppress compiler warnings
      default:
         break;
      }

      if (currentState() != ParseStateTopLevel)
      {
         DEBUG("Popping state: " << currentStateAsString());
         parseStateStack_.pop();
      }
      else
      {
         DEBUG("Already at top level; no state to pop");
      }
   }
   
   ParseState peekState(std::size_t depth = 0) const
   {
      if (depth > parseStateStack_.size())
         return ParseStateTopLevel;
      return parseStateStack_[parseStateStack_.size() - depth - 1];
   }
   
   ParseState currentState() const { return parseStateStack_.peek(); }
   
   std::string stateAsString(ParseState state) const
   {
#define CASE(__X__) \
   case __X__: return #__X__
      
      switch (state)
      {
      CASE(ParseStateTopLevel);
      CASE(ParseStateWithinParens);
      CASE(ParseStateWithinBraces);
      CASE(ParseStateIfCondition);
      CASE(ParseStateIfStatement);
      CASE(ParseStateIfExpression);
      CASE(ParseStateWhileCondition);
      CASE(ParseStateWhileStatement);
      CASE(ParseStateWhileExpression);
      CASE(ParseStateForCondition);
      CASE(ParseStateForStatement);
      CASE(ParseStateForExpression);
      CASE(ParseStateRepeatStatement);
      CASE(ParseStateRepeatExpression);
      CASE(ParseStateFunctionArgumentList);
      CASE(ParseStateFunctionStatement);
      CASE(ParseStateFunctionExpression);
      CASE(ParseStateParenArgumentList);
      CASE(ParseStateSingleBracketArgumentList);
      CASE(ParseStateDoubleBracketArgumentList);
      default: return "<unknown>";
      }
#undef CASE
   }
   
   std::string currentStateAsString() const { return stateAsString(currentState()); }
   
   bool isAtTopLevel() const
   {
      return currentState() == ParseStateTopLevel;
   }
   
   bool isInArgumentList() const
   {
      switch (currentState())
      {
      case ParseStateParenArgumentList:
      case ParseStateSingleBracketArgumentList:
      case ParseStateDoubleBracketArgumentList:
      case ParseStateFunctionArgumentList:
         return true;
      default:
         return false;
      }
   }
   
   bool isInControlFlowStatement() const
   {
      switch (currentState())
      {
      case ParseStateForStatement:
      case ParseStateWhileStatement:
      case ParseStateIfStatement:
      case ParseStateRepeatStatement:
      case ParseStateFunctionStatement:
         return true;
      default:
         return false;
      }
   }
   
   bool isInControlFlowExpression() const
   {
      switch (currentState())
      {
      case ParseStateForExpression:
      case ParseStateWhileExpression:
      case ParseStateIfExpression:
      case ParseStateRepeatExpression:
      case ParseStateFunctionExpression:
         return true;
      default:
         return false;
      }
   }
   
   bool isInControlFlowCondition() const
   {
      switch (currentState())
      {
      case ParseStateForCondition:
      case ParseStateWhileCondition:
      case ParseStateIfCondition:
         return true;
      default:
         return false;
      }
   }
   
   bool isInParentheticalScope() const
   {
      switch (currentState())
      {
      case ParseStateForCondition:
      case ParseStateWhileCondition:
      case ParseStateIfCondition:
         
      case ParseStateParenArgumentList:
      case ParseStateSingleBracketArgumentList:
      case ParseStateDoubleBracketArgumentList:
      case ParseStateFunctionArgumentList:
         
      case ParseStateWithinParens:
         return true;
      default:
         return false;
      }
   }
   
   const Stack<std::wstring>& functionNames() const
   {
      return functionNames_;
   }
   
   const ParseOptions& parseOptions() const
   {
      return parseOptions_;
   }
   
   void pushNseCall(const std::wstring& call)
   {
      nseCalls_.push(call);
   }
   
   void popNseCall()
   {
      if (withinNseCall())
         nseCalls_.pop();
   }
   
   bool withinNseCall() const
   {
      return !nseCalls_.empty();
   }
   
private:
   boost::shared_ptr<ParseNode> pRoot_;
   ParseNode* pNode_;
   LintItems lint_;
   ParseOptions parseOptions_;
   Stack<ParseState> parseStateStack_;
   Stack<std::wstring> functionNames_;
   Stack<std::wstring> nseCalls_;
};

class ParseResults {
   
public:
   
   ParseResults()
      : parseTree_(ParseNode::createRootNode())
   {}
   
   ParseResults(boost::shared_ptr<ParseNode> parseTree,
                LintItems lint)
      : parseTree_(parseTree),
        lint_(lint)
   {}
   
   // copy ctor: copyable members
   
   ParseNode* parseTree() const
   {
      return parseTree_.get();
   }
   
   const LintItems& lint() const { return lint_; }
   LintItems& lint() { return lint_; }
   
private:
   
   boost::shared_ptr<ParseNode> parseTree_;
   LintItems lint_;
};

ParseResults parse(const std::string& rCode,
                   const ParseOptions& parseOptions = ParseOptions());

ParseResults parse(const std::wstring& rCode,
                   const ParseOptions& parseOptions = ParseOptions());

} // namespace rparser
} // namespace modules
} // namespace session
} // namespace rstudio


#endif // SESSION_MODULES_RPARSER_HPP
