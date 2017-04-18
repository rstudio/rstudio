/*
 * RMarkdownPresentation.cpp
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

#include "RMarkdownPresentation.hpp"

#include <boost/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace rmarkdown {
namespace presentation {

namespace {

struct SlideNavigationItem
{
   SlideNavigationItem(const std::string& title,
                       int indent,
                       int index,
                       int line)
      : title(title), indent(indent), index(index), line(line)
   {
   }

   std::string title;
   int indent;
   int index;
   int line;
};

json::Value itemAsJson(const SlideNavigationItem& item)
{
   json::Object slideJson;
   slideJson["title"] = item.title;
   slideJson["indent"] = item.indent;
   slideJson["index"] = item.index;
   slideJson["line"] = item.line;
   return slideJson;
}

} // anonymous namespace


void ammendResults(const std::string& formatName,
                   core::FilePath& targetFile,
                   int sourceLine,
                   json::Object* pResultJson)
{
   // provide slide navigation for ioslides and beamer
   if (formatName != "ioslides_presentation" &&
       formatName != "slidy_presentation" &&
       formatName != "beamer_presentation")
   {
      return;
   }

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
   int totalSlides = 0;
   std::vector<SlideNavigationItem> slideNavigationItems;
   bool inCode = false;
   bool inYaml = false;
   bool haveTitle = false;
   boost::regex reYaml("^\\-{3}\\s*$");
   boost::regex reTitle("^title\\:(.*)$");
   boost::regex reCode("^`{3,}.*$");
   boost::regex reTitledSlide("^#(#)?([^#][^|\\{]+).*$");
   boost::regex reUntitledSlide("^(\\-{3,}|\\*{3,})\\w*$");
   for (unsigned i = 0; i<lines.size(); i++)
   {
      // alias line
      const std::string& line = lines.at(i);

      // toggle code state
      if (regex_utils::search(line, reCode))
         inCode = !inCode;

      // bail if we are in code
      if (inCode)
         continue;

      // look for a title if we don't have one
      if (!haveTitle || inYaml)
      {
         if (regex_utils::search(line, reYaml))
         {
            if (!inYaml)
            {
               inYaml = true;
            }
            else if (inYaml)
            {
               // bail if there was no title
               if (!haveTitle)
               {
                  break;
               }
               else
               {
                  inYaml = false;
               }
            }
         }

         // titles only valid in yaml
         if (inYaml)
         {
            boost::smatch match;
            if (regex_utils::search(line, match, reTitle))
            {
               std::string title = match[1];
               boost::algorithm::trim(title);
               string_utils::stripQuotes(&title);
               if (title.empty())
                  title = "Untitled Slide";
               SlideNavigationItem item(title, 0, totalSlides++, 1);
               slideNavigationItems.push_back(item);
               haveTitle = true;
            }
         }
      }
      // if we already have the title look for slides
      else
      {
         // titled slides
         boost::smatch match;
         if (regex_utils::search(line, match, reTitledSlide))
         {
            std::string title = match[2];
            boost::algorithm::trim(title);
            if (title.empty())
               title = "Untitled Slide";

            int indent = std::string(match[1]).empty() ? 0 : 1;
            SlideNavigationItem item(title, indent, totalSlides++, i+1);
            slideNavigationItems.push_back(item);
         }
         // untitled slides
         else if (regex_utils::search(line, reUntitledSlide))
         {
            SlideNavigationItem item("Untitled Slide", 1, totalSlides++, i+1);
            slideNavigationItems.push_back(item);
         }
      }
   }

   // did we find slides?
   if (totalSlides > 0)
   {
      // determine which slide the cursor is on
      int previewSlide = 1;
      for (int i = (slideNavigationItems.size()-1); i>=0; i--)
      {
         const SlideNavigationItem& item = slideNavigationItems.at(i);
         if (sourceLine >= item.line)
         {
            previewSlide = item.index + 1;
            break;
         }
      }

      // return as json
      resultJson["preview_slide"] = previewSlide;
      json::Array jsonSlideNavigationItems;
      std::transform(slideNavigationItems.begin(),
                     slideNavigationItems.end(),
                     std::back_inserter(jsonSlideNavigationItems),
                     itemAsJson);
      json::Object jsonSlideNavigation;
      jsonSlideNavigation["total_slides"] = totalSlides;
      jsonSlideNavigation["anchor_parens"] = formatName == "slidy_presentation";
      jsonSlideNavigation["items"] = jsonSlideNavigationItems;
      resultJson["slide_navigation"] = jsonSlideNavigation;
   }
}


} // namespace presentation
} // namepsace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

