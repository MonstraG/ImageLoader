package app

import app.models.*
import app.utils.findClosest
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.min

class MainView : View() {
    private var srcImg: Image = Image("bahamas.png")
    private var parsedImg: WritableImage = WritableImage(srcImg.pixelReader, srcImg.width.toInt(), srcImg.height.toInt())
    private val cellMap = mutableListOf<MutableList<Cell>>()
    private val biomeCounts = mutableMapOf<Biome, Int>()
    private val regions = mutableListOf<Region>()
    private var prevRegionCache: RegionCache? = null

    private val selectedFile: StringProperty = SimpleStringProperty()
    private val zoomProperty: DoubleProperty = SimpleDoubleProperty(1.0)

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
                        //todo: MouseX MouseY select region
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
                            zoomProperty.value = zoomProperty.value * 1.1
                        } else if (it.deltaY < 0) {
                            zoomProperty.value = zoomProperty.value / 1.1
                        }
                        println("Current zoom level: ${zoomProperty.value}")
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
        zoomProperty.value = smallestScale
        resizeImg()
    }

    private fun resizeImg() {
        imageView.fitWidth = zoomProperty.value * imageView.image.width
        imageView.fitHeight = zoomProperty.value * imageView.image.height
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

        biomeCounts.forEach { println("${it.key.Name}: ${it.value}") }
    }

    private fun detectRegion(cell: Cell) {
        if (regions.isEmpty()) {
            addNewRegion(cell)
        }

        val w = cell.pos.w
        val h = cell.pos.h

        //cache
        if (prevRegionCache != null) {
            if (prevRegionCache!!.lastPos.near(cell.pos)) {
                print("lel") //todo
            }
        }

        //to the top
        if (h > 0 && cellMap[w][h - 1].biome == cell.biome) {
            regions.find { it.contains(cell) }?.cells?.add(cell)
        }
        //to the left
        if (h > 0 && cellMap[w][h - 1].biome == cell.biome) {
            regions.find { it.contains(cell) }?.cells?.add(cell)
        }
    }

    private fun addNewRegion(cell: Cell) {
        val region = Region()
        region.cells.add(cell)
        regions.add(region)
        return
    }
}

data class RegionCache(val region: Region, val lastPos: Pos)
