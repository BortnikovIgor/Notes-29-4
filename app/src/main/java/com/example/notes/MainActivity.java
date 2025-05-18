package com.example.notes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.NoteAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NoteAdapter adapter;
    private List<Note> notes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Инициализируем RecyclerView и список
        RecyclerView rv = findViewById(R.id.recyclerView);
        notes = new ArrayList<>();
        adapter = new NoteAdapter(notes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // 2) Кнопка "Создать заметку"
        MaterialButton btnCreate = findViewById(R.id.btnCreateNote);
        btnCreate.setOnClickListener(v -> showCreateDialog());
    }

    /**
     * Показываем диалог ввода новой заметки.
     */
    private void showCreateDialog() {
        // 1) Inflate layout диалога (создаём файл dialog_new_note.xml в res/layout)
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_note, null);
        EditText etTitle   = dialogView.findViewById(R.id.etTitle);
        EditText etContent = dialogView.findViewById(R.id.etContent);

        new AlertDialog.Builder(this)
                .setTitle("Новая заметка")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String title   = etTitle.getText().toString().trim();
                    String content = etContent.getText().toString().trim();
                    if (!title.isEmpty() || !content.isEmpty()) {
                        addNote(title, content);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Добавляем новую заметку в список и обновляем адаптер.
     */
    private void addNote(String title, String content) {
        notes.add(new Note(title, content));
        adapter.notifyItemInserted(notes.size() - 1);
    }
}
