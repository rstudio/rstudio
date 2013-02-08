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

#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/json/Json.hpp>

#include <core/markdown/Markdown.hpp>

#include <session/SessionModuleContext.hpp>

#include "SlideParser.hpp"
#include "SlideMediaRenderer.hpp"
#include "SlideNavigationList.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace presentation {

namespace {

std::string commandsAsJsonArray(const Slide& slide)
{
   json::Array commandsJsonArray;

   std::vector<Command> commands = slide.commands();
   BOOST_FOREACH(const Command& command, commands)
   {
      commandsJsonArray.push_back(command.asJson());
   }

   std::ostringstream ostr;
   json::write(commandsJsonArray, ostr);
   return ostr.str();
}


Error slideMarkdownToHtml(const Slide& slide, std::string* pHTML)
{
   // setup markdown options
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;

   return markdown::markdownToHTML(slide.content(),
                                   extensions,
                                   htmlOptions,
                                   pHTML);

}

} // anonymous namespace


Error renderSlides(const SlideDeck& slideDeck,
                   std::string* pSlides,
                   std::string* pRevealConfig,
                   std::string* pInitActions,
                   std::string* pSlideActions)
{
   // render the slides to HTML and slide commands to case statements
   std::ostringstream ostr, ostrRevealConfig, ostrInitActions, ostrSlideActions;

   // track json version of slide list
   SlideNavigationList navigationList;

   // now the slides
   std::string cmdPad(8, ' ');
   int slideNumber = 0;
   for (size_t i=0; i<slideDeck.slides().size(); i++)
   {
      // slide
      const Slide& slide = slideDeck.slides().at(i);

      // is this the first slide?
      bool isFirstSlide = (i == 0);

      // if this is the first slide then set the navigation type from it
      if (isFirstSlide)
         navigationList.setNavigationType(slide.navigation());

      // track in list
      navigationList.add(slide);

      ostr << "<section";
      if (!slide.id().empty())
         ostr << " id=\"" << slide.id() << "\"";

      // get the slide type
      std::string type = isFirstSlide ? "section" : slide.type();

      // add the state if there is a type
      if (!type.empty())
         ostr << " data-state=\"" << type <<  "\"";

      // end section tag
      ostr << ">" << std::endl;

      // show the title with the appropriate header
      if (isFirstSlide || slide.showTitle())
      {
         std::string hTag;
         if (isFirstSlide)
            hTag = "h1";
         else if (type == "section" || type == "sub-section")
            hTag = "h2";
         else
            hTag = "h3";

         ostr << "<" << hTag << ">"
              << string_utils::htmlEscape(slide.title())
              << "</" << hTag << ">";
      }

      // if this is slide one then render author and date if they are included
      if (isFirstSlide)
      {
         ostr << "<p>";
         if (!slide.author().empty())
         {
            ostr << string_utils::htmlEscape(slide.author());
            if (!slide.date().empty())
               ostr << "<br/>";
         }
         if (!slide.date().empty())
            ostr << string_utils::htmlEscape(slide.date());

         ostr << "</p>" << std::endl;
      }

      // render markdown
      std::string htmlContent;
      Error error = slideMarkdownToHtml(slide, &htmlContent);
      if (error)
         return error;

      // render content
      ostr << htmlContent << std::endl;

      // setup vectors for reveal config and init actions
      std::vector<std::string> revealConfig;
      std::vector<std::string> initActions;

      // setup a vector of js actions to take when the slide loads
      // (we always take the action of adding any embedded commands)
      std::vector<std::string> slideActions;
      slideActions.push_back("cmds = " + commandsAsJsonArray(slide));

      // get at commands
      std::vector<AtCommand> atCommands = slide.atCommands();

      // render video if specified
      std::string video = slide.video();
      if (!video.empty())
      {
         renderMedia("video",
                     slideNumber,
                     slideDeck.baseDir(),
                     video,
                     atCommands,
                     ostr,
                     &initActions,
                     &slideActions);
      }

      // render audio if specified
      std::string audio = slide.audio();
      if (!audio.empty())
      {
         renderMedia("audio",
                     slideNumber,
                     slideDeck.baseDir(),
                     audio,
                     atCommands,
                     ostr,
                     &initActions,
                     &slideActions);
      }

      ostr << "</section>" << std::endl;

      // reveal config actions
      BOOST_FOREACH(const std::string& config, revealConfig)
      {
         ostrRevealConfig << config << "," << std::endl;
      }

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

   // init slide list as part of actions
   navigationList.complete();
   ostrInitActions << navigationList.asCall() << std::endl;

   *pSlides = ostr.str();
   *pRevealConfig = ostrRevealConfig.str();
   *pInitActions = ostrInitActions.str();
   *pSlideActions = ostrSlideActions.str();
   return Success();
}


} // namespace presentation
} // namespace modules
} // namesapce session

