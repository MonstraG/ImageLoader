package app.components

import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.Parent
import tornadofx.*

class Menu : View() {
    val selectedFile: StringProperty = SimpleStringProperty()

    override val root: Parent = menubar {
        menu("File") {
            togglegroup {
                radiomenuitem("Bahamas", this, null, null, "bahamas.png")
                radiomenuitem("Europe", this, null, null, "europe.png")
                radiomenuitem("Ice ice baby", this, null, null, "ice ice baby.png")
                radiomenuitem("Italy", this, null, null, "italy.png")
                radiomenuitem("Scandinavia", this, null, null, "scandinavia.png")
                radiomenuitem("World", this, null, null, "world.png")
                radiomenuitem("World4x", this, null, null, "world4x.png")
                bind(selectedFile)
            }
        }
    }
}