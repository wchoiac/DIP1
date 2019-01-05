package xyz.medirec.medirec

import java.io.Serializable

class KeyProperties(val encoded: ByteArray, val format: String, val algorithm: String): Serializable{
    companion object { const val serialVersionUID = 1234123412341L }
}