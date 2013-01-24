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
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/markdown/Markdown.hpp>

#include "SlideParser.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {


void renderMedia(const std::string& type,
                 const std::string& format,
                 int slideNumber,
                 const std::string& fileName,
                 std::ostream& os,
                 std::vector<std::string>* pJsActions)
{
   boost::format fmt("slide%1%%2%");
   std::string mediaId = boost::str(fmt % slideNumber % type);
   fmt = boost::format(
         "<script type=\"text/javascript\">\n"
         "  function %2%Updated() {\n"
         "       \n"
         "  }\n"
         "  function %2%Ended() {\n"
         "     %2%.load();\n"
         "  }\n"
         "</script>\n"

         "<%1% id=\"%2%\" controls preload=\"none\"\n"
         "       ontimeupdate=\"%2%Updated();\"\n"
         "       onended=\"%2%Ended();\">\n"
         "  <source src=\"%3%\" type=\"%1%/%4%\">\n"
         "  Your browser does not support the %1% tag.\n"
         "</%1%>\n");

   os << boost::str(fmt % type % mediaId % fileName % format) << std::endl;

   // add video autoplay action
   fmt = boost::format("if (%1%.ended) %1%.load(); %1%.play();");
   pJsActions->push_back(boost::str(fmt % mediaId));
}


} // anonymous namespace


Error renderSlides(const SlideDeck& slideDeck,
                   std::string* pSlides,
                   std::string* pSlideActions,
                   std::string* pUserErrorMsg)
{
   // setup markdown options
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;

   // render the slides to HTML and slide commands to case statements
   std::ostringstream ostr, ostrActions;
   std::string cmdPad(8, ' ');
   int slideNumber = 0;
   for (std::vector<Slide>::const_iterator it = slideDeck.begin();
        it != slideDeck.end(); ++it)
   {
      // slide
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

      // render content
      ostr << htmlContent << std::endl;

      // setup a vector of js actions to take when the slide loads
      // (we always take the action of adding any embedded commands)
      std::vector<std::string> jsActions;
      jsActions.push_back("cmds = " + it->commandsJsArray());

      // render video if specified
      std::string video = it->video();
      if (!video.empty())
         renderMedia("video", "mp4", slideNumber, video, ostr, &jsActions);

      // render audio if specified
      std::string audio = it->audio();
      if (!audio.empty())
         renderMedia("audio", "mpeg", slideNumber, audio, ostr, &jsActions);

      ostr << "</section>" << std::endl;

      // javascript actions to take on slide load
      ostrActions << cmdPad << "case " << slideNumber << ":" << std::endl;
      BOOST_FOREACH(const std::string& jsAction, jsActions)
      {
         ostrActions << cmdPad << "  " << jsAction << ";" << std::endl;
      }
      ostrActions << std::endl << cmdPad << "  break;" << std::endl;

      // increment slide number
      slideNumber++;
   }

   *pSlides = ostr.str();
   *pSlideActions = ostrActions.str();
   return Success();


}


} // namespace learning
} // namespace modules
} // namesapce session

