package main

import javafx.application.Application
import javafx.stage.Screen
import javafx.stage.Stage
import viewmodel.Config
import viewmodel.SceneManager

class Main : Application() {
    override fun start(primaryStage: Stage) {
        Config.WIDTH = Screen.getScreens().first().visualBounds.width
        Config.HEIGHT = Screen.getScreens().first().visualBounds.height
        Config.IMAGE_WIDTH = Config.WIDTH * 0.25
        Config.IMAGE_HEIGHT = Config.HEIGHT * 0.25
        SceneManager.stage = primaryStage
        SceneManager.showLogInScene()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Main::class.java, *args)
        }
    }
}
