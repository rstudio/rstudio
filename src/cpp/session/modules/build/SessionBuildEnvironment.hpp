/*
 * SessionBuildEnvironment.hpp
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

#ifndef SESSION_BUILD_ENVIRONMENT_HPP
#define SESSION_BUILD_ENVIRONMENT_HPP

#include <string>

#include <core/system/Environment.hpp>

namespace session {
namespace modules {
namespace build {

bool addRtoolsToPathIfNecessary(std::string* pPath,
                                std::string* pWarningMessage);

bool addRtoolsToPathIfNecessary(core::system::Options* pEnvironment,
                                std::string* pWarningMessage);

} // namespace build
} // namespace modules
} // namespace session

#endif // SESSION_BUILD_ENVIRONMENT_HPP

