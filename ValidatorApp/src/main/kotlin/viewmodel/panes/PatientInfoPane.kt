package viewmodel.panes

import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

class PatientInfoPane : StackPane() {
    private val container = VBox(30.0)
    private val nameLabel = Label("Name")
    private val name = TextArea("")
    private val identificationLabel = Label("ID")
    private val identification = TextArea("Iden")
}