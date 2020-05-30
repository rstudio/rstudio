/*
 * CriticalSection.hpp
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

#ifndef CORE_SYSTEM_CRITICAL_SECTION_HPP
#define CORE_SYSTEM_CRITICAL_SECTION_HPP

#ifdef _WIN32

#include <windows.h>

namespace rstudio {
namespace core {
namespace system {

// critical section wrapper
class CriticalSection
{
public:
   CriticalSection()
   {
      ::InitializeCriticalSection(&criticalSection_);
   }

   class Scope
   {
   public:
      explicit Scope(CriticalSection& cs)
         : cs_(cs)
      {
         cs_.enter();
      }

      virtual ~Scope()
      {
         try
         {
            cs_.leave();
         }
         catch(...)
         {
         }
      }

   private:
      CriticalSection& cs_;
   };

private:
   void enter()
   {
      ::EnterCriticalSection(&criticalSection_);
   }
   void leave()
   {
      ::LeaveCriticalSection(&criticalSection_);
   }

   CRITICAL_SECTION criticalSection_;
};


} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32

#endif // CORE_SYSTEM_CRITICAL_SECTION_HPP
