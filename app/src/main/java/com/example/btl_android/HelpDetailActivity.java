package com.example.btl_android;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class HelpDetailActivity extends AppCompatActivity {
    public static final String EXTRA_FAQ = "isFaq";
    public static final String EXTRA_INDEX = "articleIndex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_detail);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        boolean faq = getIntent().getBooleanExtra(EXTRA_FAQ, false);
        int index = getIntent().getIntExtra(EXTRA_INDEX, -1);
        List<HelpContent.Article> items = faq ? HelpContent.faqs() : HelpContent.guides();
        if (index < 0 || index >= items.size()) {
            finish();
            return;
        }
        HelpContent.Article article = items.get(index);
        ((TextView) findViewById(R.id.tvSectionName)).setText(faq ? "Câu hỏi thường gặp" : "Quy trình hướng dẫn");
        ((TextView) findViewById(R.id.tvArticleTitle)).setText(article.title);
        ((TextView) findViewById(R.id.tvArticleContent)).setText(article.content);
    }
}
