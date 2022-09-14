#
# SessionDependencies.R
#
# Copyright (C) 2022 by Posit Software, PBC
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

# Topographically sorts a list of packages (nodes) by their dependencies (edges). Note that this is
# not meant to be a general-purpose topo sort algorithm, and that it returns packages in the correct
# installation order, which is the exact reverse of traditional topological order. For example,
# given packages with dependencies a -> b -> c, we would want to install packages in the order "c",
# "b", and then "a".
.rs.addFunction("topoSortPackages", function(nodes, edges) {
   # List of sorted packages
   sorted <- c()

   # All nodes are unvisited to begin with
   visited <- c()

   # Define recursive descent function for dependencies
   visit <- function(node, stack) {
      if (node %in% visited) {
         return()
      }
      if (node %in% stack) {
         # We visited this node while visiting itself; this is a dependency loop.
         stop("Package dependency graph is not a directed acyclic graph.")
      }

      # Visit all the edges of this node
      stack <- c(stack, node)
      for (edge in edges) {
         if (identical(edge$from, node)) {
            visit(edge$to, stack)
         }
      }

      visited <<- c(visited, node)
      sorted <<- c(sorted, node)
   }

   # Keep visiting unvisited nodes until we have visited all of them
   while (length(visited) < length(nodes)) {
      for (node in nodes) {
         if (!(node %in% visited)) {
            visit(node, c())
            break
         }
      }
   }

   # Return topologically sorted list
   sorted
})

.rs.addFunction("expandPkgDependencies", function(dependencies) {
   available <- if (identical(R.version$os, "linux-gnu")) {
      # Get the default set of available packages on Linux (usually source) 
      available.packages()
   } else {
      # On other platforms (Mac and Windows), explicitly list both binary and source packages
      available.packages(type = "both")
   }
   .rs.expandDependencies(available, installed.packages(), dependencies)
})

.rs.addFunction("expandDependencies", function(available, installed, dependencies) {
   # A list of nodes (package names) to be installed
   nodes <- c()

   # A list of details for packages to be installed
   packages <- dependencies

   # A vector of lists, with "from" and "to" named elements giving the dependencies
   edges <- list()

   # Get the dependencies of each package
   for (dep in dependencies) {
      # Add the package itself to the list of nodes
      nodes <- c(nodes, dep$name)
   }

   # Look for dependencies of each package
   for (dep in dependencies) {
      # Dependencies are discovered from these three fields
      fields <- c("Depends", "Imports", "LinkingTo")
      for (field in fields) {
         # Read contents for field (ignore if no contents)
         contents <- available[dep$name, field]
         if (is.na(contents) || !is.character(contents))
            next

         # Split into a list of individual package names, using comma/whitespace as a delimiter
         prereqs <- strsplit(contents, "\\s*,\\s*")[[1]]

         # Parse the package names into groups:
         # 1. The package name
         # 2. The package's requirements
         # 3. The package's version
         parsed <- regexec("([a-zA-Z0-9._]+)(?:\\s*\\(([><=]+)\\s*([0-9.-]+)\\))?", prereqs)
         matches <- regmatches(prereqs, parsed)

         # Decompose matches into additional nodes
         for (match in matches) {
            if (length(match) < 2)
               next

            # Extract package name from regex result
            pkgName <- match[[2]]

            # Ignore packages that don't have an entry in the availability list
            if (!(pkgName %in% rownames(available)))
                next

            # Append to node list if we don't know about it already
            if (!pkgName %in% nodes) {
               nodes <- c(nodes, pkgName)


               # Figure out whether there is a version of the package installed which meets the
               # given constraints.
               satisfied <- if (pkgName %in% rownames(installed))
               {
                  if (length(match >= 4) && nchar(match[[4]]) > 0)
                  {
                     # We found a requirement for a specific version. See if that requirement is
                     # met.
                     installedVer <- as.package_version(installed[pkgName, "Version"])
                     requiredVer <- as.package_version(match[[4]])
                     if (identical(match[[3]], ">"))
                     {
                        # Satisfied if the installed version is strictly greater than the actual
                        # version.
                        installedVer > requiredVer
                     }
                     else
                     {
                        # If we aren't sure, treat it as a simple minimum version (this is by far
                        # the most common pattern, represented by `>=` in the Depends field and
                        # others)
                        installedVer >= requiredVer
                     }
                  }
                  else
                  {
                     # No version information was given, so the installed version is okay.
                     TRUE
                  }
               }
               else
               {
                  # The package is not installed, and therefore not satisfied.
                  FALSE
               }
               
               # The dependency is not satisfied; we will need to install it
               if (!satisfied)
               {
                  packages <- append(packages, list(list(
                        name = pkgName,
                        location = "cran",
                        version = available[pkgName, "Version"],
                        source = FALSE)))
               }
            }

            # Add a dependency edge
            edges <- append(edges, list(list(from = dep$name, to = pkgName)))
         }
      }
   }

   # We now have a complete list of packages that we need to install. Sort it topologically so that
   # we install dependencies before the packages they depend on. This returns a character vector of
   # sorted package names.
   sorted <- .rs.topoSortPackages(nodes, edges)

   # Rebuild the list of actual package records from the sorted names.
   result <- list()
   for (package in sorted) {
      for (record in packages) {
         if (record$name == package) {
            result <- append(result, list(record))
            break
         }
      }
   }

   # Return the expanded and sorted result
   result
})
