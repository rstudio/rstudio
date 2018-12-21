#!/usr/bin/env Rscript

themes <- read.delim("theme_urls.tsv", sep = "\t")

Map(download.file, themes$url, themes$filename)
