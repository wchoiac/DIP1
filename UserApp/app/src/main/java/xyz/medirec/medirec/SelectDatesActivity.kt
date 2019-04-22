package xyz.medirec.medirec

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.CheckBox
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_select_dates.*
import java.security.KeyPair
import java.text.SimpleDateFormat
import java.util.*

class SelectDatesActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_dates)
        val initialSet = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
        val sortedSet = initialSet.toSortedSet()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        for(timestamp in sortedSet) {
            val box = CheckBox(this)
            val timestampLong = timestamp.toLong()
            val date = Date(Math.abs(timestampLong))
            val addText = if(timestampLong < 0) getString(R.string.info) else getString(R.string.mediRec)
            box.text = dateFormat.format(date) + " " + addText
            box.textSize = 20f
            dateList.addView(box)
        }

        generateKey.setOnClickListener {
            val list = mutableListOf<Long>()
            var i = 0
            for(timestamp in sortedSet)
                if((dateList.getChildAt(i++) as CheckBox).isChecked)
                    list.add(Math.abs(timestamp.toLong()))
            val intent = Intent(this, ViewQrActivity::class.java)
            intent.putExtra("keyPair", this.intent.getSerializableExtra("keyPair") as KeyPair)
            intent.putExtra("randomString", this.intent.getStringExtra("randomString"))
            intent.putExtra("timeList", list.toLongArray())
            startActivity(intent)
        }

        backMain.setOnClickListener {
            goToMenu()
        }

        selectAll.setOnCheckedChangeListener { _, isChecked ->
            for(i in 0 until sortedSet.size)
                (dateList.getChildAt(i) as CheckBox).isChecked = isChecked
        }

        discard.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder
                .setMessage("Are you sure to discard selected dates?\n(Discarded dates are irreparable)")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    var i = 0
                    while(i < dateList.childCount) {
                        if((dateList.getChildAt(i) as CheckBox).isChecked) {
                            val date = (dateList.getChildAt(i) as CheckBox).text
                            for(j in sortedSet) {
                                val dateString = dateFormat.format(Date(Math.abs(j.toLong())))
                                if(date.contains(dateString)) {
                                    sortedSet.remove(j)
                                    break
                                }
                            }
                            dateList.removeViewAt(i)
                        } else ++i
                    }

                    val editor = getSharedPreferences("UserData", MODE_PRIVATE).edit()
                    editor.putStringSet("TimeSet", sortedSet)
                    editor.apply()

                    if(sortedSet.isEmpty()) {
                        generateKey.performClick()
                    }
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss()}

            val dialog = builder.show()

            val myMsg = dialog.findViewById<TextView>(android.R.id.message)
            myMsg.gravity = Gravity.CENTER
            myMsg.textSize = 20f
        }

        //INITIALLY SELECT ALL
        selectAll.performClick()

        // IF THERE IS NO TIMESET -> CREATE KEY
        if(sortedSet.isEmpty()) generateKey.performClick()
    }

    override fun onBackPressed() {
        goToMenu()
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
    }
}
