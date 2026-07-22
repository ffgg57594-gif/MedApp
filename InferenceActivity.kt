package com.medapp.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medapp.ai.core.InferenceResult
import com.medapp.ai.core.MedicalModel
import com.medapp.ai.core.ModelTask
import com.medapp.ai.registry.ModelRegistry
import com.medapp.databinding.ActivityInferenceBinding
import kotlinx.coroutines.launch

/**
 * Generic inference screen: works for ANY model task type because it only
 * talks to the [MedicalModel] interface and branches on [InferenceResult]'s
 * sealed subtype to decide how to render the outcome.
 *
 * When you add a segmentation model, this screen does not need to change —
 * the `is InferenceResult.Segmentation` branch already handles rendering
 * a mask overlay.
 */
class InferenceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODEL_ID = "extra_model_id"
    }

    private lateinit var binding: ActivityInferenceBinding
    private var model: MedicalModel? = null
    private var selectedBitmap: Bitmap? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadAndShowImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val modelId = intent.getStringExtra(EXTRA_MODEL_ID)
            ?: error("InferenceActivity requires EXTRA_MODEL_ID")

        val descriptor = ModelRegistry.loadDescriptor(this, modelId)
        binding.tvTitle.text = descriptor.displayNameAr
        binding.tvDisclaimer.text = getString(com.medapp.R.string.medical_disclaimer)

        setLoading(true, "جاري تحميل النموذج...")
        lifecycleScope.launch {
            model = ModelRegistry.instantiate(this@InferenceActivity, modelId)
            model?.load(this@InferenceActivity)
            setLoading(false)
        }

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnAnalyze.setOnClickListener {
            runInference()
        }
    }

    private fun loadAndShowImage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        selectedBitmap = bitmap
        binding.ivPreview.setImageBitmap(bitmap)
        binding.btnAnalyze.isEnabled = true
        binding.tvResult.text = ""
    }

    private fun runInference() {
        val bitmap = selectedBitmap ?: return
        val activeModel = model ?: return

        setLoading(true, "جاري التحليل...")
        lifecycleScope.launch {
            val result = activeModel.runInference(bitmap)
            setLoading(false)
            renderResult(result)
        }
    }

    private fun renderResult(result: InferenceResult) {
        when (result) {
            is InferenceResult.Classification -> {
                val top = result.predictions.firstOrNull()
                val sb = StringBuilder()
                sb.append("النتائج (${result.inferenceTimeMs} ملي ثانية):\n\n")
                result.predictions.forEach { p ->
                    val pct = (p.confidence * 100).toInt()
                    sb.append("${p.label}: $pct%\n")
                }
                binding.tvResult.text = sb.toString()
                binding.tvTopResult.text = top?.let {
                    "الأرجح: ${it.label} (${(it.confidence * 100).toInt()}%)"
                } ?: ""
            }

            is InferenceResult.Segmentation -> {
                binding.ivPreview.setImageBitmap(result.maskBitmap)
                binding.tvResult.text =
                    "نسبة المنطقة المتأثرة: ${result.affectedAreaPercent}%\n" +
                    "الوقت: ${result.inferenceTimeMs} ملي ثانية"
            }

            is InferenceResult.Error -> {
                binding.tvResult.text = "⚠️ ${result.message}"
            }
        }
    }

    private fun setLoading(loading: Boolean, message: String = "") {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnAnalyze.isEnabled = !loading && selectedBitmap != null
        if (loading) binding.tvResult.text = message
    }

    override fun onDestroy() {
        super.onDestroy()
        model?.unload()
    }
}
