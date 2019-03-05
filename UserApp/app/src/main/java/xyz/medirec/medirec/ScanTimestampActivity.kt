package xyz.medirec.medirec

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import com.beust.klaxon.JsonArray
import com.beust.klaxon.Parser
import com.google.zxing.integration.android.IntentIntegrator

class ScanTimestampActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)
        startQRScanner()
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan any timestamp QR Code")
        integrator.captureActivity = CapturePortrait::class.java
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
                goToMenu()
            } else {
                println(result.contents)
                val arr = Parser.default().parse(StringBuilder(result.contents)) as JsonArray<*>
                val set = mutableSetOf<String>()
                arr.forEach {
                    set.add(it.toString())
                }
                val currentSet = getSharedPreferences("UserData", MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())
                currentSet?.forEach {
                    set.add(it)
                }
                val editor = getSharedPreferences("UserData", MODE_PRIVATE).edit()
                editor.putStringSet("TimeSet", set)
                editor.apply()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        goToMenu()
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
