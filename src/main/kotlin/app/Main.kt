package app

import app.models.Biome
import app.models.Cell
import app.models.Pos
import app.models.biomes
import app.utils.colorFromRGB
import app.utils.findClosest
import app.utils.sq
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ChangeListener
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.TransferMode
import javafx.scene.paint.Color
import javafx.scene.text.Font
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

    private val imageZoom: DoubleProperty = SimpleDoubleProperty(1.0)
    private var userZoom: Double = 1.0

    private val regionTooltip: Tooltip = Tooltip().apply {
        font = Font(12.0)
    }
    private var tooltipPos = Pair(0.0, 0.0)

    private var imageView = ImageView()
    private var stackPane = stackpane()

    private var selectedRegionBorders = setOf<Pos>()
    private var selectedRegionCells = setOf<Pos>()

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
                        var currentRegionTask: FXTask<*>? = null
                        setOnMouseClicked {
                            currentRegionTask?.cancel()
                            deselectRegion()
                            val x = (it.x / imageZoom.value).toInt()
                            val y = (it.y / imageZoom.value).toInt()
                            val tooltipAnchorX = it.screenX + 25
                            val tooltipAnchorY = it.screenY
                            regionTooltip.text = "(${x}, ${y}) - ${cellMap[x][y].biome.Name}, loading region..."
                            regionTooltip.show(this, tooltipAnchorX, tooltipAnchorY)
                            runAsync {
                                currentRegionTask = this
                                calculateRegion(Pos(x, y))
                                if (!this.isCancelled) {
                                    selectRegion()
                                }
                            } ui {
                                regionTooltip.text = "($x, $y) - ${cellMap[x][y].biome.Name}, size: ${selectedRegionCells.size}"
                                regionTooltip.show(imageView, tooltipAnchorX, tooltipAnchorY)
                            }
                            tooltipPos = Pair(it.screenX, it.screenY)
                        }
                        setOnMouseMoved {
                            if (regionTooltip.isShowing &&
                                sqrt(sq(tooltipPos.first - it.screenX) + sq(tooltipPos.second - it.screenY)) > 40) {
                                regionTooltip.hide()

                                currentRegionTask?.cancel()
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
                            userZoom *= 1.1
                        } else if (it.deltaY < 0) {
                            userZoom /= 1.1
                        }
                        println("Current zoom level: $userZoom")
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

        val stageSizeListener: ChangeListener<Number> =
            ChangeListener<Number> { wat, new, old ->
                if (imageView.image != null) {
                    println("Resizing, wat: ${wat}, old: $old, new: $new")
                    detectZoomValue()
                    resizeImg()
                }
            }
        root.widthProperty().addListener(stageSizeListener)
        root.heightProperty().addListener(stageSizeListener)
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
        cellMap.clear()
        biomeCounts.clear()
        parsedImg = WritableImage(srcImg.pixelReader, srcImg.width.toInt(), srcImg.height.toInt())
        detectBiomes(parsedImg)
        imageView.image = parsedImg

        detectZoomValue()
        resizeImg()
    }

    private fun detectZoomValue() {
        val widthScale = stackPane.width / imageView.image.width
        val heightScale = stackPane.height / imageView.image.height
        val smallestScale = min(widthScale, heightScale)
        imageZoom.value = smallestScale
    }

    private fun resizeImg() {
        imageView.fitWidth = imageZoom.value * imageView.image.width * userZoom
        imageView.fitHeight = imageZoom.value * imageView.image.height * userZoom
        println("Setting ${imageView.fitWidth} = ${imageZoom.value} * ${imageView.image.width} * ${userZoom}\n${imageView.fitHeight} = ${imageZoom.value} * ${imageView.image.height} * ${userZoom}")
    }

    private fun detectBiomes(image: WritableImage) {
        val width = srcImg.width.toInt()
        val height = srcImg.height.toInt()

        for (w in 0 until width) {
            cellMap.add(mutableListOf())
            for (h in 0 until height) {
                val pixelColor: Color = image.pixelReader.getColor(w, h)
                val closestBiome: Biome = findClosest(pixelColor, biomes)
                val currentCell = Cell(Pos(w, h), closestBiome)
                biomeCounts[closestBiome] = biomeCounts.getOrPut(closestBiome) { 1 } + 1
                cellMap[w].add(currentCell)
                image.pixelWriter.setColor(w, h, closestBiome.Color)
            }
        }

        //todo: properly show this
        println("Biome data:")
        biomeCounts.forEach { println("${it.key.Name}: ${it.value}") }
    }

    private fun deselectRegion() {
        selectedRegionBorders.forEach {
            parsedImg.pixelWriter.setColor(it.w, it.h, cellMap[it.w][it.h].biome.Color)
        }
    }

    private fun calculateRegion(pos: Pos) {
        val regionCells = mutableSetOf(pos)
        val borderCells = mutableSetOf<Pos>()
        val startingBiome = cellMap[pos.w][pos.h].biome
        val unvisitedCellsStack = mutableListOf(pos)

        while (unvisitedCellsStack.isNotEmpty()) {
            val cur = unvisitedCellsStack.removeAt(0)
            val cell: Cell = cellMap[cur.w][cur.h]
            cell.pos.getNeighbors().forEach {
                if (!regionCells.contains(it) && it.w in 0 until cellMap.size && it.h in 0 until cellMap[it.w].size) {
                    if (cellMap[it.w][it.h].biome == startingBiome) {
                        regionCells.add(it)
                        unvisitedCellsStack.add(it)
                    } else {
                        borderCells.add(cur)
                    }
                }
            }
        }
        selectedRegionBorders = borderCells
        selectedRegionCells = regionCells
    }

    private fun selectRegion() {
        selectedRegionBorders.forEach {
            parsedImg.pixelWriter.setColor(it.w, it.h, colorFromRGB(0, 255, 0))
        }
    }
}
