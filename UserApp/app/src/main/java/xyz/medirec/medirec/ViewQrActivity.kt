package xyz.medirec.medirec

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_view_qr.*
import xyz.medirec.medirec.pojo.KeyTime
import java.security.PublicKey
import javax.crypto.SecretKey

class ViewQrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_qr)
        val pubKey = intent.getSerializableExtra("pubKey") as PublicKey
        val hash = intent.getStringExtra("priKeyHash")
        val timeList = intent.getLongArrayExtra("timeList")

        createQRcode(pubKey, hash, timeList)
        GoToMenuButton.setOnClickListener {
            goToMenu()
        }
    }

    private fun createQRcode(pubKey: PublicKey, hash: String, timeList: LongArray) {
//        pubKey as ECPublicKey
//        val publicKeyProperties = KeyProperties(pubKey.encoded)
        val secretKeyList = mutableListOf<ByteArray>()
        timeList.forEach { secretKeyList.add(Helper.generateSecretKey((hash + it).toCharArray()).encoded) }
        val serialized = Helper.serialize(
            KeyTime(
                pubKey.encoded,
                secretKeyList.toTypedArray(),
                timeList
            )
        )
        Helper.drawQRCode(qrCodeView, serialized, windowManager)
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
