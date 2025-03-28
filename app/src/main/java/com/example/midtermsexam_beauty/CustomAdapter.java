package com.example.midtermsexam_beauty;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

    Context context;
    ArrayList<String> personNames;
    ArrayList<String> personEmails;

    // Constructor
    public CustomAdapter(Context context, ArrayList<String> personNames, ArrayList<String> personEmails) {
        this.context = context;
        this.personNames = personNames;
        this.personEmails = personEmails;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_layout, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.nameTextView.setText(personNames.get(position));
        holder.emailTextView.setText(personEmails.get(position));
    }

    @Override
    public int getItemCount() {
        return personNames.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, emailTextView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.Name);
            emailTextView = itemView.findViewById(R.id.Email);
        }
    }
}
