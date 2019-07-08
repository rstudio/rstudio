/*
 * CssParser.hpp
 *
 * Copyright (C) 2019 by RStudio, Inc.
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

#include "CssParser.hpp"

#include <cctype>
#include <queue>

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>

#include "SessionThemes.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

namespace {

// Helpers =============================================================================================================
inline bool matches(const std::string& val, const std::string& regex)
{
   return boost::regex_search(val, boost::regex(regex));
}

inline bool isDigit(const std::string& val)
{
   return !val.empty() && std::isdigit(val[0]);
}

inline bool isHex(const std::string& val)
{
   return !val.empty() && std::isxdigit(val[0]);
}

inline bool startsWithEscape(const std::string& val)
{
   if (val.size() < 2)
      return false;

   return (val[0] == '\\') && (val[1] != '\n');
}

inline bool startsWithNameStartCodePoint(const std::string& val)
{
   return matches(val, "^([a-zA-Z\\x{0080}-\\x{FFFF}_])");
}

inline bool startsWithNameCodePoint(const std::string& val)
{
   return startsWithNameStartCodePoint(val) || isDigit(val) || (!val.empty() && (val[0] == '-'));
}

inline bool startsWithIdent(const std::string& val)
{
   if ((val.size() > 1) && (val[0] == '-'))
      return startsWithNameStartCodePoint(&val[1]) || startsWithEscape(&val[1]);

   return startsWithNameStartCodePoint(val) || startsWithEscape(val);
}

inline std::string cutString(std::string& val, const std::string& regex)
{
   boost::smatch match;
   if (!boost::regex_search(val, match, boost::regex(regex)))
      return "";

   std::string result = match[1];
   val = val.substr(result.size());
   return result;
}

inline std::string consumeEscape(std::string& val)
{
   std::string esc;
   assert(val.size() >= 2);

   if (isHex(val))
      esc = cutString(val, "^\\\\[a-fA-F\\d]{1,6}");
   else
   {
      esc = cutString(val, "^\\\\.");
   }

   return esc;
}

inline std::string consumeName(std::string& val)
{
   std::string name;
   bool startsWithEsc = startsWithEscape(val);
   while (startsWithNameCodePoint(val) || startsWithEsc)
   {
      if (startsWithEsc)
         name += consumeEscape(val);
      else
         name += cutString(val, "^[a-zA-Z\\x{0080}-\\x{FFFF}_\\d-]");

      startsWithEsc = startsWithEscape(val);
   }

   return name;
}

// CSS Tokenization Types ==============================================================================================
enum class CssTokenType
{
   AT,
   BRACE_CLOSE,
   BRACE_OPEN,
   BRACKET_CLOSE,
   BRACKET_OPEN,
   CD_CLOSE,
   CD_OPEN,
   COLON,
   COLUMN,
   COMMENT,
   COMMA,
   DELIM,
   DIM,
   FUNCTION,
   HASH,
   IDENT,
   MATCH_DASH,
   MATCH_INCLUDE,
   MATCH_PREFIX,
   MATCH_SUBSTRING,
   MATCH_SUFFIX,
   NUMBER,
   PERCENT,
   PAREN_CLOSE,
   PAREN_OPEN,
   SEMICOLON,
   STRING,
   STRING_BAD,
   UNI_RANGE,
   URL,
   URL_BAD,
   WHITESPACE,
};

struct CssToken
{
   CssTokenType Type;
   std::string Value;

   explicit CssToken(CssTokenType type, std::string value = "") :
      Type(type), Value(std::move(value)) { };
};

CssToken at(std::string& css)
{
   return CssToken(CssTokenType::AT, consumeName(css));
}

CssToken braceClose(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::BRACE_CLOSE);
}

CssToken braceOpen(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::BRACE_OPEN);
}

CssToken bracketClose(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::BRACKET_CLOSE);
}

CssToken bracketOpen(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::BRACKET_OPEN);
}

CssToken CDClose(std::string& css)
{
   // Consume -->
   css = css.substr(3);
   return CssToken(CssTokenType::CD_CLOSE);
}

CssToken CDOpen(std::string& css)
{
   // Consume <!--
   css = css.substr(4);
   return CssToken(CssTokenType::CD_OPEN);
}

CssToken colon(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::COLON);
}

CssToken column(std::string& css)
{
   // Consume ||
   css = css.substr(2);
   return CssToken(CssTokenType::COLUMN);
}

CssToken comment(std::string& css)
{
   return CssToken(CssTokenType::COMMENT, cutString(css, "^(/\\*.*?\\*/)"));
}

