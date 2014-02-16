/*
 * Presentation.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "Presentation.hpp"

#include <boost/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/FileSerializer.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace rmarkdown {
namespace presentation {

void ammendResults(const std::string& formatName,
                   core::FilePath& targetFile,
                   int sourceLine,
                   json::Object* pResultJson)
{
   // screen for presentation format
   if (!boost::algorithm::ends_with(formatName, "_presentation"))
      return;

   // alias for nicer map syntax
   json::Object& resultJson = *pResultJson;

   // read the input file
   std::vector<std::string> lines;
   Error error = core::readStringVectorFromFile(targetFile, &lines, false);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // scan the input file looking for headers and slide breaks
   std::vector<int> slideBreaks;
   slideBreaks.push_back(1);
   bool inCode = false;
   boost::regex reCode("^`{3,}.*$");
   boost::regex reSlide("^(##?.*|\\-{4,}\\w*|\\*{4,}\\w*)$");
   for (int i = 0; i<lines.size(); i++)
   {
      if (boost::regex_search(lines.at(i), reCode))
      {
         inCode = !inCode;
      }
      else if (boost::regex_search(lines.at(i), reSlide))
      {
         if (!inCode)
            slideBreaks.push_back(i + 1);
      }
   }

   // determine which slide the user was editing by scanning for
   // the first slide line which is >= the source line then
   // add this as an anchor field
   int slide = slideBreaks.size();
   for (int i = 0; i<slideBreaks.size(); i++)
   {
      if (slideBreaks.at(i) >= sourceLine)
      {
         slide = i;
         break;
      }
   }

   // set slide number and slide breaks
   resultJson["slide_number"] = slide;
   resultJson["slide_breaks"] = json::toJsonArray(slideBreaks);
}


} // namespace presentation
} // namepsace rmarkdown
} // namespace modules
} // namesapce session

