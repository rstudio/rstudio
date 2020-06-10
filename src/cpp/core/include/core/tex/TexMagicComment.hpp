/*
 * TexMagicComment.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_TEX_TEX_MAGIC_COMMENT_HPP
#define CORE_TEX_TEX_MAGIC_COMMENT_HPP

#include <string>
#include <vector>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace tex {
   
class TexMagicComment
{
public:

   TexMagicComment(const std::string& scope,
                   const std::string& variable,
                   const std::string& value)
     : scope_(scope), variable_(variable), value_(value)
   {
   }

   const std::string& scope() const { return scope_; }
   const std::string& variable() const { return variable_; }
   const std::string& value() const { return value_; }

private:
   std::string scope_;
   std::string variable_;
   std::string value_;
};

typedef std::vector<TexMagicComment> TexMagicComments;

Error parseMagicComments(const FilePath& texFile, TexMagicComments* pComments);



} // namespace tex
} // namespace core 
} // namespace rstudio


#endif // CORE_TEX_TEX_MAGIC_COMMENT_HPP