CssToken comma(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::COMMA);
}

CssToken delim(std::string& css)
{
   std::string val = css.substr(0, 1);
   css = css.substr(1);
   return CssToken(CssTokenType::DELIM, val);
}

CssToken hash(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::HASH, consumeName(css));
}

CssToken match(std::string& css)
{
   assert(css.size() >= 2);
   CssTokenType type = CssTokenType::MATCH_DASH; // Avoid warnings
   switch (css[0])
   {
      case '|':
      {
         type = CssTokenType::MATCH_DASH;
         break;
      }
      case '~':
      {
         type = CssTokenType::MATCH_INCLUDE;
         break;
      }
      case '^':
      {
         type = CssTokenType::MATCH_PREFIX;
         break;
      }
      case '*':
      {
         type = CssTokenType::MATCH_SUBSTRING;
         break;
      }
      case '$':
      {
         type = CssTokenType::MATCH_SUFFIX;
         break;
      }
      default:
         assert(false);

   }

   css = css.substr(2);
   return CssToken(type);
}

CssToken parenClose(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::PAREN_CLOSE);
}

CssToken parenOpen(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::PAREN_OPEN);
}

CssToken semicolon(std::string& css)
{
   css = css.substr(1);
   return CssToken(CssTokenType::SEMICOLON);
}

CssToken stringTok(std::string& css)
{
   std::string quoteChar = css.substr(0, 1);
   std::string regex = "^([" + quoteChar + "].*?[^\\\\][" + quoteChar + "])";

   std::string str = cutString(css, regex);
   if (str.empty())
   {
      str = css;
      css = "";
      return CssToken(CssTokenType::STRING_BAD, str);
   }

   return CssToken(CssTokenType::STRING, str);
}

CssToken uniRange(std::string& css)
{
   return CssToken(CssTokenType::UNI_RANGE, cutString(css, "^([uU]\\+[a-fA-F\\d?]{1,6}(?:-[a-fA-F\\d]{1,6})?)"));
}

CssToken url(std::string& css)
{
   std::string val = cutString(css, "^([(].*?(?:[^\\\\][)]|$))");
   if (val.empty())
   {
      val = css;
      css = "";
      return CssToken(CssTokenType::URL_BAD, val);
   }

   boost::trim(val);
   return CssToken(CssTokenType::URL, val);
}

CssToken whitespace(std::string& css)
{
   boost::trim_left(css);
   return CssToken(CssTokenType::WHITESPACE);
}

CssToken identLike(std::string& css)
{
   std::string name = consumeName(css);
   if (!css.empty() && (css[0] == '('))
   {
      if (name == "url")
         return url(css);

      css = css.substr(1);
      return CssToken(CssTokenType::FUNCTION, name);
   }

   return CssToken(CssTokenType::IDENT, name);
}

CssToken numberLike(std::string& css)
{
   std::string num = cutString(css, "^([+-]?\\d*\\.?\\d+(?:[eE][+-]?\\d+)?");
   if (startsWithIdent(css))
   {
      num += consumeName(css);
      return CssToken(CssTokenType::DIM, num);
   }
   else if (!css.empty() && (css[0] == '%'))
   {
      css = css.substr(1);
      return CssToken(CssTokenType::PERCENT, num + "%");
   }

   return CssToken(CssTokenType::NUMBER, num);
}

