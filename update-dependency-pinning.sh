#!/bin/bash

gradle -q calculateChecksums -PnoWitness=kapt -PisCI -PsingleFlavor | grep -v "registerResGeneratingTask is deprecated, use registerGeneratedResFolders(FileCollection)" | grep -v ':android-sdk:' | grep -v ':android:' | grep -v Skipping > app/witness.gradle

 
