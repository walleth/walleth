[![on Google Play](http://ligi.de/img/play_badge.png)](https://play.google.com/store/apps/details?id=org.walleth)

![](https://github.com/ligi/walleth/blob/master/assets/1024x500.png)

WALLΞTH
=======

The native Android Ethereum light client wallet

Contributing
==========

When running the WALLΞTH project in Android Studio one might encounter the following error during the build process: 

[![enter image description here][2]][2]

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

  [2]: https://i.stack.imgur.com/1RDcW.png
