package xyz.medirec.medirec

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.support.v4.provider.DocumentFile
import android.widget.Toast
import xyz.medirec.medirec.pojo.MediRec
import java.security.KeyPair
import java.util.*


class DirChooserActivity : AppCompatActivity() {
    private var mode = ""
    private val code = 2019
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dir_chooser)
        mode = if(intent.getStringExtra("TYPE") == "SAVE") "SAVE" else "LOAD"
        loadChooser()
    }

    private fun loadChooser() {
        val i = if(mode == "SAVE") Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        } else Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(Intent.createChooser(i, "Select" + if(mode == "SAVE") " Directory" else " MediRec File"), code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(data != null) {
            when (requestCode) {
                code -> {
                    if (mode == "SAVE") {
                        val directory = DocumentFile.fromTreeUri(this, data.data!!)!!
                        val set = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
                        val temp = getSharedPreferences("UserData", Context.MODE_PRIVATE).getString("keyPair", "")!!
                        val keyPair = Helper.deserialize(temp) as KeyPair
                        val fileContents = Helper.serialize(MediRec(set, keyPair))
                        val timestamp = GregorianCalendar.getInstance().timeInMillis / 1000
                        val newFile = directory.createFile("*/*", "$timestamp.medirec")
                        val out = contentResolver.openOutputStream(newFile!!.uri)!!
                        out.write(fileContents)
                        out.close()
                        Toast.makeText(this, "File successfully exported as $timestamp.medirec", Toast.LENGTH_SHORT).show()
                    } else {
                        if(data.data!!.path!!.endsWith(".medirec")) {
                            val input = contentResolver.openInputStream(data.data!!)!!
                            val mediRec = Helper.deserialize(input.readBytes()) as MediRec
                            val editor = getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
                            editor.putString("keyPair", Helper.encodeToString(Helper.serialize(mediRec.keyPair)))
                            editor.putStringSet("TimeSet", mediRec.timeSet)
                            editor.apply()
                            Toast.makeText(this, "File successfully imported and overridden current identity", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Please choose .medirec file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
