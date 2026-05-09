/*
 * SessionProjectsOverlay.cpp
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

#include <session/projects/SessionProjectsOverlay.hpp>

#include <shared_core/Error.hpp>

using namespace rstudio::core;

namespace rstudio::session::projects::overlay {

void onProjectConfigUpdate(const core::r_util::RProjectConfig&,
                           const core::r_util::RProjectConfig&)
{
}

void onProjectStartup(const ProjectContext&,
                      const core::r_util::ProjectId&)
{
}

void onCreateProject(const core::FilePath&)
{
}

core::Error ensureProjectIdForPath(const core::FilePath& directory)
{
   return Success();
}

} // namespace rstudio::session::projects::overlay
