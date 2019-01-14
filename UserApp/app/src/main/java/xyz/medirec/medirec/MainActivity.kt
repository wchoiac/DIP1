package xyz.medirec.medirec

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Context
import android.content.Intent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Helper.logInHash = getSharedPreferences("UserData", MODE_PRIVATE).getString("loginPIN", "")!!
        setContentView(R.layout.activity_main)
        initButtons()
        if(Helper.logInHash != "") {
            register_header.text = getString(R.string.PIN)
        }
    }

    private fun initButtons() {
        val randomIndexList = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).shuffled()

        val buttonList = listOf(
            button0, button1, button2, button3, button4,
            button5, button6, button7, button8, button9
        )

        for (i in 0 until buttonList.size) {
            buttonList[i].text = randomIndexList[i].toString()
            buttonList[i].textSize = 28f
            buttonList[i].setOnClickListener { e ->
                Helper.list.add((e as Button).text.toString().toInt())
                when {
                    Helper.list.isEmpty() -> register_header.text = getString(R.string.PIN)
                    Helper.list.size == 6 -> {
                        if(Helper.logInHash != "") {
                            logInListener()
                        } else {
                            registerListener()
                        }
                        Helper.list.clear()
                    }
                    else -> updateStar()
                }
            }
        }
        blankButton1.setOnClickListener {
            Helper.list.clear()
            register_header.text = getString(R.string.PIN)
        }

        blankButton2.setOnClickListener {
            Helper.list.removeAt(Helper.list.size - 1)
            when {
                Helper.list.isEmpty() -> register_header.text = getString(R.string.PIN)
                else -> updateStar()
            }
        }
    }

    private fun updateStar() {
        val text = StringBuilder()
        for (count in 0 until Helper.list.size)
            text.append("* ")
        register_header.text = text.toString()
    }

    private fun listToString(list: List<Int>): String {
        val sb = StringBuilder()
        for(value in list)
            sb.append(value)
        return sb.toString()
    }

    private fun logInListener() {
        if (tryLogIn(Helper.list)) {
            register_header.text = getString(R.string.PIN)
            logIn()
        } else {
            alertLogInFail(getString(R.string.Wrong_PIN))
            register_header.text = getString(R.string.PIN)
            initButtons()
        }
    }

    private fun registerListener() {
        if(Helper.temp == ""){
            Helper.temp = listToString(Helper.list)
            register_header.text = getString(R.string.Retype_PIN)
            initButtons()
        } else {
            register_header.text = if(Helper.temp != listToString(Helper.list)) {
                alertLogInFail("Mismatch PIN, Re-register PIN")
                getString(R.string.main_header)
            } else {
                setLogInHash(Helper.temp)
                getString(R.string.PIN)
            }
            Helper.temp = ""
        }
    }

    private fun setLogInHash (pin : String) {
        val hash = Helper.getHash(pin)
        Helper.logInHash = hash
        val prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("loginPIN", hash)
        editor.apply()
        initButtons()
    }

    private fun logIn() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.putExtra("FROM_ACTIVITY", "MAIN")
        startActivity(intent)
    }

    private fun alertLogInFail(message: String) {
        val myMsg = TextView(this)
        myMsg.text = message
        myMsg.top = 3
        myMsg.textSize = 24f
        myMsg.setPadding(0, 40, 0, 0)
        myMsg.gravity = Gravity.CENTER

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
            .setView(myMsg)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val positiveButtonLL = positiveButton.layoutParams as LinearLayout.LayoutParams
        positiveButtonLL.weight = 1000f
        positiveButton.layoutParams = positiveButtonLL
    }

    private fun tryLogIn(list: List<Int>): Boolean {
        return Helper.logInHash == Helper.getHash(listToString(list))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
