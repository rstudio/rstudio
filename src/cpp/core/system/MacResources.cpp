/*
 * MacResources.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <core/Error.hpp>

#include <sys/sysctl.h>  
#include <mach/mach.h>

#include <core/system/Resources.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace {

Error getHostStats(vm_data_statistics_t *pStats)
{
    mach_msg_type_number_t count = HOST_VM_INFO_COUNT;
    vm_statistics_data_t stats;
    kern_return_t ret = host_statistics(
        mach_host_self(), HOST_VM_INFO, static_cast<host_info_t>(&vmStats), &count);

    if (ret == KERN_SUCCESS)
    {
        *pStats = stats;
        return Success();
    }
    
    Error result = systemError(ret, ERROR_LOCATION);
    result.addDescription("Failed to get memory resource usage from host_statistics");
    return result;
}

} // anonymous namespace

Error getMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
{
    vm_statistics64_data_t stats;
    Error error = getHostStats(&stats);
    if (error)
    {
        return error;
    }

    // Typically pages are 4096 bytes, so to go from pages -> kb we'll be
    // multiplying by 4.
    *pUsedKb = static_cast<int>(stats.active_count * (getpagesize() / 1024));

    *pProvider = MemoryProviderMacOS;
    return Success();
}

Error getMemoryAvailable(int *pAvailKb, MemoryProvider *pProvider)
{
    vm_statistics64_data_t stats;
    Error error = getHostStats(&stats);
    if (error)
    {
        return error;
    }

    *pAvailKb = static_cast<int>(stats.free_count * (getpagesize() / 1024));

    *pProvider = MemoryProviderMacOS;
    return Success();
}

Error getProcessMemoryUsed(int *pUsedKb, MemoryProvider *pProvider)
{
    struct task_basic_info info;
    mach_msg_type_number_t count = TASK_BASIC_INFO_COUNT;
    kern_return_t ret = task_info(
        mach_task_self(), TASK_BASIC_INFO, static_cast<task_info_t>(&info), &count);
    if (ret == KERN_SUCCESS)
    {
        *pUsedKb = static_cast<int>(info.virtual_size * (getpagesize() / 1024));
        return Success();
    }

    Error result = systemError(ret, ERROR_LOCATION);
    result.addDescription("Failed to get memory resource usage from task_info");
    return result;
}

} // namespace sytem
} // namespace core
} // namespace rstudio

