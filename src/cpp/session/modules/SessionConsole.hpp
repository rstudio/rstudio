/*
 * SessionConsole.hpp
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

#ifndef SESSION_CONSOLE_HPP
#define SESSION_CONSOLE_HPP

namespace core {
   class Error;
}
 
namespace session {
namespace modules { 
namespace console {
   
core::Error initialize();
                       
} // namespace console
} // namespace modules
} // namesapce session

#endif // SESSION_CONSOLE_HPP
