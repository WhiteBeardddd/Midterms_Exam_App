package com.example.midtermsexam_beauty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.midtermsexam_beauty.R;
import com.example.midtermsexam_beauty.models.SellerProfile;

import java.util.List;

public class SellerCard extends RecyclerView.Adapter<SellerCard.ViewHolder> {

    private final Context context;
    private final List<SellerProfile> shopList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(SellerProfile seller);
    }

    public SellerCard(
            Context context,
            List<SellerProfile> shopList,
            OnItemClickListener listener
    ) {
        this.context = context;
        this.shopList = shopList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(context)
                .inflate(R.layout.seller_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        holder.bind(shopList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView bgImg;
        ImageView pfpImg;

        TextView shopName;
        TextView address;
        TextView description;
        TextView isOpen;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            bgImg        = itemView.findViewById(R.id.seller_bgimg);
            pfpImg       = itemView.findViewById(R.id.seller_pfpimg);
            shopName     = itemView.findViewById(R.id.seller_shopname);
            address      = itemView.findViewById(R.id.seller_address);
            description  = itemView.findViewById(R.id.seller_description);
            isOpen       = itemView.findViewById(R.id.seller_isopen);
        }

        public void bind(
                SellerProfile seller,
                OnItemClickListener listener
        ) {
            // Shop name
            String name = seller.getStoreName();
            shopName.setText((name != null && !name.isEmpty()) ? name : "");

            // Address — guard against null so it never shows "null"
            String addr = seller.getAddress();
            address.setText((addr != null && !addr.isEmpty()) ? addr : "");

            // Description — same guard
            String desc = seller.getDescription();
            description.setText((desc != null && !desc.isEmpty()) ? desc : "");

            // Open / Closed badge
            isOpen.setText(seller.isOpen() ? "Open" : "Closed");

            // Avatar
            Glide.with(itemView.getContext())
                    .load(seller.getSellerAvatarUrl())
                    .placeholder(R.drawable.tarabytes)
                    .into(pfpImg);

            // Background
            Glide.with(itemView.getContext())
                    .load(seller.getSellerProfileBg())
                    .placeholder(R.drawable._61858385_3853680348107377_8038201137559404165_n)
                    .into(bgImg);

            // Click listener
            itemView.setOnClickListener(v -> listener.onClick(seller));
        }
    }
}