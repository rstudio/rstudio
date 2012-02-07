/*
 * TexMagicComment.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/tex/TexMagicComment.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <boost/foreach.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/regex.hpp>

namespace core {
namespace tex {

Error parseMagicComments(const FilePath& texFile,
                         TexMagicComments* pComments)
{
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(texFile, &lines);
   if (error)
      return error;

   boost::regex mcRegex("%\\s*!(\\w+)\\s+(\\w+)\\s*=\\s*(\\w+)");
   BOOST_FOREACH(std::string line, lines)
   {
      boost::algorithm::trim(line);
      if (line.empty())
      {
         continue;
      }
      else if (boost::algorithm::starts_with(line, "%"))
      {
         boost::smatch match;
         if (regex_match(line, match, mcRegex))
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



