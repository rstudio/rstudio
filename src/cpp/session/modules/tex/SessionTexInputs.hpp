/*
 * SessionTexInputs.hpp
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

#ifndef SESSION_MODULES_TEX_INPUTS_HPP
#define SESSION_MODULES_TEX_INPUTS_HPP

#include <core/FilePath.hpp>

#include <core/system/Types.hpp>

namespace core {
   class Error;
}
 
namespace session {
namespace modules { 
namespace tex {
namespace inputs {

struct RTexmfPaths
{
   bool empty() const { return texInputsPath.empty(); }

   core::FilePath texInputsPath;
   core::FilePath bibInputsPath;
   core::FilePath bstInputsPath;
};

RTexmfPaths rTexmfPaths();


core::system::Options environmentVars();

} // namespace inputs
} // namespace tex
} // namespace modules
} // namesapce session

#endif // SESSION_MODULES_TEX_INPUTS_HPP
