/*
 * SessionBuildUtils.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_BUILD_UTILS_HPP
#define SESSION_BUILD_UTILS_HPP

#include <string>

#include <core/json/Json.hpp>

namespace session {
namespace modules {
namespace build {


const int kBuildOutputCommand = 0;
const int kBuildOutputNormal = 1;
const int kBuildOutputError = 2;

struct BuildOutput
{
   BuildOutput(int type, const std::string& output)
      : type(type), output(output)
   {
   }

   int type;
   std::string output;
};

core::json::Object buildOutputAsJson(const BuildOutput& buildOutput);


} // namespace build
} // namespace modules
} // namespace session

#endif // SESSION_BUILD_UTILS_HPP

