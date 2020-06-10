/*
 * TexMagicComment.cpp
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

#include <core/tex/TexMagicComment.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/regex.hpp>

namespace rstudio {
namespace core {
namespace tex {

Error parseMagicComments(const FilePath& texFile,
                         TexMagicComments* pComments)
{
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(texFile, &lines);
   if (error)
      return error;

   boost::regex mcRegex("%{1,2}\\s*!(\\w+)\\s+(\\w+)\\s*=\\s*(.*)$");
   for (std::string line : lines)
   {
      boost::algorithm::trim(line);
      if (line.empty())
      {
         continue;
      }
      else if (boost::algorithm::starts_with(line, "%"))
      {
         boost::smatch match;
         if (regex_utils::match(line, match, mcRegex))
         {
            pComments->push_back(
                        TexMagicComment(match[1], match[2], match[3]));
         }
      }
      else
      {
         break;
      }
   }

   return Success();
}

   
} // namespace tex
} // namespace core 
} // namespace rstudio



