package org.walleth.util

import java.io.File

private fun semicolonSeparatedSet(file: File) = file.readText().split(";").toHashSet()

class FileBackedStore(private val storeDir: File) {

    fun upsert(signatureHash: String, signatureText: String): Boolean {
        val file = File(storeDir, signatureHash)
        val isNewEntry = !file.exists()
        return isNewEntry.also {
            val res = if (isNewEntry) {
                HashSet()
            } else {
                semicolonSeparatedSet(file)
            }
            res.add(signatureText)
            file.writeText(res.joinToString(";"))
        }
    }

    fun get(signatureHash: String) = semicolonSeparatedSet(toFile(signatureHash))
    fun has(signatureHash: String) = toFile(signatureHash).exists()
    fun all() = storeDir.list().toList()
    private fun toFile(signatureHash: String) = File(storeDir, signatureHash)

}