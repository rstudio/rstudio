/*
 * ConfigUtils.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/ConfigUtils.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Settings.hpp>

namespace core {
namespace config_utils {

Error extractVariables(const FilePath& file, Variables* pVariables)
{
   // treat as a settings file
   Settings settings;
   Error error = settings.initialize(file);
   if (error)
      return error;

   // look for each of the specified variables
   for (Variables::iterator it = pVariables->begin();
        it != pVariables->end();
        ++it)
   {
      // reset its value to what was in the config file
      it->second = settings.get(it->first, it->second);
   }

   // return success
   return Success();
}

} // namespace config_utils
} // namespace core 



