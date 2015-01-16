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

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/r_util/RTokenizer.hpp>
#include <core/FileUtils.hpp>
#include <core/collection/Tree.hpp>

namespace session {
namespace modules {
namespace linter {

using namespace core;
using namespace core::r_util;
using namespace core::r_util::token_utils;

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
   
   operator RToken() const
   {
      return token_;
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
   AnnotatedRTokens(const RTokens& rTokens)
      : dummyToken_(AnnotatedRToken(-1, -1, RToken()))
   {
      std::size_t row = 0;
      std::size_t column = 0;
      
      std::size_t n = rTokens.size();
      for (std::size_t i = 0; i < n; ++i)
      {
         // Add the token
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
            column = content.length() - content.find_last_of(L"\r\n");
         }
         else
         {
            column += content.length();
         }
      }
   }
   
   const AnnotatedRToken& at(std::size_t index) const
   {
      if (index > tokens_.size())
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
            << "Dumping " << tokens_.size() << " tokens:\n";
      
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
               << "}\n";
      }
   }
   
private:
   std::vector<AnnotatedRToken> tokens_;
   AnnotatedRToken dummyToken_;
   
};

enum LintType
{
   STYLE,
   INFO,
   WARNING,
   ERROR
};

class LintItem
{
private:
   int startRow_;
   int startColumn_;
   int endRow_;
   int endColumn_;
   LintType type_;
   std::string message_;
};

typedef std::vector<LintItem> LintItems;

struct ParseItem
{
   ParseItem(int row, int column, const std::string& name)
      : row(row), column(column), name(name) {}
   
   ParseItem(const AnnotatedRToken& rToken)
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

class ParseNode
{
public:
   
   explicit ParseNode(const std::string& name)
      : name_(name) {}
   
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
   
   boost::weak_ptr<ParseNode>& getParent()
   {
      return parent_;
   }
   
   std::vector< boost::shared_ptr<ParseNode> > getChildren()
   {
      return children_;
   }
   
   void addChild(const boost::shared_ptr<ParseNode>& pChild)
   {
      children_.push_back(pChild);
   }
   
private:
   
