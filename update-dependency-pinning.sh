#!/bin/bash

gradle -q calculateChecksums -PisCI -PsingleFlavor | grep -v "registerResGeneratingTask is deprecated, use registerGeneratedResFolders(FileCollection)" > app/witness.gradle

 
