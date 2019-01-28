package xyz.medirec.medirec

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_view_qr.*
import xyz.medirec.medirec.pojo.KeyTime
import java.security.KeyPair

class ViewQrActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_qr)
        val keyPair = intent.getSerializableExtra("keyPair") as KeyPair
        val timeList = intent.getLongArrayExtra("timeList")
        val randomStr = intent.getStringExtra("randomString")
        createQRcode(keyPair, timeList, randomStr)
        GoToMenuButton.setOnClickListener {
            goToMenu()
        }
    }

    override fun onBackPressed() {
        goToMenu()
    }

    private fun createQRcode(keyPair: KeyPair, timeList: LongArray, randomString: String) {
        val secretKeyList = mutableListOf<ByteArray>()
        timeList.forEach { secretKeyList.add(Helper.getAESKey(keyPair.private, it.toString(), randomString).encoded) }
        val serialized = Helper.serialize(KeyTime(keyPair.public.encoded, secretKeyList.toTypedArray(), timeList))
        Helper.drawQRCode(qrCodeView, serialized, windowManager)
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
