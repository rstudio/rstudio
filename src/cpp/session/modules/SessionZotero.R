#
# SessionZotero.R
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

# R interface for prototyping local Zotero libraries. R packages to use here:
#
#  - RSQLite: https://cran.r-project.org/web/packages/RSQLite
#  - jsonlite: https://cran.r-project.org/web/packages/jsonlite/
#  - stringr: https://cran.r-project.org/web/packages/stringr/
#
# When you save this file it will be dynamically re-sourced into the 
# R session (so unlike C++ no need to restart R)
# 
# NOTE: When testing with demo data, if you don't increment the version you
# will be consistently frustrated by all of the cache hits that occur
#

.rs.addFunction("zoteroGetLibrary", function(dataDir, cacheSpec) {
   
   # debug print values to R console (this code can be removed)
   # cat("zoteroGetLibrary:\n")
   # str(dataDir)
   # str(cacheSpec)
   
   library <- list(
      name = "C5EC606F-5FF7-4CFD-8873-533D6C31DDF0",
      version = 1,
      items = .rs.zoteroDemoItems()
   )
  
   jsonlite::toJSON(
      library,
      auto_unbox = TRUE,
      pretty = TRUE
   )
   
})

.rs.addFunction("zoteroGetCollections", function(dataDir, collections, cacheSpecs) {
   
   # debug print values to R console (this code can be removed)
   # cat("zoteroGetCollections:\n")
   # str(dataDir)
   # str(collections)
   # str(cacheSpecs)
   
   # demo items for use in demo collections
   items <- .rs.zoteroDemoItems()
   
   collection1 <- list(
      name = "Collection1",
      version = 1,
      items = list(items[[1]])
   )
   
   collection2 <- list(
      name = "Collection2",
      version = 1,
      items = list(items[[2]])
   )
   
   jsonlite::toJSON(
      list(collection1, collection2),
      auto_unbox = TRUE,
      pretty = TRUE
   )
})

# some demo items for proving that everything works end to end
.rs.addFunction("zoteroDemoItems", function() {
   
   # item 1
   item1 <- list(
      id = "7989909/84869UEJ",
      type = "article-journal",
      title = "Dataset for usa flights",
      URL = "https://zenodo.org/record/1246060",
      DOI = "10.5281/ZENODO.1246060",
      note = "Citation Key: arthur2009a\nPublisher: Zenodo\ntex.abstractnote: Dataset for USA flights From 1987 to 2008",
      author = list(
         list(
            family = "Arthur",
            given = "Daniel"
         )
      ),
      issued = list(
         `date-parts` = list(
            c(
               2009,
               2
            )
         )
      )
   )
   
   # item 2
   item2 <- list(
      id = "7989909/UL6LI29C",
      type = "article-journal",
      title = "Implications of COVID-19 in pediatric rheumatology",
      `container-title` = "Rheumatology International",
      page = "1193–1213",
      volume = "40",
      issue = "8",
      URL = "http://dx.doi.org/10.1007/s00296-020-04612-6",
      DOI = "10.1007/s00296-020-04612-6",
      note = "Citation Key: batu2020\nPublisher: Springer Science and Business Media LLC",
      author = list(
         list(
            family = "Batu",
            given = "Ezgi Deniz"
         ),
         list(
            family = "Özen",
            given = "Seza"
         )
      ),
      issued = list(
         `date-parts` = list(
            c(
               2020,
               6
            )
         )
      )
   )
   
   # return them both as an array
   list(
      item1,
      item2
   )
})

