#
# SessionSystemResources.R
#
# Copyright (C) 2022 by Posit, PBC
#
# Unless you have received this program directly from Posit pursuant
# to the terms of a commercial license agreement with Posit, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("getRMemoryUsed", function()
{
   # Perform a garbage collection to collect memory statistics
   mem_used <- gc()

   # A cons cell is 28 bytes on a 32 bit R and 56 on 64 bit (see ?gc)
   cons_size <- .Machine$sizeof.pointer * 7
   cons_kb <- (mem_used["Ncells", "used"] * cons_size) / 1024

   # Vector cells are always 8 bytes (see ?gc)
   vector_kb <- mem_used["Vcells", "used"] / 128

   # Return stats
   list(
      cons = .rs.scalar(round(cons_kb)),
      vector = .rs.scalar(round(vector_kb))
   )
})
