package com.kengine.ui

import com.kengine.Game
import com.kengine.GameRunner
import com.kengine.createGameContext
import com.kengine.font.getFontContext
import com.kengine.font.useFontContext
import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.useState
import com.kengine.input.mouse.useMouseContext
import com.kengine.log.Logger
import com.kengine.sdl.useSDLContext
import com.kengine.time.useTimer
import kotlin.test.Test

class DrawerIT {

    @Test
    fun `drawer component test`() {
        createGameContext(
            title = "Drawer Component Test",
            width = 800,
            height = 600,
            logLevel = Logger.Level.INFO
        ) {
            useFontContext {
                addFont("arcade_classic", "src/nativeTest/resources/assets/fonts/arcade_classic.ttf", fontSize = 13f)
            }

            val font = getFontContext().getFont("arcade_classic", fontSize = 13f)

            GameRunner(frameRate = 60) {

                // Create the view context and main menu views
                val rootView = useView(
                    id = "rootView",
                    x = 0.0,
                    y = 0.0,
                    w = 800.0,
                    h = 600.0,
                    bgColor = Color.black
                )

                // Create selection state for menu items
                val selectedItem = useState<String?>(null)

                // Add dropdown menu - basic dropdown demonstrating DOWN direction
                val dropdown = rootView.drawer(
                    id = "mainMenu",
                    x = 50.0,
                    y = 50.0,
                    w = 120.0,
                    h = 30.0,
                    bgColor = Color.neonBlue,
                    hoverColor = Color.neonCyan,
                    activeColor = Color.neonGreen,
                    trigger = DrawerTrigger.CLICK,
                    direction = DrawerDirection.DOWN,
                    padding = 4.0,
                    spacing = 2.0
                )

                // Add text to the dropdown header
                dropdown.addToHeader(
                    TextView(
                        id = "menuLabel",
                        x = 5.0,
                        y = 5.0,
                        w = 110.0,
                        h = 20.0,
                        text = "Main Menu",
                        font = font,
                        textColor = Color.white,
                        align = Align.CENTER
                    )
                )

                // Add menu items to the dropdown content
                val menuItems = listOf("File", "Edit", "View", "Help")

                menuItems.forEachIndexed { index, item ->
                    dropdown.view(
                        id = "$item-item",
                        w = 120.0,
                        h = 30.0,
                        bgColor = Color.neonBlue,
                        hoverColor = Color.neonCyan,
                        onClick = {
                            selectedItem.set(item)
                            logger.info { "Selected menu item: $item" }
                            dropdown.setOpen(false)
                        }
                    ) {
                        text(
                            id = "$item-text",
                            x = 5.0,
                            y = 5.0,
                            w = 110.0,
                            h = 20.0,
                            text = item,
                            font = font,
                            textColor = Color.white,
                            align = Align.LEFT
                        )
                    }
                }

                // Add nested dropdown for File menu to demonstrate nesting
                val fileSubmenu = rootView.drawer(
                    id = "fileMenu",
                    x = 200.0,
                    y = 50.0,
                    w = 120.0,
                    h = 30.0,
                    bgColor = Color.neonBlue,
                    hoverColor = Color.neonCyan,
                    activeColor = Color.neonGreen,
                    trigger = DrawerTrigger.HOVER, // Use hover for this one
                    direction = DrawerDirection.RIGHT,
                    padding = 4.0,
                    spacing = 2.0
                )

                // Add header text
                fileSubmenu.addToHeader(
                    TextView(
                        id = "fileMenuLabel",
                        x = 5.0,
                        y = 5.0,
                        w = 110.0,
                        h = 20.0,
                        text = "File →",
                        font = font,
                        textColor = Color.white,
                        align = Align.LEFT
                    )
                )

                // Add submenu items
                val fileOptions = listOf("New", "Open", "Save", "Exit")

                fileOptions.forEachIndexed { index, option ->
                    fileSubmenu.view(
                        id = "$option-option",
                        w = 120.0,
                        h = 30.0,
                        bgColor = Color.neonPurple,
                        hoverColor = Color.neonPink,
                        onClick = {
                            selectedItem.set("File > $option")
                            logger.info { "Selected File option: $option" }
                            fileSubmenu.setOpen(false)
                        }
                    ) {
                        text(
                            id = "$option-text",
                            x = 5.0,
                            y = 5.0,
                            w = 110.0,
                            h = 20.0,
                            text = option,
                            font = font,
                            textColor = Color.white,
                            align = Align.LEFT
                        )
                    }
                }

                // Add "up" direction drawer example
                val upDrawer = rootView.drawer(
                    id = "upMenu",
                    x = 50.0,
                    y = 200.0,
                    w = 120.0,
                    h = 30.0,
                    bgColor = Color.neonOrange,
                    hoverColor = Color.neonYellow,
                    activeColor = Color.neonGreen,
                    trigger = DrawerTrigger.CLICK,
                    direction = DrawerDirection.UP,
                    padding = 4.0,
                    spacing = 2.0
                )

                // Add text to header
                upDrawer.addToHeader(
                    TextView(
                        id = "upMenuLabel",
                        x = 5.0,
                        y = 5.0,
                        w = 110.0,
                        h = 20.0,
                        text = "Up Menu",
                        font = font,
                        textColor = Color.black,
                        align = Align.CENTER
                    )
                )

                // Add items to up drawer
                listOf("Item 1", "Item 2", "Item 3").forEach { item ->
                    upDrawer.view(
                        id = "$item-up",
                        w = 120.0,
                        h = 30.0,
                        bgColor = Color.neonOrange,
                        hoverColor = Color.neonYellow,
                        onClick = {
                            selectedItem.set("UpMenu > $item")
                            logger.info { "Selected up menu item: $item" }
                            upDrawer.setOpen(false)
                        }
                    ) {
                        text(
                            id = "$item-text",
                            x = 5.0,
                            y = 5.0,
                            w = 110.0,
                            h = 20.0,
                            text = item,
                            font = font,
                            textColor = Color.black,
                            align = Align.LEFT
                        )
                    }
                }

                // Add "left" direction drawer example
                val leftDrawer = rootView.drawer(
                    id = "leftMenu",
                    x = 300.0,
                    y = 150.0,
                    w = 120.0,
                    h = 30.0,
                    bgColor = Color.neonGreen,
                    hoverColor = Color.neonCyan,
                    activeColor = Color.neonBlue,
                    trigger = DrawerTrigger.CLICK,
                    direction = DrawerDirection.LEFT,
                    padding = 4.0,
                    spacing = 2.0
                )

                // Add text to header
                leftDrawer.addToHeader(
                    TextView(
                        id = "leftMenuLabel",
                        x = 5.0,
                        y = 5.0,
                        w = 110.0,
                        h = 20.0,
                        text = "← Left Menu",
                        font = font,
                        textColor = Color.white,
                        align = Align.CENTER
                    )
                )

                // Add items to left drawer
                listOf("Option A", "Option B", "Option C").forEach { item ->
                    leftDrawer.view(
                        id = "$item-left",
                        w = 120.0,
                        h = 30.0,
                        bgColor = Color.neonGreen,
                        hoverColor = Color.neonCyan,
                        onClick = {
                            selectedItem.set("LeftMenu > $item")
                            logger.info { "Selected left menu item: $item" }
                            leftDrawer.setOpen(false)
                        }
                    ) {
                        text(
                            id = "$item-text",
                            x = 5.0,
                            y = 5.0,
                            w = 110.0,
                            h = 20.0,
                            text = item,
                            font = font,
                            textColor = Color.white,
                            align = Align.LEFT
                        )
                    }
                }

                // Perform initial layout
                rootView.performLayout()

                // Run the test for a few seconds
                object : Game {
                    override fun update() {
                        // Handle mouse input to interact with UI
                        useMouseContext {
                            rootView.hover(mouse.cursor().x, mouse.cursor().y)

                            if (mouse.isLeftPressed()) {
                                rootView.click(mouse.cursor().x, mouse.cursor().y)
                            }

                            if (!mouse.isLeftPressed() && mouse.timeSinceLeftPressed() < 50) {
                                rootView.release(mouse.cursor().x, mouse.cursor().y)
                            }
                        }

                        // Auto exit after a shorter time to prevent issues
                        useTimer(20000L) {
                            logger.info { "Timer completed, shutting down test" }
                            isRunning = false
                        }
                    }

                    override fun draw() {
                        useSDLContext {
                            // Clear the screen
                            fillScreen(0x22u, 0x22u, 0x22u)

                            // Draw all UI elements
                            rootView.draw()

                            // Draw the currently selected item at the bottom of the screen
                            if (selectedItem.get() != null) {
                                val selection = "Selected: ${selectedItem.get()}"
                                useGeometryContext {
                                    fillRectangle(0.0, 550.0, 800.0, 50.0, Color.gray60)
                                }
                                font.drawText(selection, 10, 560, Color.white)
                            }

                            // Swap buffers
                            flipScreen()
                        }
                    }

                    override fun cleanup() {
                        logger.info { "Starting test cleanup" }
                        rootView.cleanup()
                        // Don't explicitly cleanup the font as it might be handled by FontContext cleanup
                        logger.info { "Test cleanup completed successfully" }
                    }
                }
            }
        }
    }
}
