package viewmodel.panes

import blockchain.block.transaction.MedicalContent
import blockchain.block.transaction.MedicalRecord
import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamResolution
import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import javafx.application.Platform
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import main.Helper
import viewmodel.SceneManager
import xyz.medirec.medirec.PublicKeyProperties

import javax.crypto.SecretKey
import java.awt.image.BufferedImage
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PublicKey

class ScanPane : BorderPane() {

    private val container = HBox(20.0)
    private val backToMenu = Button("Back to Menu")
    private var webcam: Webcam? = null
    private val imgWebCamCapturedImage = ImageView()
    private var stopCamera = true
    private val imageProperty = SimpleObjectProperty<Image>()
    private val cameraOptions: ComboBox<WebCamInfo> = ComboBox()
    private var isSign = false

    private inner class WebCamInfo {
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

    fun setIsSign(isSign: Boolean) {
        this.isSign = isSign
    }

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
        cameraOptions.selectionModel.selectedItemProperty().addListener { list, oldValue, newValue ->
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
                    println(webcam!!.fps.toString() + ", width: " + img.width + ", height: " + img.height)

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
                        println(decode)
                        val publicKey = tryObtainPublicKey(decode)
                        if (publicKey != null) {
                            //scan pane -> scan public Key -> Find record from blockchain -> show encrypted aes key
                            println("PUBLIC KEY OBTAINED")
                            if (isSign) {
                                val medicalRecord = findMedicalRecord(publicKey)
                                if (medicalRecord != null) {
                                    SceneManager.qrCodePane.drawQRCode(
                                        Helper.encodeToString(medicalRecord.encryptedAESKey)
                                    )
                                    SceneManager.showQRScene()
                                } else {
                                    println("INVALID PUBLIC KEY")
                                }
                            } else {
                                val medicalContent = findMedicalContent(publicKey)
                                if (medicalContent != null) {
                                    try {
                                        val serialized = Helper.serialize(medicalContent)
                                        val hash = Helper.getHash(serialized)
                                        SceneManager.qrCodePane.drawQRCode(hash)
                                        SceneManager.showQRScene()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                        println("INVALID PUBLIC KEY")
                                    } catch (e: NoSuchAlgorithmException) {
                                        e.printStackTrace()
                                        println("INVALID PUBLIC KEY")
                                    } catch (e: NoSuchProviderException) {
                                        e.printStackTrace()
                                        println("INVALID PUBLIC KEY")
                                    }

                                } else {
                                    println("INVALID PUBLIC KEY")
                                }
                            }
                        } else {
                            if (isSign) {
                                println("IT IS NOT AN ID")
                                continue
                            }
                            //get encrypted aes key then make it into QR code -> scan by user -> return aes key
                            val aesKey = tryObtainAESKey(decode)
                            if (aesKey != null) {
                                println("AES KEY OBTAINED")
                                //scan pane -> scan aes key -> decode record using aes key -> save it into local database

                                SceneManager.showMainMenuScene()
                            } else {
                                if (Helper.decodeFromString(decode).size == 256) {
                                    println("SIGNATURE FOUND")

                                    //                                    MedicalRecord mediRec = new MedicalRecord(,)
                                    //generate hash of the record into QR code -> scan by user -> return signature
                                    //scan pane -> scan signature -> put signature into the record then create record

                                } else {
                                    println("INVALID QR CODE")
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

    private fun findMedicalRecord(publicKey: PublicKey): MedicalRecord? {

        return null
    }

    private fun findMedicalContent(publicKey: PublicKey): MedicalContent? {

        return null
    }

    private fun tryObtainPublicKey(str: String): PublicKey? {
        try {
            val prop = Helper.deserialize(str) as PublicKeyProperties
            return object : PublicKey {
                override fun getAlgorithm(): String {
                    return prop.algorithm
                }

                override fun getFormat(): String {
                    return prop.format
                }

                override fun getEncoded(): ByteArray {
                    return prop.encoded
                }
            }
        } catch (e: Exception) {
            return null
        }

    }

    private fun tryObtainAESKey(str: String): SecretKey? {
        return try {
            Helper.deserialize(str) as SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeQRCode(qrCodeimage: BufferedImage): String? {
        val source = BufferedImageLuminanceSource(qrCodeimage)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = MultiFormatReader().decode(bitmap)
            return result.text
        } catch (e: NotFoundException) {
            return null
        }

    }

    fun disposeWebCamCamera() {
        if (webcam != null && webcam!!.isOpen) {
            stopCamera = true
            webcam!!.close()
        }
    }
}