// CSS Tokenization ====================================================================================================
CssToken nextToken(std::string& css)
{
   assert(!css.empty());

   if (std::isspace(css[0]))
      return whitespace(css);
   if ((css[0] == '"') || (css[0] == '\''))
      return stringTok(css);
   if ((css[0] == '#') && (css.size() > 1) && (startsWithNameCodePoint(&css[1]) || startsWithEscape(&css[1])))
      return hash(css);
   if (matches(css, "^[$*^|~]="))
      return match(css);
   if (css[0] == '(')
      return parenOpen(css);
   if (css[0] == ')')
      return parenClose(css);
   if (css[0] == ',')
      return comma(css);
   if (matches(css, "^/\\*"))
      return comment(css);
   if (css[0] == ':')
      return colon(css);
   if (css[0] == ';')
      return semicolon(css);
   if (matches(css, "-->"))
      return CDClose(css);
   if (matches(css, "^<!--"))
      return CDOpen(css);
   if ((css[0] == '@') && (css.size() > 1) && (startsWithIdent(&css[1])))
      return at(css);
   if (css[0] == '[')
      return bracketOpen(css);
   if (css[0] == ']')
      return bracketClose(css);
   if (css[0] == '{')
      return braceOpen(css);
   if (css[0] == '}')
      return braceClose(css);
   if (matches(css, "^[uU]\\+[a-fA-F\\d?]"))
      return uniRange(css);
   if ((css[0] == '|') && (css.size() > 1) && (css[1] == '|'))
      return column(css);
   if (matches(css, "^[+-]?\\.?\\d"))
      return numberLike(css);
   if (startsWithIdent(css))
      return identLike(css);

   return delim(css);
}

std::queue<CssToken> tokenizeCss(std::string& css)
{
   std::queue<CssToken> tokens;
   while (!css.empty())
      tokens.push(nextToken(css));

   return tokens;
}

// Parsing functions ====================================================================================================
inline void addIndents(unsigned int indentLevel, std::string* pStr)
{
   // Two spaces per indentation.
   for (unsigned int i = 0; i < indentLevel; ++i)
      *pStr += "  ";
}

Error consumeComponent(std::queue<CssToken>& tokens, unsigned int indentLevel, CssComponent* pComponent);
Error consumeFunction(std::queue<CssToken>& tokens, unsigned int indentLevel, CssFunction* pFunction);
Error consumeBlock(std::queue<CssToken>& tokens, unsigned int indentLevel, CssBlock* pBlock);
Error consumeQualifiedRule(std::queue<CssToken>& tokens, unsigned int indentLevel, CssRule* pQualifiedRule);
Error consumeAtRule(std::queue<CssToken>& tokens, unsigned int indentLevel, CssRule* pAtRule);

Error consumeComponent(std::queue<CssToken>& tokens, unsigned int indentLevel, CssComponent* pComponent)
{
   // This can't start with a comment because it would have been consumed before calling this, and we only
   // directly consume one token here, so no comment handling necessary.
   CssComponent component;
   switch (tokens.front().Type)
   {
      case CssTokenType::BRACE_OPEN:
      case CssTokenType::BRACKET_OPEN:
      case CssTokenType::PAREN_OPEN:
      {
         CssBlock block;
         Error error = consumeBlock(tokens, indentLevel, &block);
         if (error)
            return error;

         component.Value = block;
         *pComponent = component;
         return Success();
      }
      case CssTokenType::FUNCTION:
      {
         CssFunction function;
         Error error = consumeFunction(tokens, indentLevel, &function);
         if (error)
            return error;

         component.Value = function;
         *pComponent = component;
         return Success();
      }
      default:
      {
         component.Value = tokens.front().Value;
         tokens.pop();
         *pComponent = component;
         return Success();
      }
   }
}

Error consumeFunction(std::queue<CssToken>& tokens, unsigned int indentLevel, CssFunction* pFunction)
{
   CssFunction function;
   function.Name = tokens.front().Value;
   tokens.pop();

   std::string comment;
   while (!tokens.empty())
   {
      switch (tokens.front().Type)
      {
         case CssTokenType::COMMENT:
         {
            comment += "\n";
            addIndents(indentLevel, &comment);
            comment += tokens.front().Value;
            tokens.pop();
            break;
         }
         case CssTokenType::PAREN_CLOSE:
         {
            function.TrailingComment = comment;
            *pFunction = function;
            return Success();
         }
         default:
         {
            CssComponent component;
            Error error = consumeComponent(tokens, indentLevel, &component);
            if (error)
               return error;

            component.LeadingComment = comment;
            comment.clear();
            function.Components.push_back(component);
            break;
         }
      }
   }

   function.TrailingComment = comment;
   *pFunction = function;
   return Success();
}

