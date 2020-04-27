package org.walleth.tmp

// can be used to make when exhaustive
// hope this becomes a language feature at some point
// https://github.com/Kotlin/KEEP/issues/204

object Do {
    inline infix fun<reified T> exhaustive(any: T?) = any
}