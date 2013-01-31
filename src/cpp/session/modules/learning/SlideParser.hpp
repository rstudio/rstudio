/*
 * SlideParser.hpp
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

#ifndef SESSION_LEARNING_SLIDE_PARSER_HPP
#define SESSION_LEARNING_SLIDE_PARSER_HPP


#include <string>
#include <vector>
#include <map>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

namespace session {
namespace modules { 
namespace learning {

class Command {
public:
   Command(const std::string& name, const std::string& params)
      : name_(name), params_(params)
   {
   }

   const std::string& name() const { return name_; }
   const std::string& params() const { return params_; }

private:
   std::string name_;
   std::string params_;
};

class AtCommand {
public:
   AtCommand(int seconds, const Command& command)
      : seconds_(seconds), command_(command)
   {
   }

   int seconds() const { return seconds_; }
   const Command& command() const { return command_; }

private:
   int seconds_;
   Command command_;
};

class Slide
{
public:
   typedef std::pair<std::string,std::string> Field;

public:
   Slide(const std::string& title,
         const std::vector<Field>& fields,
         const std::string& content)
      : title_(title), fields_(fields), content_(content)
   {
   }

   std::string title() const { return title_; }
   bool showTitle() const;

   std::string id() const { return fieldValue("id"); }

   std::string type() const { return fieldValue("type"); }

   std::string video() const { return fieldValue("video"); }
   std::string audio() const { return fieldValue("audio"); }

   std::vector<Command> commands() const;

   std::vector<AtCommand> atCommands() const;

   std::vector<std::string> fields() const;
   std::string fieldValue(const std::string& name,
                          const std::string& defaultValue="") const;
   std::vector<std::string> fieldValues(const std::string& name) const;

   const std::string& content() const { return content_; }

private:
   std::string title_;
   std::vector<Field> fields_;
   std::string content_;
};

class SlideDeck
{
public:
   SlideDeck()
   {
   }

   core::Error readSlides(const core::FilePath& filePath);

   std::string title() const;

   std::string preamble() const { return preamble_; }

   const std::vector<Slide>& slides() const { return slides_; }

   core::FilePath baseDir() const { return baseDir_; }

private:
   core::FilePath baseDir_;
   std::string preamble_;
   std::vector<Slide> slides_;
};


core::Error readSlides(const core::FilePath& filePath,
                       std::vector<Slide>* pSlides,
                       std::string* pUserErrMsg);


} // namespace learning
} // namespace modules
} // namesapce session

#endif // SESSION_LEARNING_SLIDE_PARSER_HPP
