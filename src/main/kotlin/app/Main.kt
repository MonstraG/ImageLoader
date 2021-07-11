package app

import app.components.Logger
import app.components.Menu
import app.models.*
import app.utils.findClosest
import app.utils.fmt
import app.utils.sq
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ChangeListener
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import tornadofx.*
import java.io.File
import java.io.FileInputStream
import kotlin.math.min
import kotlin.math.sqrt

class MainView : View() {
    private var srcImg: Image = Image("bahamas.png")
    private var parsedImg: WritableImage = WritableImage(srcImg.pixelReader, srcImg.width.toInt(), srcImg.height.toInt())
    private val cellMap = mutableListOf<MutableList<Cell>>()

    private var imageZoom = SimpleDoubleProperty(1.0)
    private var userZoom = SimpleDoubleProperty(1.0)

    private val regionTooltip: Tooltip = Tooltip().apply {
        font = Font(12.0)
    }
    private var tooltipPos = Pair(0.0, 0.0)

    private var imageView = ImageView()
    private lateinit var imageViewContainer: Pane

    private var selectedRegionBorders = setOf<Pos>()
    private var selectedRegionCellsCount = 0

    override val root = VBox()

    private val logger = Logger()
    private val menu = Menu()

    init {
        with(root) {
            prefWidth = 1000.0
            prefHeight = 700.0
            add(menu)
            hbox {
                add(logger)
                var currentRegionTask: FXTask<*>? = null

                stackpane {
                    imageViewContainer = this
                    prefWidth = 800.0
                    style {
                        backgroundColor = multi(Color.LIGHTGRAY)
                    }
                    add(imageView.apply {
                        //dragging
                        var mouseAnchorX = 0.0
                        var mouseAnchorY = 0.0
                        var initialTranslateX = 0.0
                        var initialTranslateY = 0.0
                        addEventFilter(MouseEvent.MOUSE_PRESSED) {
                            // remember initial mouse cursor coordinates
                            // and node position
                            mouseAnchorX = it.x
                            mouseAnchorY = it.y
                            initialTranslateX = this.translateX
                            initialTranslateY = this.translateY
                        }
                        addEventFilter(MouseEvent.MOUSE_DRAGGED) {
                            // shift node from its initial position by delta
                            // calculated from mouse cursor movement
                            this.translateX = initialTranslateX + it.x - mouseAnchorX
                            this.translateY = initialTranslateY + it.y - mouseAnchorY
                            initialTranslateX = this.translateX
                            initialTranslateY = this.translateY
                            //fixme:
                            // 1. image covers others things
                            // 2. image doens't properly resize when reloaded
                            // 3. image yeets away sometime
                        }

                        setOnMouseClicked {
                            if (it.button != MouseButton.PRIMARY) {
                                it.consume()
                                return@setOnMouseClicked
                            }

                            currentRegionTask?.cancel()
                            unhighlightSelectedRegion()
                            val x = (it.x / imageZoom.value / userZoom.value).toInt()
                            val y = (it.y / imageZoom.value / userZoom.value).toInt()
                            if (!inCellMap(Pos(x, y))) {
                                it.consume()
                                return@setOnMouseClicked
                            }

                            val tooltipAnchorX = it.screenX + 25
                            val tooltipAnchorY = it.screenY
                            regionTooltip.text = "(${x}, ${y}) - ${cellMap[x][y].biome.Name}, loading region..."
                            regionTooltip.show(this, tooltipAnchorX, tooltipAnchorY)
                            runAsync {
                                currentRegionTask = this
                                floodDetectRegion(Pos(x, y))
                                if (!this.isCancelled) {
                                    highlightSelectedRegion()
                                }
                            } ui {
                                regionTooltip.text = "($x, $y) - ${cellMap[x][y].biome.Name}, size: $selectedRegionCellsCount"
                                regionTooltip.show(imageView, tooltipAnchorX, tooltipAnchorY)
                            }
                            tooltipPos = Pair(it.screenX, it.screenY)
                            it.consume()
                        }
                        setOnMouseMoved {
                            val movedFar = sqrt(sq(tooltipPos.first - it.screenX) + sq(tooltipPos.second - it.screenY)) > 40
                            if (regionTooltip.isShowing && movedFar) {
                                regionTooltip.hide()
                                currentRegionTask?.cancel()
                            }
                        }
                    })
                    setOnDragOver {
                        logger.log("Drag over")
                        if (it.gestureSource != this) {
                            it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
                        }
                        it.consume()
                    }
                    setOnDragDropped {
                        logger.log("Drag dropped event detected")
                        val file = it.dragboard.files[0]
                        if (it.dragboard.hasFiles()) {
                            it.isDropCompleted = true

                            val path = file.absolutePath
                            val validEnding = path.endsWith(".png") || path.endsWith(".jpeg") || path.endsWith(".jpg")
                            if (validEnding) {
                                loadImg(file)
                            } else {
                                logger.log("File found, but it is not an image, path: ${file.absolutePath}")
                            }

                        } else {
                            logger.log("No files found in DragEvent ")
                        }
                    }
                    setOnScroll {
                        if (it.deltaY > 0) {
                            userZoom.value *= 1.1
                        } else if (it.deltaY < 0) {
                            userZoom.value /= 1.1
                        }
                        logger.log("Zoom level: ${(userZoom.value * 100).fmt(2)}%")

                        currentRegionTask?.cancel()
                    }
                }
            }
        }

        menu.selectedFile.onChange {
            loadImg(menu.selectedFile.value)
        }

        //detect appropriate zoom value when image or window  size changes
        val zoomDetector = ChangeListener<Any> { _, _, _ ->
            if (imageView.image != null) {
                logger.log("zoom")
                val widthScale = imageViewContainer.width / imageView.image.width
                val heightScale = imageViewContainer.height / imageView.image.height
                imageZoom.value = min(widthScale, heightScale)
            }
        }
        imageView.imageProperty().addListener(zoomDetector)
        root.widthProperty().addListener(zoomDetector)
        root.heightProperty().addListener(zoomDetector)

        val resetTranslation = ChangeListener<Any> { _, _, _ ->
            imageView.translateX = 0.0
            imageView.translateY = 0.0
        }

        imageView.imageProperty().addListener(resetTranslation)

        //rescale image when zoom values change, also hide tooltip
        val rescaler = ChangeListener<Number> { _, _, _ ->
            logger.log("rescaled")
            val scale = imageZoom.value * userZoom.value
            imageView.scaleX = scale
            imageView.scaleY = scale

            if (regionTooltip.isShowing) {
                regionTooltip.hide()
            }
            //todo: make so that image doesn't zoom in over everything else
        }
        imageZoom.addListener(rescaler)
        userZoom.addListener(rescaler)
    }