   // tree reference -- children and parent
   // parents protect their children
   boost::weak_ptr<ParseNode> parent_;
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

namespace {

// Move pIndex to the index of the matching token, or no movement
// on failure.
bool moveIndexToMatchingToken(const AnnotatedRTokens& rTokens,
                              std::size_t* pIndex)
{
   std::size_t index = *pIndex;
   const AnnotatedRToken& currentToken = rTokens.at(index);

#define RSTUDIO_FWD_TO_MATCHING_TOKEN(LEFT_OP, RIGHT_OP) do { \
   if (currentToken.type() == LEFT_OP) \
   { \
      std::size_t numTokens = rTokens.size(); \
      ++index; \
      while (rTokens.at(index).type() != RIGHT_OP && index < numTokens - 1) \
         ++index; \
      *pIndex = index; \
      return true; \
   } \
} while (0)
   
#define RSTUDIO_BWD_TO_MATCHING_TOKEN(LEFT_OP, RIGHT_OP) do { \
   if (currentToken.type() == RIGHT_OP) \
   { \
      --index; \
      while (rTokens.at(index).type() != LEFT_OP && index > 0) \
         --index; \
      *pIndex = index; \
      return index; \
   } \
} while (0)
   
#define RSTUDIO_MOVE_TO_MATCHING_TOKEN(TOKEN_NAME) \
   RSTUDIO_FWD_TO_MATCHING_TOKEN(RToken::L##TOKEN_NAME, RToken::R##TOKEN_NAME); \
   RSTUDIO_BWD_TO_MATCHING_TOKEN(RToken::L##TOKEN_NAME, RToken::R##TOKEN_NAME); \
   
RSTUDIO_MOVE_TO_MATCHING_TOKEN(BRACE);
RSTUDIO_MOVE_TO_MATCHING_TOKEN(PAREN);
RSTUDIO_MOVE_TO_MATCHING_TOKEN(BRACKET);
RSTUDIO_MOVE_TO_MATCHING_TOKEN(DBRACKET);

return false;

#undef RSTUDIO_BWD_TO_MATCHING_TOKEN
#undef RSTUDIO_FWD_TO_MATCHING_TOKEN
#undef RSTUDIO_MOVE_TO_MATCHING_TOKEN

}

void handleIdToken(const AnnotatedRTokens& rTokens,
                   const AnnotatedRToken& thisToken,
                   boost::shared_ptr<ParseNode> pNode,
                   std::size_t i)
{
   // If the following token is a '::' or ':::', then
   // we add to the set of namespace entries used.
   const AnnotatedRToken& nextToken = rTokens.at(i + 1);
   if (isNamespace(nextToken))
   {
      const AnnotatedRToken& nextNextToken = rTokens.at(i + 2);
      if (nextToken.content() == L"::")
         pNode->addExportedPackageSymbol(
                  thisToken.contentAsUtf8(),
                  nextNextToken.contentAsUtf8());
      else
         pNode->addInternalPackageSymbol(
                  thisToken.contentAsUtf8(),
                  nextNextToken.contentAsUtf8());

      return;
   }

   // If the previous symbol is a '$' or an '@', then we don't
   // touch it for now.
   //
   // TODO: Add another map for 'containers' in a scope?
   const AnnotatedRToken& prevToken = rTokens.at(i - 1);
   if (isAt(prevToken) ||
       isDollar(prevToken))
      return;

   // Check to see if there is a left assign following the token.
   // If so, add that symbol to this scope.
   if (isLocalLeftAssign(nextToken))
      pNode->addDefinedVariable(thisToken);

   // Similarily for previous assign
   if (isLocalRightAssign(prevToken))
      pNode->addDefinedVariable(thisToken);

   // TODO: Handle 'global' assign. This implies searching
   // parent scopes to see if the variable is defined locally
   // or defined in a parent scope.

   // Add a reference to this variable
   pNode->addReferencedVariable(nextToken);
}

// This function takes a token stream of the form:
//
//    x <- function(a, b, c) {
//         ^
// and:
// 1. Populates the current scope with variables in the argument list,
// 2. Creates a new node for the function.
std::size_t handleFunctionToken(const AnnotatedRTokens& rTokens,
                                const AnnotatedRToken& thisToken,
                                boost::shared_ptr<ParseNode> pNode,
                                std::size_t i)
{
   std::size_t n = rTokens.size();
   std::size_t tokenIndex = i + 1;
   
   // TODO: name me!
   boost::shared_ptr<ParseNode> pChild(new ParseNode("name"));
   
   // TODO: What if the token following the 'function' isn't an open paren?
   if (rTokens.at(tokenIndex).content() != L"(")
      return -1;
   
   ++tokenIndex;
   
   // Check to see if we have no arguments for this function.
   if (rTokens.at(tokenIndex).content() == L")")
      goto ENCOUNTERED_END_OF_ARGUMENT_LIST;
   
   ++tokenIndex;
   
   // Add the first element from the argument list.
   if (isId(rTokens.at(tokenIndex)))
      pChild->addDefinedVariable(rTokens.at(tokenIndex));
   
   // Start looking for ',' for the next entries.
   while (++tokenIndex < n)
   {
      const RToken& token = rTokens.at(tokenIndex);
      
      // Move over matching parens
      if (moveIndexToMatchingToken(rTokens, &tokenIndex))
         continue;
      
      // If we encounter a ',', the next token should be
      // an identifier.
      if (isComma(token))
         pChild->addDefinedVariable(rTokens.at(tokenIndex + 1));
      
      // If we encounter a closing paren, break
      if (isRightBrace(token))
      {
         // TODO: We escape on all right braces but only ')' is valid.
         break;
      }
   }
      
   ENCOUNTERED_END_OF_ARGUMENT_LIST:
   
   // The next token should be a '{'.
   if (rTokens.at(tokenIndex).content() == L"{")
      pNode->addChild(pChild);
   
   // Return the updated index
   ++tokenIndex;
   return tokenIndex;
   
}

std::size_t handleStringToken(const AnnotatedRTokens& rTokens,
                              const AnnotatedRToken& thisToken,
                              boost::shared_ptr<ParseNode> pNode,
                              std::size_t i)
{
   const RToken& nextToken = rTokens.at(i + 1);
   if (isLocalLeftAssign(nextToken))
   {
      pNode->addDefinedVariable(thisToken);
      return i + 1;
   }
   return i;
}

   
} // end anonymous namespace

boost::shared_ptr<ParseNode> parseRFile(const FilePath& filePath)
{
   std::string contents = file_utils::readFile(filePath);
   
   RTokens tokens(string_utils::utf8ToWide(contents));
   AnnotatedRTokens rTokens(tokens);
   
   // Create an empty tree to populate
   boost::shared_ptr<ParseNode> pNode(new ParseNode("<root>"));
   
   // Move through the tokens and populate the lint tree.
   std::size_t numTokens = rTokens.size();
   
   std::size_t braceStack = 0; // counting '{', '}'
   
   for (std::size_t i = 0; i < numTokens; ++i)
   {
      const AnnotatedRToken& thisToken = rTokens.at(i);
      
      // Handle a 'function' token (implies we are about to enter a new scope)
      if (isFunction(thisToken))
      {
         i = handleFunctionToken(rTokens, thisToken, pNode, i);
         continue;
      }
      
      // Handle an identifier (implies we should check to see if it has a reference
      // in the parse tree)
      if (isId(thisToken))
      {
         handleIdToken(rTokens, thisToken, pNode, i);
         continue;
      }
      
      // Strings can be assigned to, too. One could do something like:
      //
      //    "abc" <- function() {}
      //    "abc"()
      if (isString(thisToken))
      {
         i = handleStringToken(rTokens, thisToken, pNode, i);
         continue;
      }
      
      // If we encounter a '{', add to the stack. Note that 'isFunction()'
      // will have moved over such an open brace and hence these open
      // braces are just opening expressions (not associated with a closure)
      if (thisToken.contentEquals(L"{"))
      {
         ++braceStack;
         continue;
      }
      
      // If we encounter a '}', decrement the stack,
      // or close a function node.
      if (thisToken.contentEquals(L"}"))
      {
         if (braceStack == 0)
         {
            // If this is a '}' not associated with a function, then
            // there will be no parent. Only set the current node to
            // the parent if it exists!
            boost::shared_ptr<ParseNode> pParent(pNode->getParent());
            if (pParent != NULL)
            {
               pNode = pParent;
            }
         }
         else
            --braceStack;
         
         continue;
      }
   }
   
   // Return the parent node
   while (pNode->getName() != "<root>")
      pNode = boost::shared_ptr<ParseNode>(pNode->getParent());
   
   return pNode;
   
}

SEXP rs_parseRFile(SEXP absoluteFilePathSEXP)
{
   std::string absoluteFilePath = r::sexp::asString(absoluteFilePathSEXP);
   FilePath filePath(absoluteFilePath);
   boost::shared_ptr<ParseNode> pParseTree = parseRFile(filePath);
   return R_NilValue;
}

core::Error initialize()
{
   using namespace core;
   using boost::bind;
   using namespace module_context;
   
   r::routines::registerCallMethod(
            "rs_parseRFile",
            (DL_FUNC) rs_parseRFile,
            1);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionLinter.R"));

   return initBlock.execute();

}

} // end namespace linter
} // end namespace modules
} // end namespace session
