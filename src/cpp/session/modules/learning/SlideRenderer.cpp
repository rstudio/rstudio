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

#include <core/json/Json.hpp>

#include <core/markdown/Markdown.hpp>

#include "SlideParser.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace learning {

namespace {


json::Object commandAsJson(const Command& command)
{
   json::Object commandJson;
   commandJson["name"] = command.name();
   commandJson["params"] = command.params();
   return commandJson;
}

std::string commandsAsJsonArray(const Slide& slide)
{
   json::Array commandsJsonArray;

   std::vector<Command> commands = slide.commands();
   BOOST_FOREACH(const Command& command, commands)
   {
      commandsJsonArray.push_back(commandAsJson(command));
   }

   std::ostringstream ostr;
   json::write(commandsJsonArray, ostr);
   return ostr.str();
}

std::string atCommandsAsJsonArray(const std::vector<AtCommand>& atCommands)
{
   json::Array cmdsArray;

   BOOST_FOREACH(const AtCommand atCmd, atCommands)
   {
      json::Object obj;
      obj["at"] = atCmd.seconds();
      obj["command"] = commandAsJson(atCmd.command());
      cmdsArray.push_back(obj);
   }

   std::ostringstream ostr;
   json::write(cmdsArray, ostr);
   return ostr.str();
}

void renderMedia(const std::string& type,
                 const std::string& format,
                 int slideNumber,
                 const std::string& fileName,
                 const std::vector<AtCommand>& atCommands,
                 std::ostream& os,
                 std::vector<std::string>* pInitActions,
                 std::vector<std::string>* pSlideActions)
{
   boost::format fmt("slide%1%%2%");
   std::string mediaId = boost::str(fmt % slideNumber % type);
   fmt = boost::format(
         "<%1% id=\"%2%\" controls preload=\"none\"\n"
         "  <source src=\"%3%\" type=\"%1%/%4%\"/>\n"
         "  Your browser does not support the %1% tag.\n"
         "</%1%>\n");

   os << boost::str(fmt % type % mediaId % fileName % format) << std::endl;

   // define manager during initialization
   std::string atCmds = atCommandsAsJsonArray(atCommands);
   fmt = boost::format("%1%Manager");
   std::string managerId = boost::str(fmt % mediaId);
   fmt = boost::format("%1% = mediaManager(%2%, %3%)");
   pInitActions->push_back(boost::str(fmt % managerId % mediaId % atCmds));

   // add video autoplay action
   fmt = boost::format("%1%.play()");
   pSlideActions->push_back(boost::str(fmt % managerId));
}

} // anonymous namespace


Error renderSlides(const SlideDeck& slideDeck,
                   std::string* pSlides,
                   std::string* pInitActions,
                   std::string* pSlideActions)
{
   // setup markdown options
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;

   // render the slides to HTML and slide commands to case statements
   std::ostringstream ostr, ostrInitActions, ostrSlideActions;
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
         return error;

      // render content
      ostr << htmlContent << std::endl;

      // setup a vector of js actions to take on deck initialization
      std::vector<std::string> initActions;

      // setup a vector of js actions to take when the slide loads
      // (we always take the action of adding any embedded commands)
      std::vector<std::string> slideActions;
      slideActions.push_back("cmds = " + commandsAsJsonArray(*it));

      // get at commands
      std::vector<AtCommand> atCommands = it->atCommands();

      // render video if specified
      std::string video = it->video();
      if (!video.empty())
      {
         renderMedia("video",
                     "mp4",
                     slideNumber,
                     video,
                     atCommands,
                     ostr,
                     &initActions,
                     &slideActions);
      }

      // render audio if specified
      std::string audio = it->audio();
      if (!audio.empty())
      {
         renderMedia("audio",
                     "mpeg",
                     slideNumber,
                     audio,
                     atCommands,
                     ostr,
                     &initActions,
                     &slideActions);
      }

      ostr << "</section>" << std::endl;

      // javascript actions to take on slide deck init
      BOOST_FOREACH(const std::string& jsAction, initActions)
      {
         ostrInitActions <<  jsAction << ";" << std::endl;
      }

      // javascript actions to take on slide load
      ostrSlideActions << cmdPad << "case " << slideNumber << ":" << std::endl;
      BOOST_FOREACH(const std::string& jsAction, slideActions)
      {
         ostrSlideActions << cmdPad << "  " << jsAction << ";" << std::endl;
      }
      ostrSlideActions << std::endl << cmdPad << "  break;" << std::endl;

      // increment slide number
      slideNumber++;
   }

   *pSlides = ostr.str();
   *pInitActions = ostrInitActions.str();
   *pSlideActions = ostrSlideActions.str();
   return Success();
}


} // namespace learning
} // namespace modules
} // namesapce session

