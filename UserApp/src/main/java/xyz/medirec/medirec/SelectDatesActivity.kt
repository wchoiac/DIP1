package xyz.medirec.medirec

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import kotlinx.android.synthetic.main.activity_select_dates.*
import java.security.PublicKey
import java.text.SimpleDateFormat
import java.util.*

class SelectDatesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_dates)
        val timeSet = getSharedPreferences("UserData", Context.MODE_PRIVATE).getStringSet("TimeSet", mutableSetOf())!!
        timeSet.sorted()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        for(timestamp in timeSet) {
            val box = CheckBox(this)
            val date = Date(timestamp.toLong())
            box.text = dateFormat.format(date)
            box.textSize = 20f
            dateList.addView(box)
        }

        generateKey.setOnClickListener {
            val list = mutableListOf<Long>()
            var i = 0
            for(timestamp in timeSet)
                if((dateList.getChildAt(i++) as CheckBox).isChecked)
                    list.add(timestamp.toLong())

            val intent = Intent(this, ViewQrActivity::class.java)
            intent.putExtra("pubKey", this.intent.getSerializableExtra("pubKey") as PublicKey)
            intent.putExtra("timeList", list.toLongArray())
            startActivity(intent)
        }

        backMain.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        selectAll.setOnCheckedChangeListener { _, isChecked ->
            for(i in 0 until timeSet.size)
                (dateList.getChildAt(i) as CheckBox).isChecked = isChecked
        }

        if(timeSet.isEmpty())
            generateKey.performClick()
    }
}
