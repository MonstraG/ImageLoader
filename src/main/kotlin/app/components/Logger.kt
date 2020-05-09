package app.components

import javafx.scene.Parent
import javafx.scene.control.ScrollPane
import javafx.scene.text.Text
import tornadofx.View
import tornadofx.scrollpane
import tornadofx.text

class Logger : View() {

    private lateinit var logArea: Text
    private lateinit var logScrollPane: ScrollPane

    override val root: Parent = scrollpane {
        logScrollPane = this
        //pref required to constrain it, and show scrollbars when content overflows.
        prefWidth = 200.0
        prefHeight = 700.0

        text {
            logArea = this
        }
    }

    fun log(str: String) {
        println(str)
        val scrollToBottom = logScrollPane.vvalue == 1.0
        //todo: scroll to bottom on first log.
        logArea.text += str + "\n"
        if (scrollToBottom) {
            logScrollPane.vvalue = 1.0
        }
    }
}