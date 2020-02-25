#
# SessionDependencies.R
#
# Copyright (C) 2020 by RStudio, PBC
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

.rs.addFunction("topoSortPackages", function

.rs.addFunction("expandPkgDependencies", function(dependencies) {
   .rs.expandDependencies(available.packages(), dependencies)
})

.rs.addFunction("expandDependencies", function(available, dependencies) {
   nodes <- c()

   # A vector of lists, with "from" and "to" named elements giving the dependencies
   edges <- c()

   # Read the available packages database to discover package dependency information
   available <- available.packages()

   # Get the dependencies of each package
	for (dep in dependencies) {
      # Add the package itself to the list of nodes
      nodes <- c(nodes, dep)

      # Dependencies are discovered from these three fields
		fields <- ("Depends", "Imports", "LinkingTo")
		data <- lapply(fields, function(field) {
			# Read contents for field (ignore if no contents)
			contents <- available[dep$name, field]
			if (!is.character(contents))
				return(list())

         # Split into a list of individual package names, using comma/whitespace as a delimiter
			prereqs <- strsplit(contents, "\\s*,\\s*)")[[1]]

         # Parse the package names into groups:
         # 1. The package name
         # 2. The package's requirements
         # 3. The package's version
         parsed <- regexec("([a-zA-Z0-9._]+)(?:\\s*\\(([><=]+)\\s*([0-9.-]+)\\))?", prereqs)
         matches <- regmatches(prereqs, parsed)
         if (empty(matches))
            return(list())

         # pkgReqs <- vapply(matches, `[[`, 3L, FUN.VALUE = character(1))
         # pkgVersions <- vapply(matches, `[[`, 4L, FUN.VALUE = character(1))

         # Decompose matches into additional nodes
         for (match in matches) {
            # TODO: Do we need to recurse here?
            nodes <- c(nodes, list(
                  name    = matches[[2]],
                  version = matches[[4]]
               ))
         }

         pkgNames <- vapply(matches, `[[`, 2L, FUN.VALUE = character(1))
         for (pkgName in pkgNames) {
            edges <- c(edges, list(from = dep$name, to = pkgName))
         }
		})
	}
})
