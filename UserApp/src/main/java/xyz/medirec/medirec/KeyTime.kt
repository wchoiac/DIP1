package xyz.medirec.medirec

import java.io.Serializable

class KeyTime(val key: KeyProperties, val timeList: LongArray) : Serializable{
    companion object { const val serialVersionUID = 414232315885L }
}