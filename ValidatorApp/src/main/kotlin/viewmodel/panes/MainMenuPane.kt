package viewmodel.panes

import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import viewmodel.Config
import viewmodel.SceneManager

class MainMenuPane : BorderPane() {
    private val userContainer = HBox(50.0)
    private val hospitalContainer = HBox(50.0)
    private val validatorContainer = HBox(50.0)
    private val userButtons = arrayOf(Button("Create Record"), Button("Modify Record"), Button("View Record"))
    private val userImages = arrayOf(
        Image(Config.IMAGES["createRecord"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["modifyRecord"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["viewRecord"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    )
    private val hospitalButtons = arrayOf(Button("Authorize Hospital"), Button("Disqualify Hospital"), Button("View Hospitals"))
    private val hospitalImages = arrayOf(
        Image(Config.IMAGES["addHospital"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["removeHospital"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["viewHospitals"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    )
    private val validatorButtons = arrayOf(Button("Add Validator"), Button("Vote Validator"), Button("View Validators"))
    private val validatorImages = arrayOf(
        Image(Config.IMAGES["addValidator"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["voteValidator"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true),
        Image(Config.IMAGES["viewValidators"], Config.IMAGE_WIDTH, Config.IMAGE_HEIGHT, true, true)
    )

    init {
        connectComponents()
        styleComponents()
        setCallbacks()
    }

    private fun connectComponents() {
        val navBar = VBox(20.0)
        navBar.alignment = Pos.CENTER
        val tabs = arrayOf(
            Tab("User Tool", userContainer),
            Tab("Hospital Tool", hospitalContainer),
            Tab("Validator Tool", validatorContainer)
        )
        tabs.forEach { it.closableProperty().set(false) }
        val tabPane = TabPane(*tabs)
        userContainer.alignment = Pos.CENTER
        hospitalContainer.alignment = Pos.CENTER
        validatorContainer.alignment = Pos.CENTER
        for(i in 0 until userImages.size) {
            val tempBox = VBox(30.0, ImageView(userImages[i]), userButtons[i])
            tempBox.alignment = Pos.CENTER
            userContainer.children.add(tempBox)
        }
        for(i in 0 until hospitalImages.size) {
            val tempBox = VBox(30.0, ImageView(hospitalImages[i]), hospitalButtons[i])
            tempBox.alignment = Pos.CENTER
            hospitalContainer.children.add(tempBox)
        }
        for(i in 0 until validatorImages.size) {
            val tempBox = VBox(30.0, ImageView(validatorImages[i]), validatorButtons[i])
            tempBox.alignment = Pos.CENTER
            validatorContainer.children.add(tempBox)
        }
        this.center = tabPane
    }

    private fun styleComponents() {

    }

    private fun setCallbacks() {
        userCallbacks()
        hospitalCallbacks()
        validatorCallbacks()
    }

    private fun userCallbacks() {
        userButtons[0].setOnAction {
            //SCAN USER KEY -> INPUT USER IDENTITY -> GET HASH -> SIGN BY USER -> CREATE RECORD
            SceneManager.showScanScene(false)

        }
        userButtons[1].setOnAction {
            //SCAN USER KEY -> INPUT USER IDENTITY -> GET HASH -> SIGN BY USER -> CREATE RECORD
            SceneManager.showScanScene(false)

        }
        userButtons[2].setOnAction {
            //SCAN USER KEY -> SEARCH BLOCKCHAIN -> GET RECORD
            SceneManager.showScanScene(false)

        }
    }

    private fun hospitalCallbacks() {
        hospitalButtons[0].setOnAction {
            //GET HOSPITAL KEY

        }
        hospitalButtons[1].setOnAction {
            //DISQUALIFY HOSPITAL

        }
        hospitalButtons[2].setOnAction {
            //VIEW HOSPITALS

        }
    }

    private fun validatorCallbacks() {
        validatorButtons[0].setOnAction {
            //ADD VALIDATOR -> Get New Validator username, password,
        }
        validatorButtons[1].setOnAction {
            //VOTE VALIDATOR -> Show
        }
        validatorButtons[2].setOnAction {
            //VIEW VALIDATORS

        }
    }
}
