/*
 * MacResources.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


#include <sys/sysctl.h>  
#include <mach/mach.h>

#include <shared_core/Error.hpp>
#include <core/system/Resources.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace {

Error getHostStats(vm_statistics_data_t *pStats)
{
    mach_msg_type_number_t count = HOST_VM_INFO_COUNT;
    vm_statistics_data_t stats;
    kern_return_t ret = host_statistics(
        mach_host_self(), HOST_VM_INFO, reinterpret_cast<host_info_t>(&stats), &count);

    if (ret == KERN_SUCCESS)
    {
        *pStats = stats;
        return Success();
    }
    
    return systemError(ret, "Failed to get memory resource usage from host_statistics", ERROR_LOCATION);
}

} // anonymous namespace

Error getTotalMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
{
    vm_statistics_data_t stats;
    Error error = getHostStats(&stats);
    if (error)
    {
        return error;
    }

    // Typically pages are 4096 bytes, so to go from pages -> kb we'll be
    // multiplying by 4.
    *pUsedKb = static_cast<long>(stats.active_count * (::getpagesize() / 1024));

    *pProvider = MemoryProviderMacOS;
    return Success();
}

Error getTotalMemory(long *pTotalKb, MemoryProvider *pProvider)
{
    int flags[] = { CTL_HW, HW_MEMSIZE };
    int64_t mem;
    size_t len = sizeof(mem);

    int result = ::sysctl(flags, 2, &mem, &len, NULL, 0);
    if (result == -1)
    {
        return systemError(errno, "Failed to get total physical memory from sysctl", ERROR_LOCATION);
    }

    *pTotalKb = mem / 1024;
    *pProvider = MemoryProviderMacOS;
    return Success();
}

Error getProcessMemoryUsed(long *pUsedKb, MemoryProvider *pProvider)
{
    struct task_basic_info info;
    mach_msg_type_number_t count = TASK_BASIC_INFO_COUNT;
    kern_return_t ret = ::task_info(
        mach_task_self(), TASK_BASIC_INFO, reinterpret_cast<task_info_t>(&info), &count);
    if (ret == KERN_SUCCESS)
    {
       *pUsedKb = static_cast<long>(info.resident_size / 1024);
       *pProvider = MemoryProviderMacOS;
       return Success();
    }

    return systemError(ret, "Failed to get memory resource usage from task_info", ERROR_LOCATION);
}

} // namespace system
} // namespace core
} // namespace rstudio
