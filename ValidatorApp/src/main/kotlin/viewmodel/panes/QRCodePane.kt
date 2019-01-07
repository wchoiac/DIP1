package viewmodel.panes

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import main.Helper

class QRCodePane : BorderPane() {
    private val qrView: ImageView
    private val backButton: Button

    init {
        qrView = ImageView()
        backButton = Button("Back")

        connectComponents()
        styleComponents()
        setCallbacks()
    }

    fun drawQRCode(str: String) {
        Helper.drawQRCode(qrView, str)
    }

    private fun connectComponents() {
        BorderPane.setAlignment(qrView, Pos.CENTER)
        BorderPane.setAlignment(backButton, Pos.CENTER)
        center = qrView
        bottom = backButton
    }

    private fun styleComponents() {

    }

    private fun setCallbacks() {

    }
}