Error consumeBlock(std::queue<CssToken>& tokens, unsigned int indentLevel, CssBlock* pBlock)
{
   // Increment the indent level for the inside of the block.
   ++indentLevel;

   CssBlock block;
   CssTokenType blockEnd;
   switch (tokens.front().Type)
   {
      case CssTokenType::BRACE_OPEN:
      {
         block.Type = CssBlockType::BRACE;
         blockEnd = CssTokenType::BRACE_CLOSE;
         break;
      }
      case CssTokenType::BRACKET_OPEN:
      {
         block.Type = CssBlockType::BRACKET;
         blockEnd = CssTokenType::BRACKET_CLOSE;
         break;
      }
      case CssTokenType::PAREN_OPEN:
      {
         block.Type = CssBlockType::PAREN;
         blockEnd = CssTokenType::PAREN_CLOSE;
         break;
      }
      default:
      {
         assert(false);
         return Error(errc::ParseError, ERROR_LOCATION);
      }
   }
   tokens.pop();

   std::string comment;
   while (!tokens.empty() && (tokens.front().Type != blockEnd))
   {
      switch (tokens.front().Type)
      {
         case CssTokenType::COMMENT:
         {
            comment += "\n";
            addIndents(indentLevel, &comment);
            comment += tokens.front().Value;
            tokens.pop();
            break;
         }
         default:
         {
            CssComponent component;
            Error error = consumeComponent(tokens, indentLevel, &component);
            if (error)
               return error;

            component.LeadingComment = comment;
            comment.clear();

            block.Components.push_back(component);
            break;
         }
      }
   }

   // Pop the block end, if the tokens are not empty.
   if (!tokens.empty())
      tokens.pop();

   block.TrailingComment = comment;
   *pBlock = block;
   return Success();
}

Error consumeQualifiedRule(std::queue<CssToken>& tokens, unsigned int indentLevel, CssRule* pQualifiedRule)
{
   CssRule qRule;
   std::string comment;

   while (!tokens.empty())
   {
      switch (tokens.front().Type)
      {
         case CssTokenType::COMMENT:
         {
            comment += "\n";
            addIndents(indentLevel, &comment);
            comment += tokens.front().Value;
            tokens.pop();
            break;
         }
         case CssTokenType::BRACE_OPEN:
         {
            Error error = consumeBlock(tokens, indentLevel, &qRule.Block);
            if (error)
               return error;

            qRule.Block.LeadingComment = comment;
            *pQualifiedRule = qRule;
            return Success();
         }
         default:
         {
            CssComponent component;
            Error error = consumeComponent(tokens, indentLevel, &component);
            if (error)
               return error;

            component.LeadingComment = comment;
            comment.clear();
            qRule.Prelude.push_back(component);
            break;
         }
      }
   }

   return Error(errc::ParseError, ERROR_LOCATION);
}

Error consumeAtRule(std::queue<CssToken>& tokens, unsigned int indentLevel, CssRule* pAtRule)
{
   CssRule atRule;
   assert(tokens.front().Type == CssTokenType::AT);
   atRule.Name = tokens.front().Value;
   tokens.pop();

   std::string comment;
   while (!tokens.empty())
   {
      switch (tokens.front().Type)
      {
         // The comment cannot come before the name because this function is only called when the front token is an at
         // token, which determines the name. This comment will either be a leading comment to the prelude or a leading
         // comment to the block.
         case CssTokenType::COMMENT:
         {
            comment += "\n";
            addIndents(indentLevel, &comment);
            comment += tokens.front().Value;
            tokens.pop();
            break;
         }
         case CssTokenType::SEMICOLON:
         {
            tokens.pop();
            atRule.TrailingComment = comment;
            *pAtRule = atRule;
            return Success();
         }
         case CssTokenType::BRACE_OPEN:
         {
            Error error = consumeBlock(tokens, indentLevel, &atRule.Block);
            if (error)
               return error;

            atRule.Block.LeadingComment = comment;
            *pAtRule = atRule;
            return Success();
         }
         default:
         {
            CssComponent component;
            Error error = consumeComponent(tokens, indentLevel, &component);
            if (error)
               return error;

            component.LeadingComment = comment;
            comment.clear();

            atRule.Prelude.push_back(component);
            break;
         }
      }
   }

   atRule.TrailingComment = comment;
   *pAtRule = atRule;
   return Success();
}

} // anonymous namespace

