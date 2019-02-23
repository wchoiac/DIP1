package viewmodel.panes

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Insets
import main.SecurityHelper
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager
import xyz.medirec.medirec.pojo.KeyTime
import xyz.medirec.medirec.pojo.PatientIdentity
import java.lang.Exception
import java.security.interfaces.ECPublicKey
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.spec.SecretKeySpec
import javafx.scene.control.ListCell
import javafx.scene.image.ImageView

class PatientInfoPane(private val keyTime: KeyTime) : BorderPane() {
    private val container = VBox(30.0)
    private val nameLabel = Label("Name")
    private val name = TextField()
    private val genderLabel = Label("Gender")
    private val gender = TextField()
    private val identificationLabel = Label("ID Number")
    private val identification = TextField()
    private val birthDateLabel = Label("Date of Birth")
    private val birthDate = TextField()
    private val nationalityLabel = Label("Nationality")
    private val nationality = TextField()
    private val addressLabel = Label("Address")
    private val address = TextField()
    private val phoneNumberLabel = Label("Phone Number")
    private val phoneNumber = TextField()
    private val extraLabel = Label("Extra")
    private val extra = TextField()
    private val cancelButton = Button("Cancel")
    private val createRecordButton = Button("Create Patient Record")
    private val errorLabel = Label()
    private val qrView = ImageView()
    private val drawQR = Button("Draw Selected Timestamps")
    private val timeList = FXCollections.observableArrayList<HBox>()
    private val listView = ListView<HBox>(timeList)
    private val timeToRecord = mutableMapOf<String, PatientIdentity>()
    private val timeToStampMap = mutableMapOf<String, String>()
    var allRecordsRaw: Array<String>? = null
    private var allRecordsTimestamp: LongArray? = null

    init {
        qrView.fitWidth = Config.WIDTH * 0.2
        qrView.fitHeight = Config.WIDTH * 0.2
        allRecordsRaw = findRecord(Helper.generatePublicKey(keyTime.pubKeyEncoded))
        if(allRecordsRaw != null) fillUpData()
        listView.fixedCellSize = 50.0
        errorLabel.visibleProperty().set(false)
        listView.selectionModel.selectionMode = SelectionMode.MULTIPLE
        listView.setCellFactory {
            object : ListCell<HBox>() {
                init { style = "-fx-padding: 0 10 0 10" }
                override fun updateItem(item: HBox?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty || item == null) null else item
                }
            }
        }
        sizeComponents()
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    private fun sizeComponents() {
        name.prefColumnCount = 15
        gender.prefColumnCount = 5
        phoneNumber.prefColumnCount = 15
        identification.prefColumnCount = 60
        nationality.prefColumnCount = 25
        birthDate.prefColumnCount = 25
        address.prefColumnCount = 62
        extra.prefColumnCount = 64
    }

