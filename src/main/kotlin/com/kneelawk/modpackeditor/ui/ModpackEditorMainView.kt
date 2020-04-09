package com.kneelawk.modpackeditor.ui

import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.scene.text.FontWeight
import tornadofx.*

/**
 * The main view for the modpack editor.
 */
class ModpackEditorMainView : View("Modpack Editor") {
    private val c: ModpackEditorMainController by inject()

    override val root = vbox {
        padding = insets(0.0)
        menubar {
            menu("File") {
                item("Save", "Shortcut+S") {
                    action {
                        c.saveModpack()
                    }
                }
                item("Save As...", "Shortcut+Shift+S") {
                    action {
                        c.saveModpackAs()
                    }
                }
                item("Open...", "Shortcut+O") {
                    action {
                        c.openModpack()
                    }
                }
                item("Duplicate...") {
                    action {
                        c.duplicateModpack()
                    }
                }
            }
        }

        vbox {
            padding = insets(25.0)
            spacing = 10.0
            label(c.model.modpackName).style {
                fontSize = 36.px
                fontWeight = FontWeight.BOLD
            }

            tabpane {
                enableWhen(c.running.not())
                tab<ModpackDetailsView> {
                    closableProperty().unbind()
                    isClosable = false
                    textProperty().unbind()
                    text = "Details"
                }
            }
        }
    }

    init {
        titleProperty.bind(c.model.modpackName)
    }

    override fun onBeforeShow() {
        with(currentStage!!) {
            width = 1280.0
            height = 800.0
            minWidth = 500.0
            minHeight = 400.0
        }
    }
}