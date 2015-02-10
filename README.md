Try to identify movie files in messy/unsorted folders


This program recursively reads movies and identifies them using online searches for the filename etc.

It can then create a sorted folder structure consisting of symlinks, using the standardized name `{title} ({year}).{ext}` and various sorted folders such as genre, imdb rating, and year.

### Current Limitations:

* Uses google for fuzzy searching. Blocked after ~50 calls. The results of the online queries are cached so they only have to be done once
* Incorrectly identifies movies split into multiple parts
* No measurement for accuracy of the results
* Generic folder names with a single movie inside are not parsed correctly as the folder name takes precedence
