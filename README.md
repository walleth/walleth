[![on Google Play](http://ligi.de/img/play_badge.png)](https://play.google.com/store/apps/details?id=org.walleth)

![](https://github.com/ligi/walleth/blob/master/assets/1024x500.png)

WALLΞTH
=======

The native Android Ethereum light client wallet

Contributing
==========

When running the WALLΞTH project in Android Studio one might encounter the following error during the build process: 

**Messages Gradle Build**

    Error:Execution failed for task ':app:processNoFirebaseForFDroidDebugGoogleServices'.
    > com.google.gson.stream.MalformedJsonException: Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 25 path $
    BUILD FAILED

**Gradle Console**

    :app:processNoFirebaseForFDroidDebugGoogleServices FAILED

    FAILURE: Build failed with an exception.

    * What went wrong:
    Execution failed for task ':app:processNoFirebaseForFDroidDebugGoogleServices'.
    > com.google.gson.stream.MalformedJsonException: Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 25 path $

    * Try:
    Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

    BUILD FAILED

This happens as a result of the fact that `google-services.json` is encrypted with git-crypt. Quick fix is to remove:

    apply plugin: 'com.google.gms.google-services'

from `build.gradle`

References
==========

* [ERC67](https://github.com/ethereum/EIPs/issues/67)
* [ERC20](https://github.com/ethereum/EIPs/issues/20)
* [Import Geth - a Devcon2 talk](https://ethereum.karalabe.com/talks/2016-devcon.html#1)
* [go Mobile:-Account-management](https://github.com/ethereum/go-ethereum/wiki/Mobile:-Account-management)
* [Ethereum visual reference](https://www.ethereum.org/images/logos/Ethereum_Visual_Identity_1.0.0.pdf)

License
=======

GPL

