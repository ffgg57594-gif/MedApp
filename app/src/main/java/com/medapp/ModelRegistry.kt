package com.medapp.ai.registry

import android.content.Context
import com.medapp.ai.core.MedicalModel
import com.medapp.ai.core.ModelDescriptor
import com.medapp.ai.core.Modality
import com.medapp.ai.core.ModelTask
import com.medapp.ai.models.classification.OnnxClassificationModel
import org.json.JSONObject

/**
 * Central place that knows about every model available in the app.
 *
 * HOW TO ADD A NEW MODEL (this is the whole process):
 *   1. Drop your converted .onnx file + a config.json into
 *      app/src/main/assets/models/<your_model_id>/
 *   2. Add one line to [MODEL_IDS] below with that folder name.
 *   3. If the model needs task-specific pre/post-processing beyond what
 *      [OnnxClassificationModel] already does (e.g. segmentation output
 *      masks), implement [MedicalModel] once for that task type — see
 *      ai/models/ package — and add the branch in [instantiate].
 *
 * The UI never needs to change: it only ever talks to [MedicalModel] and
 * [ModelDescriptor].
 */
object ModelRegistry {

    /**
     * Every model the app ships with. Each string is a folder name under
     * assets/models/. Add a new line here when you add a new model folder.
     */
    private val MODEL_IDS = listOf(
        "densenet121_chest_xray"
        // "unet_ct_brain_tumor"        // example: next model to add
        // "unet_mri_brats"             // example
        // "densenet_bone_xray"         // example
        // "resnet_pathology_camelyon"  // example
    )

    private val descriptorCache = mutableMapOf<String, ModelDescriptor>()

    /** List all available model descriptors (for the model-picker screen). */
    fun listAvailable(context: Context): List<ModelDescriptor> {
        return MODEL_IDS.mapNotNull { id ->
            runCatching { loadDescriptor(context, id) }.getOrNull()
        }
    }

    /** Read + cache a model's config.json as a [ModelDescriptor]. */
    fun loadDescriptor(context: Context, modelId: String): ModelDescriptor {
        descriptorCache[modelId]?.let { return it }

        val configPath = "models/$modelId/config.json"
        val json = context.assets.open(configPath).bufferedReader().use { it.readText() }
        val obj = JSONObject(json)

        val labels = mutableListOf<String>()
        obj.optJSONArray("labels")?.let { arr ->
            for (i in 0 until arr.length()) labels.add(arr.getString(i))
        }

        val descriptor = ModelDescriptor(
            id = obj.getString("id"),
            displayNameAr = obj.getString("display_name_ar"),
            displayNameEn = obj.getString("display_name_en"),
            modality = Modality.valueOf(obj.getString("modality").uppercase()),
            task = ModelTask.valueOf(obj.getString("task").uppercase()),
            descriptionAr = obj.optString("description_ar", ""),
            inputWidth = obj.getInt("input_width"),
            inputHeight = obj.getInt("input_height"),
            fileSizeMb = obj.optInt("file_size_mb", 0),
            labels = labels,
            modelFileName = obj.optString("model_file_name", "model.onnx")
        )

        descriptorCache[modelId] = descriptor
        return descriptor
    }

    /**
     * Create a ready-to-load [MedicalModel] instance for the given id.
     * Dispatches on [ModelTask] so each task family gets the right
     * pre/post-processing implementation.
     */
    fun instantiate(context: Context, modelId: String): MedicalModel {
        val descriptor = loadDescriptor(context, modelId)
        return when (descriptor.task) {
            ModelTask.CLASSIFICATION -> OnnxClassificationModel(descriptor)

            // When you add segmentation/anomaly-detection models, implement
            // MedicalModel for them (see ai/models/) and dispatch here, e.g.:
            // ModelTask.SEGMENTATION -> OnnxSegmentationModel(descriptor)
            // ModelTask.ANOMALY_DETECTION -> OnnxAnomalyModel(descriptor)

            else -> throw UnsupportedOperationException(
                "No implementation registered yet for task ${descriptor.task}. " +
                "Add one under ai/models/ and wire it up in ModelRegistry.instantiate()."
            )
        }
    }
}
