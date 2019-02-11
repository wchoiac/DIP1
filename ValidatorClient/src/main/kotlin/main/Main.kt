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
