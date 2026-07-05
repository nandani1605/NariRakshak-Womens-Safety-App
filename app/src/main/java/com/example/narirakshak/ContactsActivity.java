package com.example.narirakshak;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
public class ContactsActivity extends AppCompatActivity {

    EditText etContactName, etContactNumber;
    Button btnSaveContact;
    ListView listContacts;

    FirebaseFirestore db;
    FirebaseAuth auth;

    ArrayList<Contact> contactList;
    ContactAdapter adapter;

    String editingContactId = null;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etContactName = findViewById(R.id.etContactName);
        etContactNumber = findViewById(R.id.etContactNumber);
        btnSaveContact = findViewById(R.id.btnSaveContact);
        listContacts = findViewById(R.id.listContacts);

        contactList = new ArrayList<>();
        adapter = new ContactAdapter(this, contactList);
        listContacts.setAdapter(adapter);

        btnSaveContact.setOnClickListener(v -> {
            if (editingContactId == null) {
                saveContact();
            } else {
                updateContact();
            }
        });

        // App khulte hi live listener on ho jayega
        loadContactsLive();
    }

    private void saveContact() {
        String name = etContactName.getText().toString().trim();
        String number = etContactNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number)) {
            Toast.makeText(this, "Enter name and number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> contact = new HashMap<>();
        contact.put("name", name);
        contact.put("number", number);

        // NAYA CHANGE: Button dabte hi turant khali kar do, Firebase ka wait mat karo!
        etContactName.setText("");
        etContactNumber.setText("");
        etContactName.clearFocus();
        etContactNumber.clearFocus();

        db.collection("users").document(uid).collection("contacts").add(contact)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Emergency Contact Saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save!", Toast.LENGTH_SHORT).show();
                });
    }

    public void editContactRequest(Contact contact) {
        etContactName.setText(contact.getName());
        etContactNumber.setText(contact.getNumber());
        btnSaveContact.setText("Update Contact");
        editingContactId = contact.getId();
    }

    private void updateContact() {
        String name = etContactName.getText().toString().trim();
        String number = etContactNumber.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number)) {
            Toast.makeText(this, "Enter name and number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null || editingContactId == null) return;
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> contactMap = new HashMap<>();
        contactMap.put("name", name);
        contactMap.put("number", number);

        String tempId = editingContactId; // ID save kar li

        // NAYA CHANGE: Edit save hote hi turant khali aur reset kar do!
        etContactName.setText("");
        etContactNumber.setText("");
        etContactName.clearFocus();
        etContactNumber.clearFocus();
        btnSaveContact.setText("Save Contact");
        editingContactId = null;

        db.collection("users").document(uid).collection("contacts").document(tempId)
                .update(contactMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Contact Updated Successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update Failed!", Toast.LENGTH_SHORT).show());
    }

    public void deleteContact(String contactId, int position) {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("contacts").document(contactId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Contact Deleted!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Delete Failed!", Toast.LENGTH_SHORT).show());
    }

    // LIVE REFRESH WALA MAGIC FUNCTION
    private void loadContactsLive() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("contacts")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) return;

                    if (queryDocumentSnapshots != null) {
                        adapter.clear();

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String id = doc.getId();
                            String name = doc.getString("name");
                            String number = doc.getString("number");
                            adapter.add(new Contact(id, name, number));
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}