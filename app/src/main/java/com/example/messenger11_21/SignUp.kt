package com.example.messenger11_21

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SignUp : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var signupBtn: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()
        editName = findViewById(R.id.name)
        editEmail = findViewById(R.id.email)
        editPassword = findViewById(R.id.password)
        signupBtn = findViewById(R.id.signupbtn)

        signupBtn.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()
            // Check form
            if (name == "") {
                Toast.makeText(this, "Name Missing", Toast.LENGTH_SHORT).show()
            } else if (email == "") {
                Toast.makeText(this, "Email Missing", Toast.LENGTH_SHORT).show()
            } else if (password == "") {
                Toast.makeText(this, "Password Missing", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6){
                editPassword.text.clear()
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                // Ensure that the username is unique
                var uniqueUser = true
                db = FirebaseDatabase.getInstance().getReference()
                db.child("user").addListenerForSingleValueEvent(object:ValueEventListener {
                //db.child("user").addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (postSnapshot in snapshot.children) {
                            val username = postSnapshot.child("name").value.toString()
                            if (username == name) {
                                uniqueUser = false
                            }
                        }
                        if (uniqueUser) {
                            signUp(name, email, password)
                        } else {
                            Toast.makeText(this@SignUp, "This username already exists. Please choose another", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            }
        }
    }

    private fun signUp(name: String, email: String, password: String){
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    addUserToDatabase(name, email, mAuth.currentUser?.uid!!)
                    val intent = Intent(this@SignUp, Contacts::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Failed to Add", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addUserToDatabase(name: String, email: String, uid: String){
        db = FirebaseDatabase.getInstance().getReference()
        db.child("user").child(uid).setValue(User(name, email, uid))
    }
}