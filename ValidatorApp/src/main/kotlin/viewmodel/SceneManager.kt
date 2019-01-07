package viewmodel

import javafx.scene.Scene
import javafx.stage.Stage
import viewmodel.panes.*

/**
 * Handles scene changing logic
 */
object SceneManager {
    private val logInScene = Scene(LogInPane(), (Config.WIDTH shr 1).toDouble(), (Config.HEIGHT shr 1).toDouble())
    private val mainMenuScene = Scene(MainMenuPane(), (Config.WIDTH).toDouble(), (Config.HEIGHT).toDouble())
    private val scanScene = Scene(ScanPane(), (Config.WIDTH).toDouble(), (Config.HEIGHT).toDouble())
    private val qrScene = Scene(QRCodePane(), (Config.WIDTH).toDouble(), (Config.HEIGHT).toDouble())
    private var stage: Stage? = null

    val qrCodePane: QRCodePane
        get() = qrScene.root as QRCodePane

    init {
        val scenes = arrayOf(logInScene, mainMenuScene, scanScene, qrScene)
        addStylesheets(*scenes)
    }

    private fun addStylesheets(vararg scenes: Scene) {
        for (scene in scenes) {
            scene.stylesheets.add(Config.CSS_STYLES)
        }
    }

    fun setStage(stage: Stage) {
        this.stage = stage
        this.stage!!.title = "MediRec"
        stage.setOnCloseRequest {
            println("Exiting...")
            (scanScene.root as ScanPane).disposeWebCamCamera()
        }
    }

    private fun showScene(scene: Scene) {
        if (stage == null)
            return

        stage!!.hide()
        stage!!.scene = scene
        stage!!.show()
    }

    fun showLogInScene() {
//        showScene(logInScene)
        showMainMenuScene()
    }

    fun showMainMenuScene() {
        showScene(mainMenuScene)
    }

    fun showScanScene(isSign: Boolean) {
        showScene(scanScene)
        (scanScene.root as ScanPane).startWebCam()
        (scanScene.root as ScanPane).setIsSign(isSign)
    }

    fun showQRScene() {
        showScene(qrScene)
    }
}
