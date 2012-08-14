/*
 * RGraphicsFileDevice.hpp
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

#ifndef R_GRAPHICS_FILE_DEVICE_HPP
#define R_GRAPHICS_FILE_DEVICE_HPP


#include "RGraphicsTypes.hpp"

namespace core {
   class Error;
   class FilePath;
}

namespace r {
namespace session {
namespace graphics {
namespace file_device {

core::Error create(int width,
                   int height,
                   int pointSize,
                   const core::FilePath& targetPath);


} // namespace file_device
} // namespace graphics
} // namespace session
} // namespace r


#endif // R_GRAPHICS_FILE_DEVICE_HPP

