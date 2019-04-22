package viewmodel

import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import main.Helper
import viewmodel.panes.*
import java.io.File
import kotlin.text.Charsets.UTF_8

object SceneManager {
    private val sizingIndex = if(Config.WIDTH < 1500) 1.5 else 2.0
    private val logInScene = Scene(LogInPane, Config.WIDTH / sizingIndex, Config.HEIGHT / sizingIndex)
    private val mainMenuScene = Scene(MainMenuPane, Config.WIDTH, Config.HEIGHT)
    private val scanScene = Scene(ScanPane, Config.WIDTH, Config.HEIGHT)
    private val qrScene = Scene(QRCodePane, Config.WIDTH, Config.HEIGHT)
    private val hospitalScene = Scene(ViewHospitalsPane, Config.WIDTH, Config.HEIGHT)
    private val addHospitalScene = Scene(AddHospitalPane, Config.WIDTH / sizingIndex, Config.HEIGHT / sizingIndex)
    private val addRemoveValidatorScene = Scene(AddRemoveValidatorsPane, Config.WIDTH / sizingIndex, Config.HEIGHT / sizingIndex)
    private val viewValidatorsScene = Scene(ViewValidatorsPane, Config.WIDTH, Config.HEIGHT)
    private val voteValidatorsScene = Scene(VoteValidatorsPane, Config.WIDTH, Config.HEIGHT)
    var stage: Stage? = null
        get() = field!!
        set(value) {
            if(value == null) return
            field = value
            field!!.title = "MediRec"
            field!!.icons.add(Image(Config.IMAGES["icon"]))
            field!!.setOnCloseRequest {
                println("Exiting...")
                (scanScene.root as ScanPane).disposeWebCamCamera()
                val file = File("${Config.BASE_PATH}/savedPatientsNotScanned.txt")
                if(Helper.nameToInfoMap.isNotEmpty()) {
                    val nameToInfoJsonString = StringBuilder("[")
                    Helper.nameToInfoMap.forEach { entry ->
                        nameToInfoJsonString.append("""{
                            "name": "${entry.key}",
                            "info": ${entry.value.first},
                            "pubKey": "${Helper.nameToPublicKey[entry.key]}",
                            "notFirst": ${entry.value.second}
                        },""")
                    }

                    file.writeText((nameToInfoJsonString.substring(0, nameToInfoJsonString.length - 1) + "]")
                            .replace(' ', '-')
                            .replace("\\s".toRegex(), ""), UTF_8)
                } else {
                    if(file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    private var lastPatientInfoScene : Scene? = null
    var lastPatientPane : PatientInfoPane? = null

    init {
        val scenes = arrayOf(
                logInScene, mainMenuScene, scanScene,
                qrScene, hospitalScene, addHospitalScene,
                addRemoveValidatorScene, viewValidatorsScene, voteValidatorsScene
        )
        addStylesheets(*scenes)
    }

    private fun addStylesheets(vararg scenes: Scene) {
        for (scene in scenes) {
            scene.stylesheets.add(Config.CSS_STYLES)
        }
    }

    private fun showScene(scene: Scene) {
        if (stage == null)
            return
        stage!!.scene = scene
        if(!stage!!.isShowing)
            stage!!.show()
        maximize((scene != addHospitalScene && scene != logInScene && scene != addRemoveValidatorScene))
    }

    private fun maximize(maximize : Boolean) {
        stage!!.isMaximized = false
        if(maximize)
            stage!!.isMaximized = true
        else
            stage!!.sizeToScene()
    }

    fun showLogInScene() {
        showScene(logInScene)
    }

    fun showMainMenuScene() {
        MainMenuPane.selectLast()
        showScene(mainMenuScene)
    }

    fun showScanScene(type: ScanPane.TYPE) {
        showScene(scanScene)
        (scanScene.root as ScanPane).startWebCam()
        (scanScene.root as ScanPane).type = type
    }

    fun showQRScene() {
        QRCodePane.showButtons()
        showScene(qrScene)
    }

    fun showPatientInfoScene(infoPane: PatientInfoPane) {
        val scene = Scene(infoPane, Config.WIDTH, Config.HEIGHT)
        lastPatientPane = infoPane
        lastPatientInfoScene = scene
        addStylesheets(scene)
        showScene(scene)
    }

    fun showHospitalViewScene() {
        showScene(hospitalScene)
    }

    fun showAddHospitalScene() {
        showScene(addHospitalScene)
    }

    fun showAddRemoveValidatorScene() {
        showScene(addRemoveValidatorScene)
    }

    fun showVoteValidatorsScene() {
        VoteValidatorsPane.loadList()
        showScene(voteValidatorsScene)
    }

    fun showViewValidatorsScene() {
        ViewValidatorsPane.loadList()
        showScene(viewValidatorsScene)
    }

}
