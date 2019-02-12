package viewmodel.panes

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import main.Helper
import viewmodel.Config
import viewmodel.SceneManager

object QRCodePane : BorderPane() {
    private val container = VBox(50.0)
    private val qrView = ImageView()
    private val backButton = Button("Back")
    private val scanButton = Button("Scan Signature")
    private val bottomBar = HBox(30.0)

    init {
        qrView.fitWidth = Config.WIDTH * 0.2
        qrView.fitHeight = Config.WIDTH * 0.2
        bottomBar.alignment = Pos.CENTER
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    fun drawQRCode(str: String) {
        Helper.drawQRCode(qrView, str)
        qrView.visibleProperty().set(true)
    }

    fun removeQRCode() {
        qrView.visibleProperty().set(false)
    }

    fun hideButtons() {
        if(bottomBar.children.contains(scanButton))
            bottomBar.children.remove(scanButton)
        if(bottomBar.children.contains(backButton))
            bottomBar.children.remove(backButton)
    }

    fun showButtons() {
        if(!bottomBar.children.contains(backButton))
            bottomBar.children.add(backButton)
        if(!bottomBar.children.contains(scanButton))
            bottomBar.children.add(scanButton)
    }

    private fun connectComponents() {
        bottomBar.children.addAll(backButton, scanButton)
        container.children.addAll(
            qrView, bottomBar
        )
        container.alignment = Pos.CENTER
        center = container
    }

    private fun styleComponents() {

    }

    private fun setCallbacks() {
        backButton.setOnAction {
            SceneManager.showMainMenuScene()
        }

        scanButton.setOnAction {
            SceneManager.showScanScene(ScanPane.TYPE.SIGNATURE)
        }
    }
}
