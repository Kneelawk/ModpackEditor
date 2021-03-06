package com.kneelawk.modpackeditor.ui

import com.kneelawk.modpackeditor.curse.ModpackFile
import com.kneelawk.modpackeditor.ui.dependency.DependencyCollectorFragment
import com.kneelawk.modpackeditor.ui.update.ModpackUpdateView
import com.kneelawk.modpackeditor.ui.util.ErrorOpeningModpackDialog
import com.kneelawk.modpackeditor.ui.util.ModListState
import com.kneelawk.modpackeditor.ui.util.ProgressDialog
import javafx.beans.property.SimpleBooleanProperty
import javafx.stage.FileChooser
import javafx.stage.Modality
import tornadofx.*
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KProperty1

/**
 * The controller for the main modpack editor view.
 */
class ModpackEditorMainController : Controller() {
    val model: ModpackModel by inject()
    val modListState: ModListState by inject()

    val running = SimpleBooleanProperty(false)
    val modpackTitle = model.modpackName.stringBinding(model.modpackVersion) { "$it - ${model.modpackVersion.value}" }

    private var previousDir: File = Path.of(model.modpackLocation.value).toAbsolutePath().parent.toFile()

    fun newModpack() {
        val newScope = Scope()
        setInScope(ModpackModel(), newScope)
        find<CreateModpackView>(newScope).openWindow(escapeClosesWindow = false, owner = null)
    }

    fun saveModpack() {
        running.value = true
        runAsync {
            model.commit()
            runLater { running.value = false }
        }
    }

    fun saveModpackAs() {
        running.value = true
        chooseFile("Save Modpack As", arrayOf(FileChooser.ExtensionFilter("Curse Modpack Files", "*.zip")),
            previousDir, FileChooserMode.Save).firstOrNull()?.let {
            var path = it.absolutePath
            if (!path.endsWith(".zip")) {
                path += ".zip"
            }
            model.modpackLocation.value = path
            previousDir = it.parentFile

            runAsync {
                model.commit()
                runLater { running.value = false }
            }
        } ?: run {
            running.value = false
        }
    }

    fun openModpack() {
        chooseFile("Open Modpack", arrayOf(FileChooser.ExtensionFilter("Curse Modpack Files", "*.zip")),
            previousDir, FileChooserMode.Single).firstOrNull()?.let {
            val location = it.absolutePath
            previousDir = it.parentFile

            runAsync {
                try {
                    val newModpack = ModpackModel(ModpackFile(it.toPath()))

                    runLater {
                        val newScope = Scope()

                        newModpack.rawModpackLocation.value = location
                        newModpack.modpackLocation.value = location

                        setInScope(newModpack, newScope)
                        find<ModpackEditorMainView>(newScope).openWindow(escapeClosesWindow = false, owner = null)
                    }
                } catch (e: IOException) {
                    runLater {
                        find<ModpackEditorMainView>().openInternalWindow(
                            find<ErrorOpeningModpackDialog>(mapOf<KProperty1<ErrorOpeningModpackDialog, Any>, Any>(
                                ErrorOpeningModpackDialog::modpackName to it.name,
                                ErrorOpeningModpackDialog::callback to { running.value = false })))
                    }
                }
            }
        }
    }

    fun duplicateModpack() {
        running.value = true
        chooseFile("Duplicate Modpack Destination",
            arrayOf(FileChooser.ExtensionFilter("Curse Modpack Files", "*.zip")),
            previousDir, FileChooserMode.Save).firstOrNull()?.let {
            var path = it.absolutePath
            if (!path.endsWith(".zip")) {
                path += ".zip"
            }

            runAsync {
                val newModpack = ModpackModel(model.openModpack.clone(Paths.get(path)))

                runLater {
                    val newScope = Scope()

                    newModpack.rawModpackLocation.value = path
                    newModpack.modpackLocation.value = path

                    setInScope(newModpack, newScope)
                    find<ModpackEditorMainView>(newScope).openWindow(escapeClosesWindow = false, owner = null)

                    running.value = false
                }
            }
        } ?: run {
            running.value = false
        }
    }

    fun runModpackUpdater() {
        find<ModpackUpdateView>().openModal(modality = Modality.WINDOW_MODAL,
            owner = find<ModpackEditorMainView>().currentWindow)
    }

    fun sortMods() {
        val openProperty = SimpleBooleanProperty(true)
        val task = modListState.sortModpackModsTask()
        task.finally { openProperty.value = false }
        find<ModpackEditorMainView>().openInternalWindow(
            find<ProgressDialog>(
                ProgressDialog::titleString to "Sorting addons...",
                ProgressDialog::progressProperty to task.progressProperty(),
                ProgressDialog::statusProperty to task.messageProperty(),
                ProgressDialog::openProperty to openProperty,
                ProgressDialog::cancelCallback to {
                    task.cancel()
                }
            )
        )
    }

    fun scanModDependencies() {
        find<DependencyCollectorFragment>(
            DependencyCollectorFragment::roots to model.modpackMods
        ).openModal(modality = Modality.WINDOW_MODAL, owner = find<ModpackEditorMainView>().currentWindow)
    }
}
