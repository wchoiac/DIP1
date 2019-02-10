package xyz.medirec.medirec.pojo

import java.io.Serializable
import javax.crypto.SecretKey

class KeyTime(val pubKeyEncoded: ByteArray,val secretKeysEncoded: Array<ByteArray>, val timeList: LongArray) : Serializable{
    companion object { const val serialVersionUID = 414232315885L }
}