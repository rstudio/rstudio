/*
 * SessionProjectsOverlay.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_PROJECTS_PROJECTS_OVERLAY_HPP
#define SESSION_PROJECTS_PROJECTS_OVERLAY_HPP

#include <string>

namespace rstudio::core {
   class Error;
   class FilePath;
namespace r_util {
   class ProjectId;
   struct RProjectConfig;
} // namespace r_util
} // namespace rstudio::core

namespace rstudio::session::projects {
   class ProjectContext;
namespace overlay {

void onProjectConfigUpdate(const core::r_util::RProjectConfig& oldConfig,
                           const core::r_util::RProjectConfig& newConfig);

void onProjectStartup(const ProjectContext& context, const core::r_util::ProjectId& projectId);

void onCreateProject(const core::FilePath& directory);

core::Error ensureProjectIdForPath(const core::FilePath& directory);

} // namespace overlay
} // namespace rstudio::session::projects

#endif // SESSION_PROJECTS_PROJECTS_OVERLAY_HPP
