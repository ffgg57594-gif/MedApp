package com.medapp.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medapp.ai.core.ModelDescriptor
import com.medapp.ai.core.ModelTask
import com.medapp.databinding.ItemModelBinding

/**
 * Renders the list of available models generically from their descriptors.
 * Adding a new model to the registry automatically makes it show up here —
 * no changes needed in this file.
 */
class ModelListAdapter(
    private val models: List<ModelDescriptor>,
    private val onModelClick: (ModelDescriptor) -> Unit
) : RecyclerView.Adapter<ModelListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemModelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        holder.binding.tvModelName.text = model.displayNameAr
        holder.binding.tvModelDescription.text = model.descriptionAr
        holder.binding.tvModelBadge.text = taskLabel(model.task)
        holder.binding.tvModelSize.text = "${model.fileSizeMb} MB"
        holder.binding.root.setOnClickListener { onModelClick(model) }
    }

    override fun getItemCount(): Int = models.size

    private fun taskLabel(task: ModelTask): String = when (task) {
        ModelTask.CLASSIFICATION -> "تصنيف"
        ModelTask.SEGMENTATION -> "تجزئة"
        ModelTask.ANOMALY_DETECTION -> "كشف شذوذ"
    }
}
