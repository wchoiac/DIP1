package viewmodel.panes

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager
import java.io.BufferedInputStream
import java.io.File
import java.security.KeyStore
import java.security.cert.CertificateFactory
import kotlin.text.Charsets.UTF_8

class LogInPane : BorderPane() {

    private val container: VBox = VBox(35.0)
    private val title = Label("SIGN IN")
    private val usernameLabel = Label("Username")
    private val username = TextField()
    private val passwordLabel = Label("Password")
    private val invalidWarning = Label("")
    private val password = PasswordField()
    private val submitBtn = Button("Sign In")

    init {
        container.alignment = Pos.CENTER
        username.promptText = "Username"
        password.promptText = "Password"
        invalidWarning.visibleProperty().value = false
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    private fun connectComponents() {
        val hBox1 = HBox(20.0, usernameLabel, username)
        hBox1.alignment = Pos.CENTER
        val hBox2 = HBox(20.0, passwordLabel, password)
        hBox2.alignment = Pos.CENTER
        container.children.addAll(
            title,
            hBox1,
            hBox2,
            invalidWarning,
            submitBtn
        )
        container.alignment = Pos.CENTER
        center = container
    }

    private fun styleComponents() {
        invalidWarning.styleClass.add("warning-text")
    }

    private fun setCallbacks() {
        submitBtn.setOnMouseClicked {
            val username = this.username.text
            val password = this.password.text
            when {
                username == "" -> {
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.USERNAME_WARNING
                }
                password == "" -> {
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.PASSWORD_WARNING
                }
                else -> {
                    FuelManager.instance.keystore = KeyStore.getInstance("pkcs12", Helper.getProvider())
                    val keyStore = FuelManager.instance.keystore!!

                    keyStore.load(null)
                    val fis = File("C:/Users/gusrb/Downloads/api.pem").inputStream()
                    val bis = BufferedInputStream(fis)
                    val cf = CertificateFactory.getInstance("X.509")

                    while (bis.available() > 0) {
                        val cert = cf.generateCertificate(bis)
                        keyStore.setCertificateEntry("MediRec"+bis.available(), cert)
                    }

                    val token = "https://13.209.89.64:443/api/user/login"
                        .httpPost()
                        .header(mapOf("Content-Type" to "application/json; charset=utf-8"))
                        .body("""
                            { "username": "$username", "password": "$password"}
                        """, UTF_8)
                        .responseString().third.component1()

                    if(token != null && token != "") {
                        invalidWarning.visibleProperty().value = false
                        Helper.token = token
                        SceneManager.showMainMenuScene()
                    } else {
                        invalidWarning.visibleProperty().value = true
                        invalidWarning.text = Config.WRONG_WARNING
                    }
                }
            }
        }
    }
}
