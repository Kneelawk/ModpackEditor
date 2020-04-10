package com.kneelawk.modpackeditor.ui

import com.kneelawk.modpackeditor.data.AddonIdWrapper
import com.kneelawk.modpackeditor.data.manifest.FileJson
import com.kneelawk.modpackeditor.ui.mods.ModDetailsFragment
import com.kneelawk.modpackeditor.ui.util.*
import javafx.beans.binding.BooleanBinding
import javafx.beans.property.SimpleSetProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.stage.Modality
import tornadofx.Controller
import tornadofx.objectBinding
import kotlin.reflect.KProperty1

/**
 * Controller for the modpack mod list view.
 */
class ModpackModListController : Controller() {
    private val elementUtils: ElementUtils by inject()
    val model: ModpackModel by inject()

    private val editingMods = SimpleSetProperty<AddonIdWrapper>(FXCollections.observableSet())

    init {
        subscribe<ModRemoveEvent> {
            removeMod(it.addonId)
        }
        subscribe<ModRequiredEvent> {
            changeModRequired(it.addonId, it.required)
        }
        subscribe<ModDetailsEvent> {
            showModDetails(it.addonId)
        }
    }

    fun startEditing(addonId: FileJson) {
        editingMods.add(AddonIdWrapper(addonId))
    }

    fun finishEditing(addonId: FileJson) {
        editingMods.remove(AddonIdWrapper(addonId))
    }

    fun notEditingProperty(property: ObservableValue<FileJson>): BooleanBinding {
        return editingMods.containsProperty(property.objectBinding { it?.let { AddonIdWrapper(it) } }).not()
    }

    private fun removeMod(addonId: FileJson) {
        find<ModpackEditorMainView>().openInternalWindow(
            find<AreYouSureDialog>(mapOf<KProperty1<AreYouSureDialog, Any>, Any>(
                AreYouSureDialog::prompt to "Are you sure you would like to remove ${elementUtils.loadModName(
                    addonId)}?",
                AreYouSureDialog::callback to { res: AreYouSureDialog.Result ->
                    if (res == AreYouSureDialog.Result.Confirm) {
                        model.modpackMods.remove(addonId)
                    }
                },
                AreYouSureDialog::closeCallback to {
                    finishEditing(addonId)
                }
            ))
        )
    }

    private fun changeModRequired(addonId: FileJson, required: Boolean) {
        model.modpackMods.replaceAll {
            if (it == addonId) {
                FileJson(addonId.projectId, addonId.fileId, required)
            } else {
                it
            }
        }
        finishEditing(addonId)
    }

    private fun showModDetails(addonId: FileJson) {
        find<ModDetailsFragment>(mapOf<KProperty1<ModDetailsFragment, Any>, Any>(
            ModDetailsFragment::projectId to addonId.projectId,
            ModDetailsFragment::closeCallback to {
                finishEditing(addonId)
            }
        )).openModal(modality = Modality.NONE)
    }
}
