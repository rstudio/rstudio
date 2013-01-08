/*
 * FileLock.hpp
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

#ifndef CORE_FILE_LOCK_HPP
#define CORE_FILE_LOCK_HPP

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>

namespace core {

class Error;
class FilePath;

class FileLock : boost::noncopyable
{
public:
   static bool isLocked(const FilePath& lockFilePath);

public:
   FileLock();
   virtual ~FileLock();

   // COPYING: noncopyable

   Error acquire(const FilePath& lockFilePath);
   Error release();

   FilePath lockFilePath() const;

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace core


#endif // CORE_FILE_LOCK_HPP