    private fun loadImg(file: String) {
        if (file == "") {
            return
        }

        srcImg = Image(file)
        loadAndParse()
    }

    private fun loadImg(file: File) {
        if (!file.exists()) {
            logger.log("Couldn't load image - file not found")
            return
        }

        menu.selectedFile.value = "" //deselect radio
        srcImg = Image(FileInputStream(file))
        loadAndParse()
    }

    private fun loadAndParse() {
        //clear stuff from last image
        cellMap.clear()
        selectedRegionBorders = setOf()
        selectedRegionCellsCount = 0

        val width = srcImg.width.toInt()
        val height = srcImg.height.toInt()
        parsedImg = WritableImage(srcImg.pixelReader, width, height)
        detectBiomes(parsedImg)
        imageView.image = parsedImg
        imageView.isSmooth = false
        logger.log("Image loaded, size: ${width}x${height}")
    }

    private fun detectBiomes(image: WritableImage) {
        val biomeCounts = mutableMapOf<Biome, Int>()
        val width = srcImg.width.toInt()
        val height = srcImg.height.toInt()

        for (w in 0 until width) {
            cellMap.add(mutableListOf())
            for (h in 0 until height) {
                val pixelColor: Color = image.pixelReader.getColor(w, h)
                val closestBiome: Biome = findClosest(pixelColor, biomes)
                biomeCounts[closestBiome] = biomeCounts.getOrPut(closestBiome) { 1 } + 1
                image.pixelWriter.setColor(w, h, closestBiome.Color)

                val currentCell = Cell(Pos(w, h), closestBiome)
                cellMap[w].add(currentCell)
            }
        }

        logger.log("Biome data:")
        biomeCounts.forEach { logger.log("${it.key.Name}: ${it.value}") }
    }

    private fun floodDetectRegion(pos: Pos) {
        val regionCells = mutableSetOf(pos)
        val borderCells = mutableSetOf<Pos>()
        val startingBiome = cellMap[pos.w][pos.h].biome
        val unvisitedCellsStack = mutableListOf(pos)

        while (unvisitedCellsStack.isNotEmpty()) {
            val cur = unvisitedCellsStack.removeAt(0)
            val cell: Cell = cellMap[cur.w][cur.h]
            val isUnvisited = { it: Pos -> !regionCells.contains(it) && inCellMap(it) }

            cell.pos.getNeighbors().forEach {
                if (isUnvisited(pos)) {
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
        selectedRegionCellsCount = regionCells.size
    }

    private fun inCellMap(pos: Pos): Boolean {
        return pos.w in 0 until cellMap.size && pos.h in 0 until cellMap[pos.w].size
    }

    private fun highlightSelectedRegion() {
        colorInCells(selectedRegionBorders, Color.GREEN)
    }

    private fun unhighlightSelectedRegion() {
        colorInCells(selectedRegionBorders, null)
    }

    /**
     * if [color] is provided, fills image [pixels] with it, otherwise uses colors from CellMap
     */
    private fun colorInCells(pixels: Set<Pos>, color: Color?) {
        pixels.forEach {
            if (color != null) {
                parsedImg.pixelWriter.setColor(it.w, it.h, color)
            } else {
                parsedImg.pixelWriter.setColor(it.w, it.h, cellMap[it.w][it.h].biome.Color)
            }
        }
    }
}
