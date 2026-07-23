package com.medapp.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.medapp.ai.registry.ModelRegistry
import com.medapp.databinding.ActivityModelListBinding
import com.medapp.ui.components.ModelListAdapter

/**
 * Shows every model currently registered in [ModelRegistry]. This screen
 * requires zero changes when new models are added — it always reflects
 * whatever is listed in ModelRegistry.MODEL_IDS.
 */
class ModelListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val models = ModelRegistry.listAvailable(this)

        binding.recyclerModels.layoutManager = LinearLayoutManager(this)
        binding.recyclerModels.adapter = ModelListAdapter(models) { descriptor ->
            val intent = Intent(this, InferenceActivity::class.java).apply {
                putExtra(InferenceActivity.EXTRA_MODEL_ID, descriptor.id)
            }
            startActivity(intent)
        }

        binding.tvEmptyState.visibility =
            if (models.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
