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
#include <boost/algorithm/string/predicate.hpp>


#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <core/json/Json.hpp>

#include <core/markdown/Markdown.hpp>

#include <session/SessionModuleContext.hpp>

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

class MediaSource
{
public:
   MediaSource(const std::string& src, const std::string& type)
      : src_(src), type_(type)
   {
   }

   std::string asTag() const
   {
      boost::format fmt("<source src=\"%1%\" type=\"%2%\" />");
      return boost::str(fmt % src_ % type_);
   }

private:
   std::string src_;
   std::string type_;
};

std::vector<MediaSource> discoverMediaSources(
                              const std::string& type,
                              const FilePath& baseDir,
                              const std::string& filename)
{
   // build list of formats based on type
   std::vector<std::string> formats;
   if (type == "video")
   {
      formats.push_back("mp4");
      formats.push_back("webm");
      formats.push_back("ogv");
   }
   else if (type == "audio")
   {
      formats.push_back("mp3");
      formats.push_back("wav");
      formats.push_back("oga");
      formats.push_back("ogg");
   }

   std::vector<MediaSource> sources;
   FilePath mediaFile = baseDir.complete(filename);
   if (mediaFile.exists())
   {
      // get the filename without extension
      std::string stem = mediaFile.stem();
      BOOST_FOREACH(std::string fmt, formats)
      {
         FilePath targetPath = mediaFile.parent().complete(stem + "." + fmt);
         if (targetPath.exists())
         {
            std::string file = targetPath.relativePath(baseDir);
            if (boost::algorithm::starts_with(fmt, "og"))
               fmt = "ogg";
            sources.push_back(MediaSource(file, type + "/" + fmt));
         }
      }
   }
   else
   {
      module_context::consoleWriteError("Media file " +
                                        mediaFile.absolutePath() +
                                        " does not exist");
   }

   return sources;
}

void renderMedia(const std::string& type,
                 int slideNumber,
                 const FilePath& baseDir,
                 const std::string& fileName,
                 const std::vector<AtCommand>& atCommands,
                 std::ostream& os,
                 std::vector<std::string>* pInitActions,
                 std::vector<std::string>* pSlideActions)
{
   // discover media sources
   std::vector<MediaSource> mediaSources = discoverMediaSources(type,
                                                                baseDir,
                                                                fileName);
   std::string sources;
   BOOST_FOREACH(const MediaSource& source, mediaSources)
   {
      sources += (source.asTag() + "\n");
   }


   boost::format fmt("slide%1%%2%");
   std::string mediaId = boost::str(fmt % slideNumber % type);
   fmt = boost::format(
         "<%1% id=\"%2%\" controls preload=\"none\">\n"
         "  %3%"
         "  Your browser does not support the %1% tag.\n"
         "</%1%>\n");

   os << boost::str(fmt % type % mediaId % sources) << std::endl;

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
                   std::string* pRevealConfig,
                   std::string* pInitActions,
                   std::string* pSlideActions)
{
   // setup markdown options
   markdown::Extensions extensions;
   markdown::HTMLOptions htmlOptions;

   // render the slides to HTML and slide commands to case statements
   std::ostringstream ostr, ostrRevealConfig, ostrInitActions, ostrSlideActions;

   // first the preamble
   ostr << slideDeck.preamble() << std::endl;

   // now the slides
   std::string cmdPad(8, ' ');
   int slideNumber = 0;
   for (size_t i=0; i<slideDeck.slides().size(); i++)
   {
      // slide
      const Slide& slide = slideDeck.slides().at(i);

      ostr << "<section";
      if (!slide.state().empty())
         ostr << " data-state=\"" << slide.state() <<  "\"";
      ostr << ">" << std::endl;
      if (slide.showTitle())
         ostr << "<h3>" << slide.title() << "</h3>";

      std::string htmlContent;
      Error error = markdown::markdownToHTML(slide.content(),
                                             extensions,
                                             htmlOptions,
                                             &htmlContent);
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

   *pSlides = ostr.str();
   *pRevealConfig = ostrRevealConfig.str();
   *pInitActions = ostrInitActions.str();
   *pSlideActions = ostrSlideActions.str();
   return Success();
}


} // namespace learning
} // namespace modules
} // namesapce session

