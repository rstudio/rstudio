/*
 * SlideMediaRenderer.cpp
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


#include "SlideMediaRenderer.hpp"

#include <iostream>
#include <sstream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/json/Json.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

namespace {

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

std::string atCommandsAsJsonArray(const std::vector<AtCommand>& atCommands)
{
   json::Array cmdsArray;
   BOOST_FOREACH(const AtCommand atCmd, atCommands)
   {
      cmdsArray.push_back(atCmd.asJson());
   }

   std::ostringstream ostr;
   json::write(cmdsArray, ostr);
   return ostr.str();
}

} // anonymous namespace

void renderMedia(const std::string& type,
                 int slideNumber,
                 const FilePath& baseDir,
                 const std::string& fileName,
                 const std::vector<AtCommand>& atCommands,
                 std::ostream& os,
                 std::vector<std::string>* pInitActions,
                 std::vector<std::string>* pSlideActions)
{
   // only do this in server mode
   if (session::options().programMode() != kSessionProgramModeServer)
      return;

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
         "<%1% id=\"%2%\" controls preload=\"auto\" data-ignore>\n"
         "  %3%"
         "  The &lt;%1%&gt; tag is not supported in this context"
         " (however the %1% will still play correctly when the presentation"
         " is shown within a browser that supports the %1% tag).\n"
         "</%1%>\n");

   os << boost::str(fmt % type % mediaId % sources);

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



} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