Error parseCss(const std::string& css, std::vector<CssRule>* pStylesheet)
{
   std::string cssCopy = css;
   std::queue<CssToken> tokens = tokenizeCss(cssCopy);
   std::vector<CssRule> rules;
   std::string comment;
   while (!tokens.empty())
   {
      CssRule rule;
      Error error;
      switch(tokens.front().Type)
      {
         case CssTokenType::COMMENT:
         {
            comment += ((rules.empty() && (comment.empty())) ? "" : "\n") + tokens.front().Value;
            tokens.pop();
            break;
         }
         case CssTokenType::WHITESPACE:
         case CssTokenType::CD_CLOSE:
         case CssTokenType::CD_OPEN:
         {
            tokens.pop();
            break;
         }
         case CssTokenType::AT:
         {
            error = consumeAtRule(tokens, 0, &rule);
            if (error)
               return error;

            rule.LeadingComment = comment;
            comment.clear();

            rules.push_back(rule);
            break;
         }
         default:
         {
            error = consumeQualifiedRule(tokens, 0, &rule);
            if (error)
               return error;

            rule.LeadingComment = comment;
            comment.clear();

            rules.push_back(rule);
            break;
         }
      }
   }


   *pStylesheet = rules;
   return Success();
}

CssComponentValue::CssComponentValue(const CssComponentValue& other)
{
   *this = other;
}

CssComponentValue::~CssComponentValue()
{
   clear();
}

CssComponentValue& CssComponentValue::operator=(const CssComponentValue& other)
{
   if (other.strValue_)
      strValue_ = new std::string(*other.strValue_);
   if (other.blockValue_)
      blockValue_ = new CssBlock(*other.blockValue_);
   if (other.functionValue_)
      functionValue_ = new CssFunction(*other.functionValue_);

   assert((isString() && !isBlock() && !isFunction()) ||
          (!isString() && isBlock() && !isFunction()) ||
          (!isString() && !isBlock() && !isFunction()) ||
          (!isString() && !isBlock() && !isFunction()));

   return *this;
}

CssComponentValue& CssComponentValue::operator=(const std::string& value)
{
   clear();
   strValue_ = new std::string(value);
   return *this;
}

CssComponentValue& CssComponentValue::operator=(const CssBlock& value)
{
   clear();
   blockValue_ = new CssBlock(value);
   return *this;
}

CssComponentValue& CssComponentValue::operator=(const CssFunction& value)
{
   clear();
   functionValue_ = new CssFunction(value);
   return *this;
}

bool CssComponentValue::isString() const
{
   return !!strValue_;
}

bool CssComponentValue::isBlock() const
{
   return !!blockValue_;
}

bool CssComponentValue::isFunction() const
{
   return !!functionValue_;
}

std::string& CssComponentValue::getStringValue()
{
   assert(isString());
   return *strValue_;
}

CssBlock& CssComponentValue::getBlockValue()
{
   assert(isBlock());
   return *blockValue_;
}

CssFunction& CssComponentValue::getFunctionValue()
{
   assert(isFunction());
   return *functionValue_;
}

const std::string& CssComponentValue::getStringValue() const
{
   assert(isString());
   return *strValue_;
}

const CssBlock& CssComponentValue::getBlockValue() const
{
   assert(isBlock());
   return *blockValue_;
}

const CssFunction& CssComponentValue::getFunctionValue() const
{
   assert(isFunction());
   return *functionValue_;
}

void CssComponentValue::clear()
{
   delete strValue_;
   delete blockValue_;
   delete functionValue_;
   strValue_ = nullptr;
   blockValue_ = nullptr;
   functionValue_ = nullptr;
}

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
