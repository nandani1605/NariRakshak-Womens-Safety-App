package com.example.narirakshak;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class ContactAdapter extends ArrayAdapter<Contact> {
    private ContactsActivity activity; // Activity ka reference chahiye logic call karne ke liye

    public ContactAdapter(ContactsActivity context, ArrayList<Contact> contacts) {
        super(context, 0, contacts);
        this.activity = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_contact, parent, false);
        }

        Contact contact = getItem(position);
        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvNumber = convertView.findViewById(R.id.tvNumber);

        // XML mein ye do naye buttons/text banayenge hum
        TextView btnEdit = convertView.findViewById(R.id.btnEdit);
        TextView btnDelete = convertView.findViewById(R.id.btnDelete);

        tvName.setText(contact.getName());
        tvNumber.setText(contact.getNumber());

        // Delete aur Edit clicks ko activity mein bhejna
        btnDelete.setOnClickListener(v -> activity.deleteContact(contact.getId(), position));
        btnEdit.setOnClickListener(v -> activity.editContactRequest(contact));

        return convertView;
    }
}