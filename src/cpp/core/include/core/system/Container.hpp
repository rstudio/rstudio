/*
 * Container.hpp
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

#ifndef CORE_SYSTEM_CONTAINER_HPP
#define CORE_SYSTEM_CONTAINER_HPP

#include <string>
#include <vector>

namespace rstudio::core::system::container {

/**
 * @brief Checks if the current process is running inside a container.
 *
 * This function inspects the system environment and configuration to determine
 * if the current process is being executed within a containerized environment
 * such as Docker, Kubernetes, Apptainer, Singularity, or other container
 * platforms.
 *
 * This function checks:
 * - presence of /.dockerenv, /run/.containerenv, or /.singularity.d
 * files/folders
 * - common environment variables (KUBERNETES_SERVICE_HOST, APPTAINER_CONTAINER,
 * etc.)
 *
 * @return true if the current process is running inside a container, false
 * otherwise.
 */
bool isRunningInContainer();

} // namespace rstudio::core::system::container

#endif // CORE_SYSTEM_CONTAINER_HPP
