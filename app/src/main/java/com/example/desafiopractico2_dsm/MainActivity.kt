package com.example.desafiopractico2_dsm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var fab: FloatingActionButton

    private val taskList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logoutButton = findViewById<Button>(R.id.logoutButton)

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        recyclerView = findViewById(R.id.todoRecyclerView)
        fab = findViewById(R.id.addTaskFab)


        taskAdapter = TaskAdapter(
            taskList,
            onItemClick = { task -> showEditDialog(task) },
            onItemLongClick = { task -> showDeleteDialog(task) },
            onDeleteClick = { task -> showDeleteDialog(task) } // ← también reutilizamos el diálogo
        )


        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        fab.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
            val titleEditText = dialogView.findViewById<EditText>(R.id.taskTitleEditText)
            val descriptionEditText = dialogView.findViewById<EditText>(R.id.taskDescriptionEditText)

            val dialog = AlertDialog.Builder(this)
                .setTitle("Nueva tarea")
                .setView(dialogView)
                .setPositiveButton("Guardar") { _, _ ->
                    val title = titleEditText.text.toString().trim()
                    val description = descriptionEditText.text.toString().trim()

                    if (title.isEmpty()) {
                        Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    addTaskToFirebase(title, description)
                }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()
        }

        loadTasksFromFirebase()
    }

    private fun addTaskToFirebase(title: String, description: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskId = FirebaseDatabase.getInstance().reference.push().key ?: return

        val task = Task(
            id = taskId,
            title = title,
            description = description,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("tasks")
            .child(userId)
            .child(taskId)
            .setValue(task)
            .addOnSuccessListener {
                Toast.makeText(this, "Tarea guardada en Firebase", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar la tarea", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTasksFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance().getReference("tasks").child(userId)

        ref.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                taskList.clear()

                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let {
                        it.id = taskSnapshot.key ?: ""

                        // ✅ Reasegura que isCompleted se lea correctamente
                        if (taskSnapshot.hasChild("isCompleted")) {
                            it.isCompleted = taskSnapshot.child("isCompleted").getValue(Boolean::class.java) == true
                        }

                        taskList.add(it)
                    }
                }

                taskAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Cerro Sesion con Exito", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Estás seguro de que quieres eliminar esta tarea?")
            .setPositiveButton("Sí") { _, _ ->
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                FirebaseDatabase.getInstance().getReference("tasks")
                    .child(userId)
                    .child(task.id)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(task: Task) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_task, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.taskTitleEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.taskDescriptionEditText)

        titleEditText.setText(task.title)
        descriptionEditText.setText(task.description)

        AlertDialog.Builder(this)
            .setTitle("Editar tarea")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val updatedTitle = titleEditText.text.toString().trim()
                val updatedDescription = descriptionEditText.text.toString().trim()

                if (updatedTitle.isEmpty()) {
                    Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedTask = task.copy(
                    title = updatedTitle,
                    description = updatedDescription
                )

                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton

                FirebaseDatabase.getInstance().getReference("tasks")
                    .child(userId)
                    .child(task.id)
                    .setValue(updatedTask)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

}