package app

import app.models.*
import app.utils.findClosest
import app.utils.sq
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.util.Duration
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.min
import kotlin.math.sqrt

class MainView : View() {
    private var srcImg: Image = Image("bahamas.png")
    private var parsedImg: WritableImage =
        WritableImage(srcImg.pixelReader, srcImg.width.toInt(), srcImg.height.toInt())
    private val cellMap = mutableListOf<MutableList<Cell>>()
    private val biomeCounts = mutableMapOf<Biome, Int>()

    private val selectedFile: StringProperty = SimpleStringProperty()
    private val zoom: DoubleProperty = SimpleDoubleProperty(1.0)

    private val regionTooltip: Tooltip = Tooltip().apply {
        font = Font(12.0)
        hideDelay = Duration.seconds(0.5)
    }
    private var tooltipPos = Pair(0.0, 0.0)

    private var imageView = ImageView()
    private var stackPane = stackpane()

    override val root = borderpane {
        center {
            vbox {
                menubar {
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
                stackpane {
                    stackPane = this
                    imageview {
                        imageView = this
                        setOnMouseClicked {
                            val x = (it.x / zoom.value).toInt()
                            val y = (it.y / zoom.value).toInt()
                            regionTooltip.text = "($x, $y) - ${cellMap[x][y].biome.Name}"
                            regionTooltip.show(this, it.screenX + 25, it.screenY)
                            tooltipPos = Pair(it.screenX, it.screenY)
                        }
                        setOnMouseMoved {
                            if (regionTooltip.isShowing &&
                                sqrt(sq(tooltipPos.first - it.screenX) + sq(tooltipPos.second - it.screenY)) > 40) {
                                regionTooltip.hide()
                            }
                        }
                    }
                    stackpaneConstraints {
                        prefWidth = 1024.0
                        prefHeight = 800.0
                    }
                    setOnDragOver {
                        if (it.gestureSource != this) {
                            it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                        }
                        it.consume()
                    }
                    setOnDragDropped {
                        println("Drag dropped event detected")
                        val file = it.dragboard.files[0]
                        if (it.dragboard.hasFiles()) {
                            val path = file.absolutePath
                            if (path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".jpg")) {
                                loadImg(file)
                                it.isDropCompleted = true
                                println("Image loaded")
                                return@setOnDragDropped
                            }
                            println("File found, but it is not an image, path: ${file.absolutePath}")
                        } else {
                            println("No files found in drop event")
                        }
                    }
                    setOnScroll {
                        if (it.deltaY > 0) {
                            zoom.value = zoom.value * 1.1
                        } else if (it.deltaY < 0) {
                            zoom.value = zoom.value / 1.1
                        }
                        println("Current zoom level: ${zoom.value}")
                        resizeImg()
                    }
                }
            }
        }
    }

    init {
        selectedFile.onChange {
            loadImg(selectedFile.value)
        }
    }

    private fun loadImg(file: String) {
        if (file == "") {
            return
        }

        srcImg = Image(file)
        loadAndParse()
    }

    private fun loadImg(file: File) {
        selectedFile.value = "" //deselect radio
        srcImg = Image(FileInputStream(file))
        loadAndParse()
    }

    private fun loadAndParse() {
        parsedImg = WritableImage(srcImg.pixelReader, srcImg.width.toInt(), srcImg.height.toInt())
        detectBiomes(parsedImg)
        imageView.image = parsedImg

        val widthScale = stackPane.width / imageView.image.width
        val heightScale = stackPane.height / imageView.image.height
        val smallestScale = min(widthScale, heightScale)
        zoom.value = smallestScale
        resizeImg()
    }

    private fun resizeImg() {
        imageView.fitWidth = zoom.value * imageView.image.width
        imageView.fitHeight = zoom.value * imageView.image.height
    }

    private fun detectBiomes(image: WritableImage) {
        val pixelReader = image.pixelReader
        val pixelWriter = image.pixelWriter
        val width = srcImg.width.toInt()
        val height = srcImg.height.toInt()

        for (w in 0 until width) {
            cellMap.add(mutableListOf())
            for (h in 0 until height) {
                val pixelColor: Color = pixelReader.getColor(w, h)
                val closestBiome: Biome = findClosest(pixelColor, biomes)
                val currentCell = Cell(Pos(w, h), closestBiome)
                biomeCounts[closestBiome] = biomeCounts.getOrPut(closestBiome) { 1 } + 1
                cellMap[w].add(currentCell)
                pixelWriter.setColor(w, h, closestBiome.Color)
            }
        }

        println("Biome data:")
        biomeCounts.forEach { println("${it.key.Name}: ${it.value}") }
    }
}

data class RegionCache(val region: Region, val lastPos: Pos)
