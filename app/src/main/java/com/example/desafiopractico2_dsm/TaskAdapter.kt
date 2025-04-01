package com.example.desafiopractico2_dsm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*


class TaskAdapter(
    private val tasks: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onItemLongClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        val taskDescription: TextView = itemView.findViewById(R.id.taskDescription)
        val checkbox: CheckBox = itemView.findViewById(R.id.taskCheckbox)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.taskTitle.text = task.title
        holder.taskDescription.text = task.description
        val dateText = holder.itemView.findViewById<TextView>(R.id.taskDate)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date(task.createdAt))
        dateText.text = "Creado: $formattedDate"

        holder.checkbox.setOnCheckedChangeListener(null) // prevenir bugs de reciclado
        holder.checkbox.isChecked = task.isCompleted

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnCheckedChangeListener
            FirebaseDatabase.getInstance().getReference("tasks")
                .child(userId)
                .child(task.id)
                .child("isCompleted")
                .setValue(isChecked)
        }


        holder.itemView.setOnClickListener {
            onItemClick(task)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(task)
            true
        }

        val deleteButton = holder.itemView.findViewById<ImageButton>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            onDeleteClick(task)
        }


    }
    override fun getItemCount(): Int = tasks.size
}
