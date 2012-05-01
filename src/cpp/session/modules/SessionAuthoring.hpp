/*
 * SessionAuthoring.hpp
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

#ifndef SESSION_MODULES_AUTHORING_HPP
#define SESSION_MODULES_AUTHORING_HPP

#include <core/json/Json.hpp>

namespace core {
   class Error;
}
 
namespace session {
namespace modules { 
namespace authoring {
   
// accessors used by SessionMain
bool isPdfViewerSupported(const std::string& userAgent);
core::json::Array supportedRnwWeaveTypes();
core::json::Array supportedLatexProgramTypes();
core::json::Object texCapabilitiesAsJson();
core::json::Object compilePdfStateAsJson();
bool hasRunningChildren();

// accessors used by workbench
std::string desktopSynctexViewer();

core::Error initialize();
                       
} // namespace authoring
} // namespace modules
} // namesapce session

#endif // SESSION_MODULES_AUTHORING_HPP
