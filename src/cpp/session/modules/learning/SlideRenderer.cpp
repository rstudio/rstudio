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


Error renderSlides(const FilePath& slidesDefPath,
                   std::string* pSlides,
                   std::string* pUserErrorMsg)
{
   // parse slide definition
   std::vector<Slide> slides;
   Error error = learning::readSlides(slidesDefPath, &slides, pUserErrorMsg);
   if (error)
      return error;

   // setup markdown options
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;

   // render the slides to HTML
   std::ostringstream ostr;
   BOOST_FOREACH(const Slide& slide, slides)
   {
      ostr << "<section>" << std::endl;
      ostr << "<h3>" << slide.title() << "</h3>";

      std::string htmlContent;
      Error error = markdown::markdownToHTML(slide.content(),
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

