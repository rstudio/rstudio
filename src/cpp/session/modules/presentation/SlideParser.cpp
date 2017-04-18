/*
 * SlideParser.cpp
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


#include "SlideParser.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/StringUtils.hpp>
#include <core/text/DcfParser.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

namespace {

struct CompareName
{
  CompareName(const std::string& name) : name_(name) {}
  bool operator()(const Slide::Field& field) const {
    return boost::iequals(name_, field.first);
  }
  private:
    std::string name_;
};

bool isCommandField(const std::string& name)
{
   return boost::iequals(name, "help-doc") ||
          boost::iequals(name, "help-topic") ||
          boost::iequals(name, "source") ||
          boost::iequals(name, "console") ||
          boost::iequals(name, "console-input") ||
          boost::iequals(name, "execute") ||
          boost::iequals(name, "pause");
}

bool isAtCommandField(const std::string& name)
{
   return isCommandField(name) ||
          boost::iequals(name, "pause");
}


bool isValidField(const std::string& name)
{
   return isCommandField(name) ||
          boost::iequals(name, "title") ||
          boost::iequals(name, "author") ||
          boost::iequals(name, "date") ||
          boost::iequals(name, "autosize") ||
          boost::iequals(name, "width") ||
          boost::iequals(name, "height") ||
          boost::iequals(name, "rtl") ||
          boost::iequals(name, "depends") ||
          boost::iequals(name, "transition") ||
          boost::iequals(name, "transition-speed") ||
          boost::iequals(name, "font-family") ||
          boost::iequals(name, "font-import") ||
          boost::iequals(name, "css") ||
          boost::iequals(name, "class") ||
          boost::iequals(name, "navigation") ||
          boost::iequals(name, "incremental") ||
          boost::iequals(name, "left") ||
          boost::iequals(name, "right") ||
          boost::iequals(name, "id") ||
          boost::iequals(name, "audio") ||
          boost::iequals(name, "video") ||
          boost::iequals(name, "type") ||
          boost::iequals(name, "at");
}

std::string normalizeFieldValue(const std::string& value)
{
   std::string normalized = text::dcfMultilineAsFolded(value);
   return boost::algorithm::trim_copy(normalized);
}

} // anonymous namespace


json::Object Command::asJson() const
{
   json::Object commandJson;
   commandJson["name"] = name();
   commandJson["params"] = params();
   return commandJson;
}

json::Object AtCommand::asJson() const
{
   json::Object atCommandJson;
   atCommandJson["at"] = seconds();
   atCommandJson["command"] = command().asJson();
   return atCommandJson;
}

// default title to true if there is a title provided and we
// aren't in a video slide
bool Slide::showTitle() const
{
   std::string defaultTitle = (!title().empty() && video().empty()) ? "true" :
                                                                      "false";
   return boost::iequals(fieldValue("title", defaultTitle), "true");
}

std::vector<Command> Slide::commands() const
{
   std::vector<Command> commands;
   BOOST_FOREACH(const Slide::Field& field, fields_)
   {
      if (isCommandField(field.first))
         commands.push_back(Command(field.first, field.second));
   }
   return commands;
}

std::vector<AtCommand> Slide::atCommands() const
{
   std::vector<AtCommand> atCommands;
   boost::regex re("^([0-9]+)\\:([0-9]{2})\\s+([^\\:]+)(?:\\:\\s+(.*))?$");


   std::vector<std::string> atFields = fieldValues("at");
   BOOST_FOREACH(const std::string& atField, atFields)
   {
      boost::smatch match;
      if (regex_utils::match(atField, match, re))
      {
         std::string cmd = match[3];
         if (isAtCommandField(cmd))
         {
            int mins = safe_convert::stringTo<int>(match[1], 0);
            int secs = (mins*60) + safe_convert::stringTo<int>(match[2], 0);
            Command command(cmd, match[4]);
            atCommands.push_back(AtCommand(secs, command));
         }
         else
         {
            module_context::consoleWriteError("Unrecognized command '" +
                                              cmd + "'\n");
         }
      }
      else
      {
         module_context::consoleWriteError(
                  "Skipping at command with invalid syntax:\n   at: " +
                  atField + "\n");
      }
   }

   return atCommands;
}


std::vector<std::string> Slide::fields() const
{
   std::vector<std::string> fields;
   BOOST_FOREACH(const Field& field, fields_)
   {
      fields.push_back(field.first);
   }
   return fields;
}

std::string Slide::fieldValue(const std::string& name,
                              const std::string& defaultValue) const
{
   std::vector<Field>::const_iterator it =
        std::find_if(fields_.begin(), fields_.end(), CompareName(name));
   if (it != fields_.end())
      return normalizeFieldValue(it->second);
   else
      return defaultValue;
}


std::vector<std::string> Slide::fieldValues(const std::string& name) const
{
   std::vector<std::string> values;
   BOOST_FOREACH(const Field& field, fields_)
   {
      if (boost::iequals(name, field.first))
         values.push_back(normalizeFieldValue(field.second));
   }
   return values;
}

std::vector<std::string> Slide::invalidFields() const
{
   return invalidFields_;
}

namespace {

bool insertField(std::vector<Slide::Field>* pFields, const Slide::Field& field)
{
   // ignore empty records
   if (field.first.empty())
      return true;

   pFields->push_back(field);
   return true;
}

} // anonymous namespace

std::string Slide::transition() const
{
   std::string value = fieldValue("transition");
   if (value == "rotate")
      value = "default";
   return value;
}

std::string Slide::rtl() const
{
   std::string value = fieldValue("rtl");
   if (value == "true")
      return value;
   else
      return "false";
}

bool Slide::autosize() const
{
   std::string value = fieldValue("autosize");
   return value == "true";
}

std::string SlideDeck::title() const
{
   if (!slides_.empty())
      return slides_[0].title();
   else
      return std::string();
}

std::string SlideDeck::rtl() const
{
   if (!slides_.empty())
      return slides_[0].rtl();
   else
      return "false";
}

bool SlideDeck::autosize() const
{
   if (!slides_.empty())
      return slides_[0].autosize();
   else
      return false;
}

int SlideDeck::width() const
{
   const int kDefaultWidth = 960;
   if (!slides_.empty() && !slides_[0].width().empty())
      return safe_convert::stringTo<int>(slides_[0].width(), kDefaultWidth);
   else
      return kDefaultWidth;
}


int SlideDeck::height() const
{
   const int kDefaultHeight = 700;
   if (!slides_.empty() && !slides_[0].height().empty())
      return safe_convert::stringTo<int>(slides_[0].height(), kDefaultHeight);
   else
      return kDefaultHeight;
}

std::string SlideDeck::fontFamily() const
{
   if (!slides_.empty())
      return slides_[0].fontFamily();
   else
      return std::string();
}

std::string SlideDeck::css() const
{
   if (!slides_.empty())
      return slides_[0].css();
   else
      return std::string();
}

std::string SlideDeck::transition() const
{
   std::string transition;
   if (!slides_.empty())
      transition = slides_[0].transition();

   if (transition.empty())
      transition = "linear";

   return transition;
}

std::string SlideDeck::transitionSpeed() const
{
   std::string speed;
   if (!slides_.empty())
      speed = slides_[0].transitionSpeed();

   if (!speed.empty())
      return speed;
   else
      return "default";
}

std::string SlideDeck::navigation() const
{
   if (!slides_.empty())
      return slides_[0].navigation();
   else
      return "slide";
}

std::string SlideDeck::incremental() const
{
   std::string val = !slides_.empty() ? slides_[0].incremental() : "";
   if (!val.empty())
      return val;
   else
      return "false";
}

std::string SlideDeck::depends() const
{
   if (!slides_.empty())
      return slides_[0].depends();
   else
      return "slide";
}


Error SlideDeck::readSlides(const FilePath& filePath)
{
   // read the file
   std::string slides;
   Error error = readStringFromFile(filePath,
                                    &slides,
                                    string_utils::LineEndingPosix);
   if (error)
      return error;


   // read the slides
   return readSlides(slides, filePath.parent());
}

Error SlideDeck::readSlides(const std::string& slides, const FilePath& baseDir)
{
   // clear existing
   slides_.clear();

   // capture base dir
   baseDir_ = baseDir;

   // split into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, slides, boost::algorithm::is_any_of("\n"));

   // find indexes of lines with 3 or more consecutive equals
   boost::regex re("^\\={3,}\\s*$");
   std::vector<std::size_t> headerLines;
   for (std::size_t i = 0; i<lines.size(); i++)
   {
      boost::smatch m;
      if (regex_utils::match(lines[i], m, re))
         headerLines.push_back(i);
   }

   // capture the preamble (if any)
   preamble_.clear();
   if (!headerLines.empty())
   {
      if (headerLines[0] > 1)
      {
         for (std::size_t i = 0; i<(headerLines[0] - 1); i++)
            preamble_.append(lines[i]);
      }
   }

   // loop through the header lines to capture the slides
   boost::regex dcfFieldRegex(core::text::kDcfFieldRegex);
   for (std::size_t i = 0; i<headerLines.size(); i++)
   {
      // line index
      std::size_t lineIndex = headerLines[i];

      // title is the line before (if there is one)
      std::string title = lineIndex > 0 ? lines[lineIndex-1] : "";

      // line of code the slide is on
      int line = !title.empty() ? lineIndex - 1 : lineIndex;

      // find the begin index (line after)
      std::size_t beginIndex = lineIndex + 1;

      // find the end index (next section or end of file)
      std::size_t endIndex;
      if (i < (headerLines.size()-1))
         endIndex = headerLines[i+1] - 1;
      else
         endIndex = lines.size();

      // now iterate through from begin to end and break into fields and content
      bool inFields = true;
      std::string fields, content;
      for (std::size_t l = beginIndex; l<endIndex; l++)
      {
         if (inFields)
         {
            if (regex_utils::match(lines[l], dcfFieldRegex))
            {
               fields += lines[l] + "\n";
            }
            else
            {
               content += lines[l] + "\n";
               inFields = false;
            }
         }
         else
         {
            content += lines[l] + "\n";
         }
      }

      // now parse the fields
      std::string errMsg;
      std::vector<Slide::Field> slideFields;
      Error error = text::parseDcfFile(fields,
                                       false,
                                       boost::bind(insertField,
                                                   &slideFields, _1),
                                       &errMsg);
      if (error)
      {
         std::string badLine = error.getProperty("line-contents");
         if (!badLine.empty())
            module_context::consoleWriteError("Invalid DCF field:\n   "
                                              + badLine + "\n");
         return error;
      }

      // validate all of the fields
      std::vector<std::string> invalidFields;
      BOOST_FOREACH(const Slide::Field& field, slideFields)
      {
         if (!isValidField(field.first))
            invalidFields.push_back(field.first);
      }

      // create the slide
      slides_.push_back(Slide(title,
                              slideFields,
                              invalidFields,
                              content,
                              line));
   }

   // if the deck is empty then insert a placeholder first slide
   if (slides_.empty())
   {
      slides_.push_back(Slide(baseDir.filename(),
                              std::vector<Slide::Field>(),
                              std::vector<std::string>(),
                              std::string(),
                              0));
   }

   return Success();
}


} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

