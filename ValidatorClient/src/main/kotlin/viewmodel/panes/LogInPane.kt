package viewmodel.panes

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import javafx.event.Event
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager
import java.io.BufferedInputStream
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateFactory
import kotlin.text.Charsets.UTF_8


object LogInPane : BorderPane() {

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
        username.text = "root"
        password.text = "1234"
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
        setOnKeyPressed {
            if(it.code == KeyCode.ENTER)
                Event.fireEvent(
                    submitBtn, MouseEvent(MouseEvent.MOUSE_CLICKED, 0.0, 0.0, 0.0, 0.0,
                        MouseButton.PRIMARY, 1, true, true, true,
                        true, true, true, true,
                        true, true, true, null)
                )
        }

        submitBtn.setOnMouseClicked {
            val username = this.username.text
            val password = this.password.text
            when {
                username == "" -> {
                    this.username.requestFocus()
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.USERNAME_WARNING
                }
                password == "" -> {
                    this.password.requestFocus()
                    invalidWarning.visibleProperty().value = true
                    invalidWarning.text = Config.PASSWORD_WARNING
                }
                else -> {
                    FuelManager.instance.keystore = KeyStore.getInstance("pkcs12", Helper.getProvider())
                    val keyStore = FuelManager.instance.keystore!!
                    keyStore.load(null)
                    val fis = Paths.get(Config.CERT_URL.toURI()).toFile().inputStream()
                    val bis = BufferedInputStream(fis)
                    val cf = CertificateFactory.getInstance("X.509")
                    while (bis.available() > 0) {
                        val cert = cf.generateCertificate(bis)
                        keyStore.setCertificateEntry("MediRec"+bis.available(), cert)
                    }
                    bis.close()

                    val response = "${Config.BASE_URL}/user/login"
                        .httpPost()
                        .header(mapOf("Content-Type" to "application/json; charset=utf-8"))
                        .body("""{ "username": "$username", "password": "$password"}""", UTF_8)
                        .responseString()

                    if(response.third.component1() != null && response.third.component1() != "") {
                        invalidWarning.visibleProperty().value = false
                        Helper.token = response.third.component1()!!
                        SceneManager.showMainMenuScene()
                    } else {
                        println("FAILED TO SIGN IN")
                        invalidWarning.visibleProperty().value = true
                        invalidWarning.text = Config.WRONG_WARNING
                    }
                }
            }
        }
    }
}
