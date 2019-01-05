package xyz.medirec.medirec

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.content_view_qr.*
import java.security.PublicKey

class ViewQrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_qr)
        val pubKey = intent.getSerializableExtra("pubKey") as PublicKey
        val timeList = intent.getLongArrayExtra("timeList")

        createQRcode(pubKey, timeList)
        GoToMenuButton.setOnClickListener {
            goToMenu()
        }
    }

    private fun createQRcode(pubKey: PublicKey, timeList: LongArray) {
        val publicKeyProperties = KeyProperties(pubKey.encoded, pubKey.format, pubKey.algorithm)
        val serialized = Helper.serialize(KeyTime(publicKeyProperties, timeList))
        Helper.drawQRCode(qrCodeView, serialized, windowManager)
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