    private fun fillUpData() {
        try {
            val gson = Gson()
            for (i in 0 until allRecordsTimestamp!!.size) {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getDefault()
                val timeString = dateFormat.format(Date(allRecordsTimestamp!![i]))
                timeToStampMap[timeString] = allRecordsTimestamp!![i].toString()
                var check = false
                for (j in 0 until keyTime.timeList.size) {
                    if (allRecordsTimestamp!![i] == keyTime.timeList[j]) {
                        check = true
                        val key = SecretKeySpec(keyTime.secretKeysEncoded[j], "AES")
                        val recordJson = String(
                            SecurityHelper.decryptAES(
                                Helper.decodeFromString(allRecordsRaw!![i]), key, ByteArray(16)
                            )
                        )
                        timeToRecord[timeString] = gson.fromJson(recordJson, PatientIdentity::class.java)
                        break
                    }
                }

                val hBox = HBox(Label(timeString))
                hBox.alignment = Pos.CENTER
                if(!check) hBox.styleClass.add("grey-background")
                timeList.add(hBox)
            }

        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
    }

    private fun findRecord(publicKey: ECPublicKey) : Array<String>? {
        val response = "${Config.BASE_URL}/patient/get-patient-short-info-list"
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; charset=utf-8", "Authorization" to Helper.token))
            .body("""{
                "content": "${Helper.encodeToString(getIdentifier(publicKey))}"
                }""".replace("\\s".toRegex(), ""), Charsets.UTF_8)
            .responseString()
//        println(response)
        return if(response.second.statusCode == 200) {
            val resultArr = Parser().parse(StringBuilder(response.third.component1()!!)) as JsonArray<*>
            val blockHashList = mutableListOf<String>()
            val targetIdentifierList = mutableListOf<String>()
            val timestampList = mutableListOf<Long>()
            for(each in resultArr) {
                each as JsonObject
                blockHashList.add((each["location"] as JsonObject)["blockHash"] as String)
                targetIdentifierList.add((each["location"] as JsonObject)["targetIdentifier"] as String)
                timestampList.add(each["timestamp"] as Long)
            }
            if(resultArr.size > 0) {
                allRecordsTimestamp = timestampList.toLongArray()
                findRecordDetails(blockHashList.toTypedArray(), targetIdentifierList.toTypedArray())
            } else null
        } else null
    }

    private fun findRecordDetails(blockHash: Array<String>, targetIdentifier: Array<String>) : Array<String>? {
        val sb = StringBuilder()
        for(i in 0 until blockHash.size)
            sb.append("""{ "blockHash": "${blockHash[i]}", "targetIdentifier": "${targetIdentifier[i]}" }""")
                .append(if(i + 1 != blockHash.size) "," else "")
        val response = "${Config.BASE_URL}/patient/get-patient-info-content-list"
            .httpPost()
            .header(mapOf("Content-Type" to "application/json; charset=utf-8", "Authorization" to Helper.token))
            .body("""[
                $sb
            ]""".replace("\\s".toRegex(), ""), Charsets.UTF_8).responseString()

        return if(response.second.statusCode == 200) {
            val resultList = mutableListOf<String>()
            val resultArr = Parser().parse(StringBuilder(response.third.component1()!!)) as JsonArray<*>
            resultArr.forEach {
                it as JsonObject
                resultList.add(it["encryptedInfo"] as String)
            }
            resultList.toTypedArray()
        } else null
    }

    private fun getIdentifier(publicKey: ECPublicKey) : ByteArray {
        var hash = ByteArray(0)
        try {
            hash = SecurityHelper.hash(
                SecurityHelper.getCompressedRawECPublicKey(publicKey),
                Config.BLOCKCHAIN_HASH_ALGORITHM
            )
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return Arrays.copyOfRange(hash, hash.size - Config.IDENTIFIER_LENGTH, hash.size)
    }

    private fun connectComponents() {
        container.alignment = Pos.CENTER
        val buttonBar = HBox(30.0, cancelButton, createRecordButton, drawQR)
        container.children.addAll(
            HBox(30.0, HBox(15.0, nameLabel, name), HBox(15.0, genderLabel, gender), HBox(15.0, phoneNumberLabel, phoneNumber)),
            HBox(15.0, identificationLabel, identification),
            HBox(30.0, HBox(15.0, nationalityLabel, nationality), HBox(15.0, birthDateLabel, birthDate)),
            HBox(15.0, addressLabel, address),
            HBox(15.0, extraLabel, extra),
            HBox(15.0, errorLabel),
            buttonBar
        )
        container.children.forEach{
            (it as HBox).alignment = Pos.CENTER
        }
        listView.setPrefSize(Config.WIDTH * 0.25, Config.HEIGHT * 0.85)
        val leftBox = VBox(listView)
        leftBox.alignment = Pos.CENTER
        leftBox.padding = Insets(0.0, 0.0, 0.0, Config.WIDTH * 0.01)
        val topBox = HBox(Config.WIDTH * 0.33, Label("Records"), Label("Information"), Label("Timestamp"))
        topBox.alignment = Pos.CENTER
        topBox.padding = Insets(Config.HEIGHT * 0.02, 0.0, 0.0, Config.WIDTH * 0.01)
        val rightBox = VBox(qrView)
        rightBox.alignment = Pos.CENTER
        rightBox.padding = Insets(0.0, Config.WIDTH * 0.01, 0.0, 0.0)
        this.top = topBox
        this.left = leftBox
        this.right = rightBox
        this.center = container
    }

    private fun styleComponents() {
        nameLabel.styleClass.add("small-size")
        identificationLabel.styleClass.add("small-size")
        errorLabel.styleClass.add("warning-text")
    }

    private fun setCallbacks() {
        cancelButton.setOnAction {
            SceneManager.showMainMenuScene()
        }

        createRecordButton.setOnAction {
            errorLabel.visibleProperty().set(true)
            when {
                name.text == "" -> {
                    name.requestFocus()
                    errorLabel.text = "Please enter the name"
                }
                gender.text == "" || !(gender.text == "M" || gender.text =="F") -> {
                    gender.requestFocus()
                    errorLabel.text = "Please enter the gender as 'M' or 'F'"
                }
                birthDate.text == "" -> {
                    birthDate.requestFocus()
                    errorLabel.text = "Please enter the date of birth"
                }
                identification.text == "" -> {
                    identification.requestFocus()
                    errorLabel.text = "Please enter the identification number"
                }
                nationality.text == "" -> {
                    nationality.requestFocus()
                    errorLabel.text = "Please enter the correct blood type"
                }
                address.text.contains("[^0-9.]".toRegex()) -> {
                    address.requestFocus()
                    errorLabel.text = "Please enter correct address value"
                }
                phoneNumber.text.contains("[^0-9.]".toRegex()) -> {
                    phoneNumber.requestFocus()
                    errorLabel.text = "Please enter correct address value"
                }
                else -> {
                    errorLabel.visibleProperty().set(false)
                    val patientIdentity = PatientIdentity(
                            name = if(name.text == "") null else name.text,
                            gender = if(gender.text == "") null else gender.text,
                            birthDate = if(birthDate.text == "") null else birthDate.text,
                            identificationNumber = if(identification.text == "") null else identification.text,
                            nationality = if(nationality.text == "") null else nationality.text,
                            address =if(address.text == "") null else address.text,
                            phoneNum = if(phoneNumber.text == "") null else phoneNumber.text,
                            extra = if(extra.text == "") null else extra.text
                    )
                    Helper.nameToInfoMap[name.text] = Pair(
                            Gson().toJson(patientIdentity).replace("\\s".toRegex(), ""), timeList.isNotEmpty())
                    Helper.nameToPublicKey[name.text] = Helper.encodeToString(keyTime.pubKeyEncoded)
                    MainMenuPane.addToList(name.text)
                    SceneManager.showMainMenuScene()
                }
            }
        }

        listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            val timestamp = (newValue.children.first() as Label).text
            name.text = timeToRecord[timestamp]?.name ?: ""
            gender.text = timeToRecord[timestamp]?.gender ?: ""
            birthDate.text = timeToRecord[timestamp]?.birthDate ?: ""
            identification.text = timeToRecord[timestamp]?.identificationNumber ?: ""
            nationality.text = timeToRecord[timestamp]?.nationality ?: ""
            address.text = timeToRecord[timestamp]?.address ?: ""
            phoneNumber.text = timeToRecord[timestamp]?.phoneNum ?: ""
        }

        listView.selectionModel.selectFirst()

        drawQR.setOnAction {
            val items = listView.selectionModel.selectedItems
            val timestampList = mutableListOf<String>()
            items.forEach {hBox ->
                timestampList.add("-" + timeToStampMap[(hBox.children.first() as Label).text]!!)
            }

            Platform.runLater {
                Helper.drawQRCode(qrView, timestampList.toString())
            }
        }
    }
}