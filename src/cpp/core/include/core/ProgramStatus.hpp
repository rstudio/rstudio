/*
 * ProgramStatus.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_PROGRAM_STATUS_HPP
#define CORE_PROGRAM_STATUS_HPP

#include <cstdlib>

namespace rstudio {
namespace core {

class ProgramStatus
{  
public:
   static ProgramStatus run()
   {
      return ProgramStatus(false, EXIT_SUCCESS);
   }
   
   static ProgramStatus exitSuccess()
   {
      return ProgramStatus(true, EXIT_SUCCESS);
   }
   
   static ProgramStatus exitFailure()
   {
      return ProgramStatus(true, EXIT_FAILURE);
   }
   
public:
   ProgramStatus(bool exit, int exitCode) : exit_(exit), exitCode_(exitCode) {}
   
   // COPYING: via compiler (copyable members)
   
   bool exit() const { return exit_; }
   int exitCode() const { return exitCode_; }

private:
   bool exit_;
   int exitCode_;
};
   
} // namespace core
} // namespace rstudio


#endif // CORE_PROGRAM_STATUS_HPP

