/*
 * SlideRenderer.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


#include "SlideRenderer.hpp"

#include <iostream>
#include <sstream>

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/markdown/Markdown.hpp>

#include "SlideParser.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {


} // anonymous namespace


Error renderSlides(const SlideDeck& slideDeck,
                   std::string* pSlides,
                   std::string* pUserErrorMsg)
{
   // setup markdown options
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;

   // render the slides to HTML
   std::ostringstream ostr;
   for (std::vector<Slide>::const_iterator it = slideDeck.begin();
        it != slideDeck.end(); ++it)
   {
      ostr << "<section>" << std::endl;
      if (it->showTitle())
         ostr << "<h3>" << it->title() << "</h3>";

      std::string htmlContent;
      Error error = markdown::markdownToHTML(it->content(),
                                             extensions,
                                             htmlOptions,
                                             &htmlContent);
      if (error)
      {
         *pUserErrorMsg = error.summary();
         return error;
      }

      ostr << htmlContent << std::endl;

      ostr << "</section>" << std::endl;
   }

   *pSlides = ostr.str();
   return Success();


}


} // namespace learning
} // namespace modules
} // namesapce session

