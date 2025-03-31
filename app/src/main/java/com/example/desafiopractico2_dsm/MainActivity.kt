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

        // Simulamos algunas tareas para probar
        taskList.add(Task(title = "Hacer tarea", description = "Terminar app DSM104"))
        taskList.add(Task(title = "Ir al gym", description = "Entrenar espalda"))

        taskAdapter = TaskAdapter(taskList)
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
                taskList.clear() // Limpiamos la lista actual

                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue(Task::class.java)
                    task?.let { taskList.add(it) }
                }

                taskAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error al cargar tareas", Toast.LENGTH_SHORT).show()
            }
        })
    }


}