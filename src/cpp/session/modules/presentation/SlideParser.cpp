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

using namespace core;

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
          boost::iequals(name, "navigation") ||
          boost::iequals(name, "incremental") ||
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

// default title to true unless this is a video slide
bool Slide::showTitle() const
{
   if (video().empty())
      return !boost::iequals(fieldValue("title", "true"), "false");
   else
      return boost::iequals(fieldValue("title", "false"), "true");
}

std::vector<Command> Slide::commands() const
{
   std::vector<Command> commands;
   std::vector<std::string> flds = fields();
   for (size_t i=0; i<flds.size(); i++)
   {
      std::string field = flds[i];
      if (isCommandField(field))
         commands.push_back(Command(field, fieldValue(field)));
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
      if (boost::regex_match(atField, match, re))
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


namespace {

void insertField(std::vector<Slide::Field>* pFields, const Slide::Field& field)
{
   pFields->push_back(field);
}

} // anonymous namespace



std::string SlideDeck::title() const
{
   if (!slides_.empty())
      return slides_[0].title();
   else
      return std::string();
}

std::string SlideDeck::navigation() const
{
   if (!slides_.empty())
      return slides_[0].navigation();
   else
      return "slides";
}

std::string SlideDeck::incremental() const
{
   std::string val = !slides_.empty() ? slides_[0].incremental() : "";
   if (!val.empty())
      return val;
   else
      return "false";
}


Error SlideDeck::readSlides(const FilePath& filePath)
{
   // clear existing
   slides_.clear();

   // capture base dir
   baseDir_ = filePath.parent();

   // read the file
   std::string slides;
   Error error = readStringFromFile(filePath, &slides);
   if (error)
      return error;

   // split into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, slides, boost::algorithm::is_any_of("\r\n"));

   // find indexes of lines with 3 or more consecutive equals
   boost::regex re("^\\={3,}\\s*$");
   std::vector<std::size_t> headerLines;
   for (std::size_t i = 0; i<lines.size(); i++)
   {
      boost::smatch m;
      if (boost::regex_match(lines[i], m, re))
         headerLines.push_back(i);
   }

   // capture the preamble (if any)
   preamble_.clear();
   if (!headerLines.empty())
   {
      for (std::size_t i = 0; i<(headerLines[0]-1); i++)
         preamble_.append(lines[i]);
   }

   // loop through the header lines to capture the slides
   for (std::size_t i = 0; i<headerLines.size(); i++)
   {
      // line index
      std::size_t lineIndex = headerLines[i];

      // title is the line before (if there is one)
      std::string title = lineIndex > 0 ? lines[lineIndex-1] : "";

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
            std::string line = boost::algorithm::trim_copy(lines[l]);
            if (!line.empty())
               fields += line + "\n";
            else
               inFields = false;
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
      BOOST_FOREACH(const Slide::Field& field, slideFields)
      {
         if (!isValidField(field.first))
         {
            module_context::consoleWriteError("Unrecognized field '" +
                                              field.first + "'\n");
         }
      }

      // create the slide
      slides_.push_back(Slide(title, slideFields, content));
   }

   // if the deck is empty then insert a placeholder first slide
   if (slides_.empty())
   {
      slides_.push_back(Slide(filePath.parent().filename(),
                              std::vector<Slide::Field>(),
                              std::string()));
   }

   return Success();
}


} // namespace presentation
} // namespace modules
} // namesapce session

