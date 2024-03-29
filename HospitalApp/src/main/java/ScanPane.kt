import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamResolution
import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import viewmodel.Config
import xyz.medirec.medirec.pojo.KeyTime
import xyz.medirec.medirec.pojo.SecretTime
import javax.crypto.SecretKey
import java.awt.image.BufferedImage
import java.lang.IllegalArgumentException
import javax.crypto.spec.SecretKeySpec

object ScanPane : BorderPane() {
    @JvmField
    val scanScene = Scene(this, Config.WIDTH, Config.HEIGHT)
    private val container = HBox(20.0)
    private val backToMenu = Button("Back to Menu")
    private var webcam: Webcam? = null
    private val imgWebCamCapturedImage = ImageView()
    var stopCamera = true
    private val imageProperty = SimpleObjectProperty<Image>()
    private val cameraOptions: ComboBox<WebCamInfo> = ComboBox()
    @JvmField
    var type = TYPE.INVALID
    var isViewOnly = false

    enum class TYPE {
        INVALID, PUBLIC_KEY, SECRET_KEY, SIGNATURE
    }

    var resultKeyTime : KeyTime? = null
    var resultSignature : ByteArray? = null
    var resultAES : SecretKey? = null
    var resultTimestamp : Long? = null
    var resultPatientInfo : ByteArray? = null

    private class WebCamInfo {
        internal var webCamName: String? = null
        internal var webCamIndex: Int = 0
        override fun toString(): String {
            return webCamName ?: ""
        }
    }

    init {
        createTopPanel()
        connectComponents()
        styleComponents()
        setCallbacks()
        Platform.runLater { this.setImageViewSize() }
    }

    @JvmStatic
    fun startWebCam() {
        val index = cameraOptions.selectionModel.selectedIndex
        if (index == -1)
            cameraOptions.selectionModel.selectFirst()
        else
            scan(index)
    }

    private fun setImageViewSize() {
        val height = height * 0.9
        val width = width * 0.9
        imgWebCamCapturedImage.fitHeight = height
        imgWebCamCapturedImage.fitWidth = width
        imgWebCamCapturedImage.prefHeight(width)
        imgWebCamCapturedImage.prefWidth(height)
        imgWebCamCapturedImage.isPreserveRatio = true
    }

    private fun connectComponents() {
        container.children.addAll(
            cameraOptions,
            backToMenu
        )
        container.alignment = Pos.CENTER
        container.padding = Insets(0.0, 0.0, 7.0, 0.0)
        this.center = imgWebCamCapturedImage
        this.bottom = container
    }

    private fun createTopPanel() {
        val options = FXCollections.observableArrayList<WebCamInfo>()

        for ((webCamCounter, webcam) in Webcam.getWebcams().withIndex()) {
            val webCamInfo = WebCamInfo()
            webCamInfo.webCamIndex = webCamCounter
            webCamInfo.webCamName = webcam.name
            options.add(webCamInfo)
        }

        cameraOptions.items = options
        cameraOptions.promptText = "Choose Camera"
        cameraOptions.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                scan(newValue.webCamIndex)
            }
        }
    }

    private fun styleComponents() {

    }

    private fun setCallbacks() {
        backToMenu.setOnAction {
            disposeWebCamCamera()
            SceneManager.showMainMenuScene()
        }
    }

    private fun scan(webCamIndex: Int) {
        val webCamThread = Thread {
            if (webcam != null)
                disposeWebCamCamera()
            stopCamera = false

            webcam = Webcam.getWebcams()[webCamIndex]
            webcam!!.setCustomViewSizes(WebcamResolution.HD.size)
            webcam!!.viewSize = WebcamResolution.HD.size
            webcam!!.open()

            var img: BufferedImage
            var decode: String?
            while (!stopCamera) {
                img = webcam!!.image
                if (img != null) {
//                    println(webcam!!.fps.toString() + ", width: " + img.width + ", height: " + img.height)
                    for (row in 0 until img.height) {
                        for (col in 0 until img.width / 2) {
                            val temp = img.getRGB(col, row)
                            img.setRGB(col, row, img.getRGB(img.width - col - 1, row))
                            img.setRGB(img.width - col - 1, row, temp)
                        }
                    }
                    imageProperty.set(SwingFXUtils.toFXImage(img, null))

                    decode = decodeQRCode(img)
                    if (decode != null) {
                        try {
                            //GET PUBLIC KEY SCAN
                            val keyTime = KTHelper.deserialize(decode) as KeyTime
                            if(type != TYPE.PUBLIC_KEY)
                                throw IllegalArgumentException("NOT PUBLIC KEY")
                            this.resultKeyTime = keyTime
                            Platform.runLater{
                                SceneManager.showPatientScene(keyTime)
                            }
                        } catch (e: Exception) {
                            try {
                                if(type != TYPE.SECRET_KEY)
                                    throw IllegalArgumentException("NOT SECRET KEY")
                                val secretTime = KTHelper.deserialize(decode) as SecretTime
                                this.resultAES = SecretKeySpec(secretTime.secretKeyEncoded, "AES")
                                this.resultTimestamp = secretTime.timestamp

                                //val mergedBytes = KTHelper.mergeByteArrays(KTHelper.longToBytes(secretTime.timestamp), resultPatientInfo!!)
                                Platform.runLater{
                                    //QRCodePane.drawQRCode(KTHelper.getHash(mergedBytes))
                                    SceneManager.showQRScene(resultAES, secretTime.timestamp)
                                }
                            } catch (e : Exception) {
                                try {
                                    //GET SIGNATURE + TIMESTAMP SCAN
                                    val signature = KTHelper.decodeFromString(decode)
                                    if (signature.size * 4 != 256 || type != TYPE.SIGNATURE)
                                        throw IllegalArgumentException("NOT SIGNATURE")
                                    //val publicKey = this.resultKeyTime!!.pubKeyEncoded
                                    this.resultSignature = signature
                                    SceneManager.getSign(signature,
                                            this.resultAES,
                                            resultTimestamp!!)
                                    Platform.runLater{
                                        //QRCodePane.drawQRCode(KTHelper.getHash(mergedBytes))
                                        SceneManager.showMainMenuScene()
                                    }
                                } catch (e: Exception) {
                                    //INVALID QR CODE
                                    e.printStackTrace()
                                    println("INVALID QR Code")
                                    continue
                                }
                            }
                        }
                        imageProperty.value = null
                        disposeWebCamCamera()
                        break
                    }
                    img.flush()
                }
            }
        }
        imgWebCamCapturedImage.imageProperty().bind(imageProperty)
        webCamThread.isDaemon = true
        webCamThread.start()
    }

    private fun decodeQRCode(qrCodeimage: BufferedImage): String? {
        val source = BufferedImageLuminanceSource(qrCodeimage)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = MultiFormatReader().decode(bitmap)
            result.text
        } catch (e: NotFoundException) {
            null
        }
    }

    fun disposeWebCamCamera() {
        if (webcam != null && webcam!!.isOpen) {
            stopCamera = true
            webcam!!.close()
        }
    }
}
