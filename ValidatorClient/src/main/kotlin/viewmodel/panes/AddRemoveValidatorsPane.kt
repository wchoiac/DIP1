package viewmodel.panes

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.httpPost
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import main.Helper
import main.SecurityHelper
import viewmodel.Config
import viewmodel.SceneManager
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import java.security.PublicKey
import java.util.*

object AddRemoveValidatorsPane : BorderPane() {
    private val authorityName = TextField()
    private val authorityNameLabel = Label("New Authority Name")
    private val publicKeyImport = Button("Import public key")
    private val publicKeyImportLabel = Label("New Authority's Public Key")
    private val publicKeyConfirm = Label("Public Key Imported!")
    private val addButton = Button("Start Voting")
    private val backButton = Button("Back To Main")
    private val warningText = Label()

    private var publicKey : PublicKey? = null
    private var publicKeyFile : File? = null

    init {
        authorityName.promptText = "Authority Name"
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    private fun connectComponents() {
        val container = VBox(30.0)
        container.alignment = Pos.CENTER
        publicKeyConfirm.visibleProperty().set(false)
        container.children.addAll(
                HBox(30.0, publicKeyConfirm),
                HBox(30.0, authorityNameLabel, authorityName),
                HBox(30.0, publicKeyImportLabel, publicKeyImport),
                HBox(30.0, warningText)
        )
        container.children.forEach {
            (it as HBox).alignment = Pos.CENTER
        }
        this.center = container
        val bottomBox = HBox(30.0, backButton, addButton)
        bottomBox.alignment = Pos.CENTER
        bottomBox.padding = Insets(20.0, 20.0, 20.0, 20.0)
        this.bottom = bottomBox
    }

    private fun styleComponents() {
        publicKeyConfirm.styleClass.add("success-text")
        warningText.styleClass.add("warning-text")
    }

    private fun setCallbacks() {
        publicKeyImport.setOnAction {
            val fc = FileChooser()
            fc.title = "Public Key Import"
            fc.initialDirectory = File("./src/main/resources")
            fc.extensionFilters.clear()
            fc.extensionFilters.addAll(FileChooser.ExtensionFilter("pem files", "*.pem"))
            try {
                val file = fc.showOpenDialog(SceneManager.stage)
                if (file != null) {
                    publicKeyFile = file
                    publicKey = SecurityHelper.getPublicKeyFromPEM(file, "EC")
                    publicKeyConfirm.visibleProperty().set(true)
                } else {
                    publicKeyConfirm.visibleProperty().set(false)
                }
            } catch(e: Exception) {
                println("Wrong public key file: ${e.message}")
            }
        }

        addButton.setOnAction {
            warningText.visibleProperty().set(true)
            when {
                authorityName.text == "" -> {
                    authorityName.requestFocus()
                    warningText.text = "Please input name of the medical organization"
                }
                publicKey == null -> {
                    warningText.text = "Please import the medical organization's public key"
                }
                else -> {
                    warningText.visibleProperty().set(true)
                    val response = "${Config.BASE_URL}/authority/vote/cast"
                        .httpPost()
                        .header(
                                mapOf(
                                        "Content-Type" to "application/json; charset=utf-8",
                                        "Authorization" to Helper.token
                                )
                        )
                        .body("""{
                            "beneficiary": {
                                "name": "${authorityName.text}",
                                "ecPublicKey": "${Helper.encodeToString(publicKey!!.encoded)}",
                                "keyDEREncoded": true
                            },
                            "add": true,
                            "agree": true
                        }""".replace("\\s".toRegex(), ""), Charsets.UTF_8)
                        .responseString()
                    if(response.second.statusCode == 200) {
                        try {
                            if (response.third.component1()!!.toInt() != 0)
                                throw IllegalStateException("FAILED TO REGISTER: ${response.third.component1()!!}")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        println(response)
                    }
                    toMainMenu()
                }
            }
        }

        backButton.setOnAction {
            toMainMenu()
        }
    }

    private fun toMainMenu() {
        SceneManager.showMainMenuScene()
    }
}