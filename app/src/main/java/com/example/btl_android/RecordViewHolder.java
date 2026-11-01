package com.example.btl_android;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

/** ViewHolder theo mẫu StudentViewHolder của thầy. */
public class RecordViewHolder extends RecyclerView.ViewHolder {
    final TextView txtTitle, txtInfo;
    final MaterialButton btnDetail;

    public RecordViewHolder(@NonNull View itemView) {
        super(itemView);
        txtTitle = itemView.findViewById(R.id.txtTitle);
        txtInfo = itemView.findViewById(R.id.txtInfo);
        btnDetail = itemView.findViewById(R.id.btnDetail);
    }
}
