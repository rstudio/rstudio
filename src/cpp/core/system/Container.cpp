/*
 * Container.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include <core/system/Container.hpp>

#include <core/Log.hpp>
#include <core/system/Environment.hpp>

#include <shared_core/FilePath.hpp>

using namespace rstudio::core;

namespace rstudio::core::system::container {
namespace {

// Files that indicate the presence of a container environment
// /.dockerenv is used by Docker
// /run/.containerenv is used by Podman
// /.singularity.d is used by Singularity/Apptainer
static constexpr const char* kFileIndicators[] = {
   "/.dockerenv", "/run/.containerenv", "/.singularity.d"};

// Environment variables that indicate the presence of a container environment
// KUBERNETES_SERVICE_HOST is used by Kubernetes
// APPTAINER_CONTAINER is used by Apptainer
static constexpr const char* kEnvIndicators[] = {
   "KUBERNETES_SERVICE_HOST", "APPTAINER_CONTAINER"};

} // anonymous namespace

bool isRunningInContainer()
{
   bool isContainer = false;
   for (const auto& file : kFileIndicators)
   {
      if (const auto path = FilePath(file); path.exists())
      {
         LOG_DEBUG_MESSAGE("Detected container environment using file: " +
                           path.getAbsolutePath());
         isContainer = true;
      }
   }

   for (const auto& env : kEnvIndicators)
   {
      if (!system::getenv(env).empty())
      {
         LOG_DEBUG_MESSAGE(
             "Detected container environment using environment variable: " +
             std::string(env));
         isContainer = true;
      }
   }

   return isContainer;
}

} // namespace rstudio::core::system::container
