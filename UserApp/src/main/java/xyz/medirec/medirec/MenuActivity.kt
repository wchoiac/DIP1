package xyz.medirec.medirec

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.Gravity
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.content_menu.*
import java.io.*
import java.security.KeyPair
import java.security.PublicKey

class MenuActivity : AppCompatActivity() {
    private var keyPair : KeyPair? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        askForPermission(Manifest.permission.CAMERA)
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        askForPermission(Manifest.permission.READ_EXTERNAL_STORAGE)

        if(intent.getStringExtra("FROM_ACTIVITY") == "MAIN")
            Snackbar.make(logInSuccess, getString(R.string.logInSuccess), Snackbar.LENGTH_SHORT).show()
        val keyPairSerialized = if(getSharedPreferences("UserData", MODE_PRIVATE).getString("keyPair", "")!! == ""){
            try {
                val file = File("/keyPair/keyPair.pkcs12")
                if(file.exists()){
                    val keyPair = Helper.loadFromPKCS12("/keyPair/keyPair")
                    serialize(keyPair)
                } else {
                    generateAndSaveKeyPair()
                }
            } catch (e: Exception) {
                generateAndSaveKeyPair()
            }
        } else getSharedPreferences("UserData", MODE_PRIVATE).getString("keyPair", "")!!

        keyPair = deserialize(keyPairSerialized)
        val buttons = listOf(
            myQRcode, importID, exportID, scanQRcode, changePin, exit
        )

        buttons[0].setOnClickListener { goToQrCodeActivity(keyPair!!.public) }
        buttons[1].setOnClickListener { importID() }
        buttons[2].setOnClickListener { exportID() }
        buttons[3].setOnClickListener { scanQRcode() }
        buttons[4].setOnClickListener { resetPIN() }
        buttons[5].setOnClickListener { exitApp() }
    }

    private fun askForPermission(permission: String) {

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
        ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

        // IF NEEDED -> Using if statement
        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
    }

    private fun goToQrCodeActivity(pubKey: PublicKey) {
        val intent = Intent(this, SelectDatesActivity::class.java)
        intent.putExtra("pubKey", pubKey)
        startActivity(intent)
    }

    private fun scanQRcode() {
        val intent = Intent(this, ScanQrActivity::class.java)
        startActivity(intent)
    }

    private fun importID() {

    }

    private fun exportID() {

    }

    private fun resetPIN() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage(R.string.reallyChangePin)
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                val editor = getSharedPreferences("UserData", MODE_PRIVATE).edit()
                editor.putString("loginPIN", null)
                editor.apply()
                startActivity(Intent(this, MainActivity::class.java))
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss()}

        val dialog = builder.show()

        val myMsg = dialog.findViewById<TextView>(android.R.id.message)
        myMsg.gravity = Gravity.CENTER
        myMsg.textSize = 20f

    }

    private fun exitApp() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setMessage(R.string.reallyExit)
            .setCancelable(false)
            .setPositiveButton("Exit") { _, _ ->
                moveTaskToBack(true)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(0)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss()}

        val dialog = builder.show()

        val myMsg = dialog.findViewById<TextView>(android.R.id.message)
        myMsg.gravity = Gravity.CENTER
        myMsg.textSize = 20f
    }

    private fun generateAndSaveKeyPair(): String{
        val keyPair = Helper.generateKeyPair("EC")
        val keyPairSerialized = serialize(keyPair)
        val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
        editor.putString("keyPair", keyPairSerialized)
        editor.apply()
        return keyPairSerialized
    }

    private fun serialize(keyPair: KeyPair): String {
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).writeObject(keyPair)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun deserialize(strings: String): KeyPair {
        val serializedMember = Base64.decode(strings, Base64.DEFAULT)
        return ObjectInputStream(ByteArrayInputStream(serializedMember)).readObject() as KeyPair
    }
}
