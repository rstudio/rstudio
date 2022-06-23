#!/usr/bin/env Rscript

themes <- read.delim("theme_urls.tsv", sep = "\t")
themes <- themes[!is.na(themes$url), ]

Map(download.file, themes$url, themes$filename)
