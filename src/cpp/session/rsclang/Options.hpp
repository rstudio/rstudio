/*
 * Options.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef RSCLANG_OPTIONS_HPP
#define RSCLANG_OPTIONS_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/FilePath.hpp>

namespace core {
   class ProgramStatus;
}

namespace rsclang {

// singleton
class Options ;
Options& options();   
   
class Options : boost::noncopyable
{
private:
   Options() {}
   friend Options& options();
   // COPYING: boost::noncopyable
   
public:
   core::ProgramStatus read(int argc, char * const argv[]);

   core::FilePath libclangPath() const { return libclangPath_; }

   bool checkAvailable() const { return checkAvailable_; }

private:
   core::FilePath libclangPath_;
   bool checkAvailable_;
};

} // namespace rsclang

#endif // RSCLANG_OPTIONS_HPP

