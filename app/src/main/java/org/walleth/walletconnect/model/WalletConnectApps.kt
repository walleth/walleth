package org.walleth.walletconnect.model

const val WalletConnectApps = """
    [
    { 
        "name": "Example dApp",
        "url": "https://example.walletconnect.org",
        "icon": "https://example.walletconnect.org/favicon.ico",
        "networks": ["1","4","5","100"]
    },    
    { 
        "name": "ENS",
        "url": "https://app.ens.domains",
        "icon": "https://app.ens.domains/favicon-32x32.png",
        "networks": ["1","4","5","3"]
     },
      { 
        "name": "Etherscan",
        "url": "https://etherscan.io",
        "icon": "https://etherscan.io/images/brandassets/etherscan-logo-circle.png",
        "networks" : ["1"]
     },   
      { 
        "name": "Etherscan",
        "url": "https://goerli.etherscan.io",
        "icon": "https://etherscan.io/images/brandassets/etherscan-logo-circle.png",
        "networks" : ["5"]
     },   
     {
         "name": "Gnosis safe",
         "url": "https://gnosis-safe.io/app",
         "networks": ["1"],
         "icon": "https://gnosis-safe.io/app/favicon.ico"
     },
     {
         "name": "Gnosis safe",
         "url": "https://rinkeby.gnosis-safe.io/app/",
         "networks": ["4"],
         "icon": "https://rinkeby.gnosis-safe.io/app/favicon.ico"
     },
     { 
        "name": "ReMix IDE",
        "networks": [ "*" ],
        "url": "http://remix.ethereum.org",
        "icon": "https://raw.githubusercontent.com/ethereum/remix-ide/master/favicon.ico"
     }, 
     { 
      "name": "uniswap",
       "url": "https://app.uniswap.org",
       "networks": ["1"],
       "icon": "https://app.uniswap.org/./favicon.png"
      },
     { 
      "name": "zkSync",
       "url": "https://rinkeby.zksync.io",       
       "networks": ["1"],
       "icon": "https://rinkeby.zksync.io/_nuxt/icons/icon_64x64.3fdd8f.png"
      },
        { 
      "name": "zkSync",
       "url": "https://wallet zksync.io",       
       "networks": ["4"],
       "icon": "https://rinkeby.zksync.io/_nuxt/icons/icon_64x64.3fdd8f.png"
      },
     { 
        "name": "Other Apps",
        "url": "https://walletconnect.org/apps",
        "icon": "https://example.walletconnect.org/favicon.ico"
    }
    ]
    
"""