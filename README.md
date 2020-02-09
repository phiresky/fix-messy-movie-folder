Try to identify movie files in messy/unsorted folders

### Description

This program recursively reads movies and identifies them using online searches for the filename etc.

It can then create a sorted folder structure consisting of symlinks, using the standardized name `{title} ({year}) [imdbRating].{ext}` and various sorted folders such as genre, imdb rating, and year.

### Demo

##### Input:

![input directory](/screenshots/in1.png)

##### Command:

Get the jar from [here](https://github.com/phiresky/fix-messy-movie-folder/releases).

```
$ java -jar movieid-1.1.jar -in demo-input -out demo-output
Warning: 17 min shorter than it should be (expected 100 min): Super Size Me (2004)[demo-input/Dokumentationen/Super SIze Me/gwl-ssm.avi]
Identified 10/10 movies
```
(use `-v 2` to see the progress, `-help` to see a full list of options)

##### Output:

![output directory](/screenshots/out1.png)

### Building / Running:
A compiled version can be found in [releases](https://github.com/phiresky/fix-messy-movie-folder/releases).

A ffprobe (ffmpeg) installation is optional, but recommended. On Windows, you will need admin rights to create symlinks.
```
# compile
gradle jar
# run
java -jar build/libs/movieid*.jar
```

### Current Limitations:

* Uses google for fuzzy searching. Blocked after ~50 calls. The results of the online queries are cached so they only have to be done once, but for lots of movies you will still have to wait or reset your IP.
* Incorrectly identifies movies split into multiple parts
* No measurement for accuracy of the results
* Generic folder names with a single movie inside are not parsed correctly as the folder name takes precedence
