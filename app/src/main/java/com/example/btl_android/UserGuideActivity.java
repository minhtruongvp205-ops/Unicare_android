package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserGuideActivity extends AppCompatActivity {
    private TextView tabGuides, tabFaq;
    private GuideAdapter adapter;
    private boolean showingFaq = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSupportContact).setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));

        tabGuides = findViewById(R.id.tabGuides);
        tabFaq = findViewById(R.id.tabFaq);

        RecyclerView rv = findViewById(R.id.rvGuides);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GuideAdapter();
        rv.setAdapter(adapter);

        tabGuides.setOnClickListener(v -> selectTab(false));
        tabFaq.setOnClickListener(v -> selectTab(true));
        selectTab(false);
    }

    private void selectTab(boolean faq) {
        showingFaq = faq;
        tabGuides.setBackgroundResource(faq ? android.R.color.transparent : R.drawable.bg_card_modern);
        tabFaq.setBackgroundResource(faq ? R.drawable.bg_card_modern : android.R.color.transparent);
        tabGuides.setTextColor(android.graphics.Color.parseColor(faq ? "#6B7280" : "#0D6EFD"));
        tabFaq.setTextColor(android.graphics.Color.parseColor(faq ? "#0D6EFD" : "#6B7280"));
        tabGuides.setTypeface(null, faq ? 0 : android.graphics.Typeface.BOLD);
        tabFaq.setTypeface(null, faq ? android.graphics.Typeface.BOLD : 0);
        adapter.update(faq ? HelpContent.faqs() : HelpContent.guides());
    }

    private class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.Holder> {
        private List<HelpContent.Article> items = HelpContent.guides();

        void update(List<HelpContent.Article> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            Holder(View v) { super(v); title = v.findViewById(R.id.tvGuideTitle); }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guide, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            HelpContent.Article article = items.get(position);
            holder.title.setText(article.title);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(UserGuideActivity.this, HelpDetailActivity.class);
                intent.putExtra(HelpDetailActivity.EXTRA_FAQ, showingFaq);
                intent.putExtra(HelpDetailActivity.EXTRA_INDEX, holder.getBindingAdapterPosition());
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return items.size(); }
    }
}
