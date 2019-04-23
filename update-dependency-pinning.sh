#!/bin/bash

gradle -q calculateChecksums -PnoWitness=kapt -PisCI -PsingleFlavor | grep -v "registerResGeneratingTask is deprecated, use registerGeneratedResFolders(FileCollection)" | grep -v Skipping | grep -v transforms-2 > app/witness.gradle

 
