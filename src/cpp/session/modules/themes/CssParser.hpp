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

#ifndef SESSION_THEMES_CSS_PARSER_HPP
#define SESSION_THEMES_CSS_PARSER_HPP

#include <boost/variant.hpp>

#include <core/Error.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

enum class CssBlockType
{
   BRACE,
   BRACKET,
   PAREN
};

struct CssBlock;
struct CssFunction;

class CssComponentValue
{
public:
   CssComponentValue() : strValue_(nullptr), blockValue_(nullptr), functionValue_(nullptr) { };
   CssComponentValue(const CssComponentValue& other);
   ~CssComponentValue();

   CssComponentValue& operator=(const CssComponentValue& other);
   CssComponentValue& operator=(const std::string& value);
   CssComponentValue& operator=(const CssBlock& value);
   CssComponentValue& operator=(const CssFunction& value);

   bool isString() const;
   bool isBlock() const;
   bool isFunction() const;

   std::string& getStringValue();
   CssBlock& getBlockValue();
   CssFunction& getFunctionValue();
   const std::string& getStringValue() const;
   const CssBlock& getBlockValue() const;
   const CssFunction& getFunctionValue() const;

private:
   void clear();

   std::string* strValue_;
   CssBlock* blockValue_;
   CssFunction* functionValue_;
};

struct CssComponent
{
   std::string LeadingComment;
   std::string TrailingComment;
   CssComponentValue Value;
};

struct CssBlock
{
   std::string LeadingComment;
   std::string TrailingComment;
   CssBlockType Type;
   std::vector<CssComponent> Components;
};

struct CssFunction
{
   std::string LeadingComment;
   std::string TrailingComment;
   std::string Name;
   std::vector<CssComponent> Components;
};

struct CssRule
{
   std::string LeadingComment;
   std::string TrailingComment;
   std::string Name;
   std::vector<CssComponent> Prelude;
   CssBlock Block;
};

core::Error parseCss(const std::string& css, std::vector<CssRule>* pStylesheet);

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
