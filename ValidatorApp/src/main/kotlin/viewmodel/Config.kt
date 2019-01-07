package viewmodel

object Config {
    const val WIDTH = 1600
    const val HEIGHT = 900

    const val QRCODE_HEIGHT = 300
    const val QRCODE_WIDTH = 300

    const val IMAGE_HEIGHT = 400.0
    const val IMAGE_WIDTH = 400.0

    const val USERNAME_WARNING = "Please enter your username"
    const val PASSWORD_WARNING = "Please enter your password"
    const val WRONG_WARNING = "You entered wrong Username or Password"

    val CSS_STYLES = Config::class.java.classLoader.getResource("css/styles.css").toExternalForm()!!

    val IMAGES = mapOf(
        "createRecord" to Config::class.java.classLoader.getResource("images/menu/createRecord.png").toExternalForm(),
        "modifyRecord" to Config::class.java.classLoader.getResource("images/menu/modifyRecord.png").toExternalForm(),
        "viewRecord" to Config::class.java.classLoader.getResource("images/menu/viewRecord.png").toExternalForm(),
        "addHospital" to Config::class.java.classLoader.getResource("images/menu/addHospital.png").toExternalForm(),
        "removeHospital" to Config::class.java.classLoader.getResource("images/menu/removeHospital.png").toExternalForm(),
        "viewHospitals" to Config::class.java.classLoader.getResource("images/menu/viewHospitals.png").toExternalForm(),
        "addValidator" to Config::class.java.classLoader.getResource("images/menu/addValidator.png").toExternalForm(),
        "voteValidator" to Config::class.java.classLoader.getResource("images/menu/voteValidator.png").toExternalForm(),
        "viewValidators" to Config::class.java.classLoader.getResource("images/menu/viewValidators.png").toExternalForm()
    )

}