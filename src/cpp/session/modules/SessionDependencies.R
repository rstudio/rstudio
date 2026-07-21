#
# SessionDependencies.R
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
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

# Parses DESCRIPTION-style dependency fields (e.g. "foo (>= 1.0), bar") into a
# data frame with 'name', 'op', and 'version' columns. Entries without a version
# requirement have empty 'op' and 'version'; the R version requirement is dropped.
# Declared versions that package_version() cannot parse are treated as though no
# version requirement was declared, so that a single malformed DESCRIPTION in a
# dependency closure cannot break version comparisons downstream.
.rs.addFunction("parsePackageDependencyFields", function(contents) {
   contents <- paste(contents[!is.na(contents)], collapse = ", ")
   entries <- trimws(strsplit(contents, ",")[[1]])
   entries <- entries[nzchar(entries)]

   pattern <- "^([a-zA-Z0-9._]+)(?:\\s*\\(\\s*([><=]+)\\s*([0-9.-]+)\\s*\\))?"
   matches <- regmatches(entries, regexec(pattern, entries))

   name <- character()
   op <- character()
   version <- character()
   for (match in matches) {
      if (length(match) < 4 || identical(match[[2]], "R"))
         next

      entryOp <- match[[3]]
      entryVersion <- match[[4]]
      if (!grepl("^[0-9]+([.-][0-9]+)+$", entryVersion)) {
         entryOp <- ""
         entryVersion <- ""
      }

      name <- c(name, match[[2]])
      op <- c(op, entryOp)
      version <- c(version, entryVersion)
   }

   data.frame(name = name, op = op, version = version, stringsAsFactors = FALSE)
})

# Reads the runtime dependencies (Depends and Imports) declared by an installed
# package, preferring the parsed DESCRIPTION metadata stored with the package.
# LinkingTo is excluded, as it declares a build-time requirement rather than a
# runtime one. Base packages, and packages whose metadata cannot be read at all
# (a warning is logged in that case), are treated as having no dependencies.
.rs.addFunction("installedPackageDependencies", function(pkgPath) {
   metaPath <- file.path(pkgPath, "Meta", "package.rds")
   description <- if (file.exists(metaPath)) {
      tryCatch(readRDS(metaPath)$DESCRIPTION, error = function(e) NULL)
   }

   descPath <- file.path(pkgPath, "DESCRIPTION")
   if (is.null(description) && file.exists(descPath)) {
      description <- tryCatch(drop(read.dcf(descPath)), error = function(e) NULL)
   }

   if (is.null(description)) {
      # unreadable package metadata may mask a corrupt installation, so leave
      # a trace rather than failing silently
      .rs.logWarningMessage("Unable to read package metadata from '%s'", pkgPath)
      return(.rs.parsePackageDependencyFields(character()))
   }

   if (identical(unname(description["Priority"]), "base"))
      return(.rs.parsePackageDependencyFields(character()))

   fields <- description[intersect(c("Depends", "Imports"), names(description))]
   .rs.parsePackageDependencyFields(fields)
})

# Records an unsatisfied dependency, keeping the strongest version requirement
# seen when a dependency is required by multiple packages.
.rs.addFunction("markUnsatisfiedDependency", function(unsatisfied, name, version) {
   record <- unsatisfied[[name]]
   supersedes <- is.null(record) ||
      (nzchar(version) &&
       (!nzchar(record$version) ||
        package_version(version) > package_version(record$version)))

   if (supersedes)
      unsatisfied[[name]] <- list(name = name, version = version)

   unsatisfied
})

# Verifies that the given packages, along with their recursive runtime
# dependencies (the Depends and Imports declared by the installed packages
# themselves), are installed with the required versions. Returns a list of
# records with 'name' and 'version' entries for each missing or outdated
# dependency; 'version' is the strongest declared requirement, or an empty
# string when no version was required.
#
# Note that this intentionally visits only the packages reachable from the
# requested packages, rather than scanning the entire library with
# installed.packages() -- full library scans can be very slow with large
# libraries on networked filesystems.
.rs.addFunction("findUnsatisfiedRuntimeDependencies", function(packages) {
   queue <- as.character(packages)
   visited <- character()
   unsatisfied <- list()

   while (length(queue) > 0) {
      pkg <- queue[[1]]
      queue <- queue[-1]

      if (pkg %in% visited)
         next
      visited <- c(visited, pkg)

      pkgPath <- find.package(pkg, quiet = TRUE)
      if (length(pkgPath) == 0) {
         # only packages supplied by the caller can be missing here; missing
         # recursive dependencies are recorded when their edge is examined
         if (is.null(unsatisfied[[pkg]]))
            unsatisfied[[pkg]] <- list(name = pkg, version = "")
         next
      }

      deps <- .rs.installedPackageDependencies(pkgPath[[1]])
      for (i in seq_len(nrow(deps))) {
         depName <- deps$name[[i]]
         depOp <- deps$op[[i]]
         depVersion <- deps$version[[i]]

         depPath <- find.package(depName, quiet = TRUE)
         if (length(depPath) == 0) {
            unsatisfied <- .rs.markUnsatisfiedDependency(unsatisfied, depName, depVersion)
            next
         }

         # the dependency is installed; check any declared version requirement.
         # as in .rs.expandDependencies, operators other than '>' are treated
         # as a simple minimum version
         if (nzchar(depVersion)) {
            installedVersion <- tryCatch(packageVersion(depName), error = function(e) NULL)
            satisfied <- if (is.null(installedVersion))
               FALSE
            else if (identical(depOp, ">"))
               installedVersion > package_version(depVersion)
            else
               installedVersion >= package_version(depVersion)

            if (!satisfied)
               unsatisfied <- .rs.markUnsatisfiedDependency(unsatisfied, depName, depVersion)
         }

         queue <- c(queue, depName)
      }
   }

   unname(unsatisfied)
})

.rs.addFunction("onInstallScriptJobStarted", function()
{
   .rs.setVar("jobPackageInfo", .rs.installedPackagesFileInfo())
})

.rs.addFunction("onInstallScriptJobFinished", function(pkgInfo)
{
   before <- .rs.getVar("jobPackageInfo")
   after <- .rs.installedPackagesFileInfo()
   .rs.clearVar("jobPackageInfo")
   
   # Figure out which packages were changed.
   rows <- .rs.installedPackagesFileInfoDiff(before, after)
   
   # For any packages which appear to have been updated,
   # tag their DESCRIPTION file with their installation source.
   .rs.recordPackageSource(rows$path)
})

