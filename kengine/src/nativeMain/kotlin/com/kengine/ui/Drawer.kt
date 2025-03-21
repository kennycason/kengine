package com.kengine.ui

import com.kengine.geometry.useGeometryContext
import com.kengine.graphics.Color
import com.kengine.hooks.state.State
import com.kengine.hooks.state.useState
import com.kengine.log.Logging

enum class DrawerDirection {
    DOWN, UP, LEFT, RIGHT
}

enum class DrawerTrigger {
    CLICK, HOVER
}

/**
 * A drawer/dropdown component that can display a list of views in a dropdown
 * or drawer panel that appears in a specified direction.
 */
class Drawer(
    id: String,
    x: Double = 0.0,
    y: Double = 0.0,
    w: Double,
    h: Double,
    private val drawerDirection: DrawerDirection = DrawerDirection.DOWN,
    private val trigger: DrawerTrigger = DrawerTrigger.CLICK,
    bgColor: Color? = null,
    drawerHoverColor: Color? = null,
    private val activeColor: Color? = null,
    padding: Double = 5.0,
    spacing: Double = 2.0,
    parent: View? = null,
    onClick: (() -> Unit)? = null,
    onHover: (() -> Unit)? = null,
    private val isOpen: State<Boolean> = useState(false)
) : View(
    id = id,
    desiredX = x,
    desiredY = y,
    desiredW = w,
    desiredH = h,
    bgColor = bgColor,
    padding = padding,
    spacing = spacing,
    onClick = onClick,
    onHover = onHover,
    parent = parent,
    hoverColor = drawerHoverColor
), Logging {

    // Container for the drawer content (dropdown items)
    private val contentContainer = View(
        id = "$id-content",
        desiredX = 0.0,
        desiredY = 0.0,
        desiredW = 0.0, // Will be set during layout
        desiredH = 0.0, // Will be set during layout
        direction = if (drawerDirection == DrawerDirection.LEFT || drawerDirection == DrawerDirection.RIGHT)
                      FlexDirection.COLUMN else FlexDirection.ROW,
        padding = padding,
        spacing = spacing,
        visible = false // Initially hidden
    )

    // The header/trigger view that opens the drawer
    private val headerView = View(
        id = "$id-header",
        desiredX = 0.0,
        desiredY = 0.0,
        desiredW = w,
        desiredH = h,
        bgColor = bgColor,
        padding = padding
    )

    private var isHovered = false
    private var dropdownWidth = 0.0
    private var dropdownHeight = 0.0

    init {
        // Add the content container as a child of this view
        super.addChild(contentContainer)

        // Add the header view as a child of this view
        super.addChild(headerView)

        // Subscribe to the isOpen state to update visibility
        isOpen.subscribe { open ->
            contentContainer.visible = open
            performLayout(layoutX, layoutY) // Re-layout when state changes
        }
    }

    /**
     * Add child views to the header part of the drawer (the visible trigger)
     */
    fun addToHeader(view: View) {
        headerView.addChild(view)
    }

    /**
     * Add a child view to the drawer content (the dropdown/drawer area)
     */
    override fun addChild(view: View) {
        contentContainer.addChild(view)
        // Recalculate dropdown dimensions
        updateDropdownDimensions()
    }

    /**
     * Update the dropdown dimensions based on content children
     */
    private fun updateDropdownDimensions() {
        // Calculate content dimensions based on children
        if (contentContainer.children.isEmpty()) {
            dropdownWidth = 0.0
            dropdownHeight = 0.0
            return
        }

        val childrenMaxWidth = contentContainer.children.maxOf { it.desiredW }
        val childrenTotalHeight = contentContainer.children.sumOf { it.desiredH } +
                                 (contentContainer.spacing * (contentContainer.children.size - 1)) +
                                 (contentContainer.padding * 2)

        dropdownWidth = when (drawerDirection) {
            DrawerDirection.DOWN, DrawerDirection.UP ->
                layoutW.coerceAtLeast(childrenMaxWidth + contentContainer.padding * 2)
            DrawerDirection.LEFT, DrawerDirection.RIGHT ->
                childrenMaxWidth + contentContainer.padding * 2
        }

        dropdownHeight = when (drawerDirection) {
            DrawerDirection.DOWN, DrawerDirection.UP ->
                childrenTotalHeight
            DrawerDirection.LEFT, DrawerDirection.RIGHT ->
                layoutH.coerceAtLeast(childrenTotalHeight)
        }
    }

    /**
     * Custom layout for the drawer to position the content container
     * based on the direction.
     */
    override fun performLayout(offsetX: Double, offsetY: Double) {
        // Layout the main container (this view)
        super.performLayout(offsetX, offsetY)

        // Layout the header view
        headerView.desiredW = layoutW
        headerView.desiredH = layoutH
        headerView.performLayout(layoutX, layoutY)

        // Update dimensions before laying out content
        updateDropdownDimensions()

        // Set content container size
        contentContainer.desiredW = dropdownWidth
        contentContainer.desiredH = dropdownHeight

        // Position content container based on direction
        val contentX = when (drawerDirection) {
            DrawerDirection.DOWN, DrawerDirection.UP -> layoutX
            DrawerDirection.RIGHT -> layoutX + layoutW
            DrawerDirection.LEFT -> layoutX - dropdownWidth
        }

        val contentY = when (drawerDirection) {
            DrawerDirection.DOWN -> layoutY + layoutH
            DrawerDirection.UP -> layoutY - dropdownHeight
            DrawerDirection.LEFT, DrawerDirection.RIGHT -> layoutY
        }

        // Layout content container
        contentContainer.performLayout(contentX, contentY)
    }

    override fun draw() {
        // Draw the main view (header background)
        val currentBgColor = when {
            isOpen.get() && activeColor != null -> activeColor
            isHovered && hoverColor != null -> hoverColor
            else -> bgColor
        }

        // Draw header background
        if (currentBgColor != null) {
            useGeometryContext {
                fillRectangle(
                    headerView.layoutX,
                    headerView.layoutY,
                    headerView.layoutW,
                    headerView.layoutH,
                    currentBgColor
                )
            }
        }

        // Draw header children
        headerView.children.forEach { it.draw() }

        // Draw content container if open
        if (isOpen.get()) {
            contentContainer.draw()
        }
    }

    override fun hover(mouseX: Double, mouseY: Double) {
        if (!visible) return

        val wasHovered = isHovered
        isHovered = headerView.isWithinBounds(mouseX, mouseY)

        // Handle hover state change
        if (isHovered != wasHovered) {
            onHover?.invoke()

            // Auto-open on hover if trigger is HOVER
            if (isHovered && trigger == DrawerTrigger.HOVER) {
                isOpen.set(true)
            } else if (!isHovered && trigger == DrawerTrigger.HOVER) {
                // For hover trigger, also check if mouse is over the content
                val overContent = contentContainer.isWithinBounds(mouseX, mouseY)
                if (!overContent) {
                    isOpen.set(false)
                }
            }
        }

        // Pass hover to header children
        headerView.hover(mouseX, mouseY)

        // Pass hover to content children if open
        if (isOpen.get()) {
            contentContainer.hover(mouseX, mouseY)
        }
    }

    override fun click(mouseX: Double, mouseY: Double) {
        if (!visible) return

        // Check if clicking on the header
        if (headerView.isWithinBounds(mouseX, mouseY)) {
            onClick?.invoke()

            // Toggle open state if trigger is CLICK
            if (trigger == DrawerTrigger.CLICK) {
                isOpen.set(!isOpen.get())
            }

            // Also pass click to header children
            headerView.click(mouseX, mouseY)
        } else if (isOpen.get()) {
            // Pass click to content if open
            contentContainer.click(mouseX, mouseY)

            // If clicking outside both header and content, close the drawer
            if (!contentContainer.isWithinBounds(mouseX, mouseY) &&
                !headerView.isWithinBounds(mouseX, mouseY)) {
                isOpen.set(false)
            }
        }
    }

    override fun release(mouseX: Double, mouseY: Double) {
        if (!visible) return

        // Pass release to header
        headerView.release(mouseX, mouseY)

        // Pass release to content if open
        if (isOpen.get()) {
            contentContainer.release(mouseX, mouseY)
        }

        super.release(mouseX, mouseY)
    }

    /**
     * For the drawer, isWithinBounds should check both the header and content if open
     */
    override fun isWithinBounds(mouseX: Double, mouseY: Double): Boolean {
        // Check if mouse is over header (using null-safe access for safety)
        val overHeader = headerView?.isWithinBounds(mouseX, mouseY) ?: false

        // Check if mouse is over content and drawer is open (using null-safe access for safety)
        val isOpenState = isOpen.get()
        val overContent = isOpenState && (contentContainer?.isWithinBounds(mouseX, mouseY) ?: false)

        return overHeader || overContent
    }

    /**
     * Direct access to the open state
     */
    fun isOpen(): Boolean = isOpen.get()

    /**
     * Open or close the drawer
     */
    fun setOpen(open: Boolean) {
        isOpen.set(open)
    }

    /**
     * Toggle the drawer's open state
     */
    fun toggle() {
        isOpen.set(!isOpen.get())
    }
}

// Extension function removed since View.kt already has a drawer function
