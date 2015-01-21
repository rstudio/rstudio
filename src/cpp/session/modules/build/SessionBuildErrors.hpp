/*
 * SessionBuildErrors.hpp
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

#ifndef SESSION_BUILD_ERRORS_HPP
#define SESSION_BUILD_ERRORS_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>
#include <boost/foreach.hpp>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace build {

typedef boost::function<std::vector<module_context::SourceMarker>(const std::string&)>
                                                         CompileErrorParser;

class CompileErrorParsers
{
public:
   CompileErrorParsers()
   {
   }

   void add(CompileErrorParser parser)
   {
      parsers_.push_back(parser);
   }

public:
   std::vector<module_context::SourceMarker> operator()(const std::string& output)
   {
      using namespace module_context;
      std::vector<SourceMarker> allErrors;
      BOOST_FOREACH(const CompileErrorParser& parser, parsers_)
      {
         std::vector<SourceMarker> errors = parser(output);
         std::copy(errors.begin(), errors.end(), std::back_inserter(allErrors));
      }

      return allErrors;
   }

private:
   std::vector<CompileErrorParser> parsers_;
};

CompileErrorParser gccErrorParser(const core::FilePath& basePath);

CompileErrorParser rErrorParser(const core::FilePath& basePath);


} // namespace build
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_BUILD_ERRORS_HPP

