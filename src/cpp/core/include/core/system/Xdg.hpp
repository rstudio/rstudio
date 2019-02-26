/*
 * Xdg.hpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include <core/FilePath.hpp>

#include <vector>

namespace rstudio {
namespace core {
namespace system {
namespace xdg {

FilePath userConfigDir();
FilePath userDataDir();

FilePath systemConfigDir();

} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

