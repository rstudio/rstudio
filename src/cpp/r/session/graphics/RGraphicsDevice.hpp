/*
 * RGraphicsDevice.hpp
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

#ifndef R_SESSION_GRAPHICS_DEVICE_HPP
#define R_SESSION_GRAPHICS_DEVICE_HPP

#include <boost/function.hpp>

namespace core {
   class Error;
   class FilePath;
}

namespace r {
namespace session {
namespace graphics {
namespace device {
   
extern const int kDefaultWidth;
extern const int kDefaultHeight;
extern const bool kDefaultIsRetina;
   
// initialize
core::Error initialize(
          const core::FilePath& graphicsPath,
          const boost::function<bool(double*,double*)>& locatorFunction);
   
// device size
void setSize(int width, int height, bool isRetina);
int getWidth();
int getHeight();
int isRetina();

// reset
void close();


} // namespace device
} // namespace graphics
} // namespace session
} // namespace r


#endif // R_SESSION_GRAPHICS_DEVICE_HPP 

