package com.example.notes;

import android.content.SharedPreferences;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private NoteAdapter adapter;
    private List<Note> notes;
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 0) Инициализируем SharedPreferences
        prefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);

        // 1) Загрузка списка заметок из SharedPreferences
        String json = prefs.getString("notes", "");
        if (json.isEmpty()) {
            notes = new ArrayList<>();
        } else {
            Type type = new TypeToken<List<Note>>(){}.getType();
            notes = gson.fromJson(json, type);
        }

        // 2) Настройка RecyclerView и адаптера
        RecyclerView rv = findViewById(R.id.recyclerView);
        adapter = new NoteAdapter(notes);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // 3) Кнопка "Создать заметку"
        MaterialButton btnCreate = findViewById(R.id.btnCreateNote);
        btnCreate.setOnClickListener(v -> showCreateDialog());

        // 4) Плавающая кнопка для создания заметки
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        fab.setOnClickListener(v -> showCreateDialog());
    }

    /**
     * Показываем диалог ввода новой заметки.
     * При сохранении нужно будет добавить вызов saveNotes().
     */
    private void showCreateDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_new_note, null);
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
     * Добавляем новую заметку в список, обновляем адаптер и сохраняем.
     */
    private void addNote(String title, String content) {
        notes.add(new Note(title, content));
        adapter.notifyItemInserted(notes.size() - 1);
        saveNotes();  // после загрузки вам нужно ещё реализовать этот метод
    }

    /**
     * Сохраняем текущий список заметок в SharedPreferences.
     */
    private void saveNotes() {
        String json = gson.toJson(notes);
        prefs.edit()
                .putString("notes", json)
                .apply();
    }
}
