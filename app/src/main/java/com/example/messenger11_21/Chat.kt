package com.example.messenger11_21

import android.content.Intent
import android.graphics.Insets.add
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Chat : AppCompatActivity() {
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var db: DatabaseReference

    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        db = FirebaseDatabase.getInstance().getReference()

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        supportActionBar?.title = name

        messageRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sendBtn)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = messageAdapter

        // Add data to recycler view
        db.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        // Get full message
                        var message = postSnapshot.getValue(Message::class.java)
                        // Get just the encrypted message text
                        val encryptedMessage = message?.message.toString()
                        val passphrase = findViewById<EditText>(R.id.passphrase).text.toString()
                        if (passphrase.length >= 6) {
                            val decryptedMessage = decrypt(encryptedMessage, passphrase)
                            // change the encrypted message to the decrypted one
                            message?.message = decryptedMessage
                        }
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        val decryptBtn = findViewById<ImageView>(R.id.decryptBtn)
        decryptBtn.setOnClickListener{
            val passphrase = findViewById<EditText>(R.id.passphrase).text.toString()

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (child in snapshot.children) {
                        // Get full message
                        var message = child.getValue(Message::class.java)
                        // Get just the encrypted message text
                        val encryptedMessage = message?.message.toString()
                        if (passphrase.length < 6) {
                            Toast.makeText(this@Chat, "Passphrase must be at least 6 characters: $passphrase", Toast.LENGTH_SHORT).show()
                        } else {
                            // Or decrypt the message
                            val decryptedMessage = decrypt(encryptedMessage, passphrase)
                            // change the encrypted message to the decrypted one
                            message?.message = decryptedMessage
                        }
                        messageList.add(message!!)
                    }
                    messageAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) { }

            }
            db.child("chats").child(senderRoom!!).child("messages").addValueEventListener(listener)
        }

        // Add message to Firebase
        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            val passphrase = findViewById<EditText>(R.id.passphrase).text.toString()
            // Only send if passphrase is long enough
            if (passphrase.length < 6) {
                Toast.makeText(this@Chat, "Passphrase must be at least 6 characters: $passphrase", Toast.LENGTH_SHORT).show()
            } else {
                val encryptedMessage = encrypt(message, passphrase)
                val messageObject = Message(encryptedMessage, senderUid)
                db.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        db.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }
                messageBox.text.clear()
            }
        }

    }

    private fun createKey(phrase: String) : MutableList<Int> {
        val pLen = phrase.length
        val mid = (pLen / 2) - 1
        val fChar = phrase[0].toInt() - 32
        val mChar = phrase[mid].toInt() - 30
        val lChar = phrase[pLen-1].toInt() - 22
        var odd = 3

        if (pLen % 2 == 0)
            odd = 4

        var key : MutableList<Int> = ArrayList()

        // Start by transposing each character
        // by the number of chars in passphrase
        for (i in pLen..(pLen + 94)) {
            if (i > 93) {
                key.add(i - 94)
            } else {
                key.add(i)
            }
        }

        // Then move every 3rd or 4th character
        for (i in odd..94 step odd) {
            var value = key[i]
            key.removeAt(i)
            key.add(value)
        }

        // Then move characters many times
        for (i in lChar downTo 1 step 1) {
            for (j in 0..94 step i) {
                var value = key[j]
                key.removeAt(j)
                key.add(value)
            }
        }

        for (i in mChar..(mChar + (20 * fChar)) step fChar){
            val index = i % 93
            val num = key[index]
            key.removeAt(index)
            key.add(num)
        }
        return key
    }

    private fun encrypt(message: String, passphrase: String) : String {
        val key = createKey(passphrase)
        var encryptedMessage = ""
        for (i in 0..message.length-1) {
            var charPos = (message[i].toInt() - 32)
            var newChar = (key[charPos] + 32).toChar()
            encryptedMessage += newChar
        }
        return encryptedMessage
    }

    private fun decrypt(message: String, passphrase: String) : String {
        val key = createKey(passphrase)
        var decryptedMessage = ""
        for (i in message) {
            for (j in 0..94) {
                if ((key[j] + 32).toChar() == i) {
                    var newChar = (j + 32).toChar()
                    decryptedMessage += newChar
                    break
                }
            }
        }
        return decryptedMessage
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu2, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.exit){
            val intent = Intent(this, Contacts::class.java)
            finish()
            startActivity(intent)
            return true
        } else if (item.itemId == R.id.logout){
            val intent = Intent(this, Login::class.java)
            finish()
            startActivity(intent)
            return true
        }
        return true
    }
}