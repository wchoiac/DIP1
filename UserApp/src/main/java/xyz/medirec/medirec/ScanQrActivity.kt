package xyz.medirec.medirec

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.content_scan_qr.*
import java.lang.IllegalArgumentException
import java.security.PrivateKey
import javax.crypto.SecretKey


class ScanQrActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)
        startQRScanner()
        backToMain_scan.setOnClickListener {
            goToMenu()
        }
        scanButton.setOnClickListener {
            startQRScanner()
        }
    }

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setBeepEnabled(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan any MediRec QR Code")
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
                updateText(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Check
     * if the input is encrypted AES key -> decrypt it with private key and return AES key
     * if the input is hash of the record -> encrypt it with private key and return signature
     * else raise pop up and re-scan
     *
     * @param scannedStr string format of QR code
     */
    private fun updateText(scannedStr: String) {
        try {
            val byteForm = Helper.decodeFromString(scannedStr)
            val privateKey = intent.getSerializableExtra("pvtKey") as PrivateKey
            val decrypted = Helper.decrypt(byteForm, privateKey, "RSA")
            val secretKey= Helper.deserialize(decrypted) as SecretKey
// GENERATE AES KEY
//            val currentTimestamp = GregorianCalendar.getInstance().timeInMillis
//            val secret = Helper.getAESKey(privateKey, currentTimestamp.toString())
//            val intent = Intent(this, GenerateKeyActivity::class.java)
            Helper.drawQRCode(imageView, secretKey, windowManager)

// ADD THE TIMESTAMP TO SET
//            val set = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
//            val newSet = set.toMutableSet()
//            newSet.add(currentTimestamp.toString())
//            val editor = getSharedPreferences("UserData", MODE_PRIVATE).edit()
//            editor.putStringSet("TimeSet", newSet)
//            editor.apply()


//            val pubKey = p as PublicKey
//            val randomChar = "abac".toCharArray()
//            val aes = Helper.generateSecretKey(randomChar)
//            val serialized = Helper.serialize(aes)
//            val encoded = Helper.encrypt(serialized, pubKey, "RSA")
//            QRCodeWriter().encode(Helper.encodeToString(encoded), BarcodeFormat.QR_CODE, 100, 100)

        } catch (e: Exception) {
            try {
                //If it is hash ( check if it is hash or throw exception)
                if(scannedStr.length * 4 != 256 || scannedStr.contains(Regex("\\D[^a-f]",RegexOption.IGNORE_CASE)))
                    throw IllegalArgumentException()
                val privateKey = intent.getSerializableExtra("pvtKey") as PrivateKey
                val signature = Helper.generateSignature(privateKey, scannedStr.toByteArray(), "RSA")
                Helper.drawQRCode(imageView, signature, windowManager)
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid QR Code. Please re-scan", Toast.LENGTH_LONG).show()
                startQRScanner()
            }
        }
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
