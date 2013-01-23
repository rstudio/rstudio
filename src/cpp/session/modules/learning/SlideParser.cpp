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
#include <core/StringUtils.hpp>
#include <core/text/DcfParser.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace learning {

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
          boost::iequals(name, "help-source") ||
          boost::iequals(name, "source");
}

std::string normalizeFieldValue(const std::string& value)
{
   std::string normalized = text::dcfMultilineAsFolded(value);
   return boost::algorithm::trim_copy(normalized);
}

} // anonymous namespace

// default title to true unless this is a video slide
bool Slide::showTitle() const
{
   if (video().empty())
      return !boost::iequals(fieldValue("title", "true"), "false");
   else
      return boost::iequals(fieldValue("title", "false"), "true");
}

std::string Slide::commandsJsArray() const
{
   std::ostringstream ostr;
   ostr << "[ ";

   std::vector<std::string> flds = fields();
   for (size_t i=0; i<flds.size(); i++)
   {
      std::string field = flds[i];
      if (isCommandField(field))
      {
         ostr << "{ name: \""
              << string_utils::jsLiteralEscape(field)
              << "\", params: \""
              << string_utils::jsLiteralEscape(fieldValue(field))
              << "\" }";
         if (i != (flds.size()-1))
            ostr << ", ";
      }
   }
   ostr << " ]";
   return ostr.str();
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


Error SlideDeck::readSlides(const FilePath& filePath,
                            std::string* pUserErrorMsg)
{
   // clear existing
   slides_.clear();;

   // read the file
   std::string slides;
   Error error = readStringFromFile(filePath, &slides);
   if (error)
   {
      *pUserErrorMsg = error.summary();
      return error;
   }

   // split into lines
   std::vector<std::string> lines;
   boost::algorithm::split(lines, slides, boost::algorithm::is_any_of("\r\n"));

   // find indexes of lines with dashes
   boost::regex re("^\\-{3,}\\s*$");
   std::vector<std::size_t> headerLines;
   for (std::size_t i = 0; i<lines.size(); i++)
   {
      boost::smatch m;
      if (boost::regex_match(lines[i], m, re))
         headerLines.push_back(i);
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
         std::string line = boost::algorithm::trim_copy(lines[l]);
         if (inFields)
         {
            if (!line.empty())
               fields += line + "\n";
            else
               inFields = false;
         }
         else
         {
            content += line + "\n";
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
            *pUserErrorMsg = "Invalid DCF field (no separator?): " + badLine;
         else
            *pUserErrorMsg = error.summary();
         return error;
      }

      // create the slide
      slides_.push_back(Slide(title, slideFields, content));
   }

   return Success();
}


} // namespace learning
} // namespace modules
} // namesapce session

