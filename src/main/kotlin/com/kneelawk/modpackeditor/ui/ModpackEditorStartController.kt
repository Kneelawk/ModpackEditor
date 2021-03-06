package com.kneelawk.modpackeditor.ui

import com.kneelawk.modpackeditor.curse.ModpackFile
import com.kneelawk.modpackeditor.ui.util.ErrorOpeningModpackDialog
import javafx.beans.property.SimpleBooleanProperty
import javafx.stage.FileChooser
import tornadofx.Controller
import tornadofx.FileChooserMode
import tornadofx.chooseFile
import tornadofx.runLater
import java.io.File
import java.io.IOException
import kotlin.reflect.KProperty1

/**
 * Created by Kneelawk on 4/8/20.
 */
class ModpackEditorStartController : Controller() {
    private var previousDir = File(System.getProperty("user.home"))

    val running = SimpleBooleanProperty(false)

    fun openModpack() {
        running.value = true
        chooseFile("Open Modpack", arrayOf(FileChooser.ExtensionFilter("Curse Modpack Files", "*.zip")),
            previousDir, FileChooserMode.Single).firstOrNull()?.let {
            val location = it.absolutePath
            previousDir = it.parentFile

            runAsync {
                try {
                    val modpack = ModpackModel(ModpackFile(it.toPath()))

                    runLater {
                        modpack.rawModpackLocation.value = location
                        modpack.modpackLocation.value = location
                        setInScope(modpack)
                        find<ModpackEditorStartView>().replaceWith(find<ModpackEditorMainView>())
                        running.value = false
                    }
                } catch (e: IOException) {
                    runLater {
                        find<ModpackEditorStartView>().openInternalWindow(
                            find<ErrorOpeningModpackDialog>(mapOf<KProperty1<ErrorOpeningModpackDialog, Any>, Any>(
                                ErrorOpeningModpackDialog::modpackName to it.name,
                                ErrorOpeningModpackDialog::callback to { running.value = false })))
                    }
                }
            }
        } ?: run {
            running.value = false
        }
    }
}