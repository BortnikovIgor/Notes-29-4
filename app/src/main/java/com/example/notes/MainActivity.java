package com.example.notes;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.NoteAdapter.OnNoteActionListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "notes_channel";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;

    private NoteAdapter adapter;
    private List<Note> notes;
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    // Ланчер для получения результата из экрана добавления/редактирования
    private ActivityResultLauncher<Intent> editNoteLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            String title   = data.getStringExtra("title");
                            String content = data.getStringExtra("content");
                            int position   = data.getIntExtra("position", -1);

                            if (position == -1) {
                                // Добавление новой заметки
                                addNote(title, content);
                            } else {
                                // Редактирование существующей заметки
                                notes.set(position, new Note(title, content));
                                adapter.notifyItemChanged(position);
                                saveNotes();
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Устанавливаем SplashScreen
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) Создаем канал уведомлений (Android 8.0+)
        createNotificationChannel();
        // 2) Запрашиваем разрешение на отправку уведомлений (Android 13+)
        requestNotificationPermissionIfNeeded();

        // 3) Инициализируем SharedPreferences
        prefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);

        // 4) Загружаем список заметок
        String json = prefs.getString("notes", "");
        if (json.isEmpty()) {
            notes = new ArrayList<>();
        } else {
            Type type = new TypeToken<List<Note>>(){}.getType();
            notes = gson.fromJson(json, type);
        }

        // 5) Настраиваем RecyclerView и адаптер с контекстным меню
        RecyclerView rv = findViewById(R.id.recyclerView);
        adapter = new NoteAdapter(notes, new OnNoteActionListener() {
            @Override
            public void onEdit(int position) {
                // Открываем экран редактирования
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                Note note = notes.get(position);
                intent.putExtra("position", position);
                intent.putExtra("title",    note.getTitle());
                intent.putExtra("content",  note.getContent());
                editNoteLauncher.launch(intent);
            }
            @Override
            public void onDelete(int position) {
                // Удаляем заметку
                notes.remove(position);
                adapter.notifyItemRemoved(position);
                saveNotes();
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // 6) Кнопка "Создать заметку"
        MaterialButton btnCreate = findViewById(R.id.btnCreateNote);
        btnCreate.setOnClickListener(v -> openAddNote());

        // 7) Плавающая кнопка для создания заметки
        FloatingActionButton fab = findViewById(R.id.fabAddNote);
        fab.setOnClickListener(v -> openAddNote());
    }

    /** Создает NotificationChannel для Android 8.0+ */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Заметки";
            String description = "Уведомления приложения Notes";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /** Запрашивает разрешение на отправку уведомлений (Android 13+) */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            // Здесь можно обработать отказ или согласие пользователя
        }
    }

    /** Открывает экран добавления заметки */
    private void openAddNote() {
        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra("position", -1);
        editNoteLauncher.launch(intent);
    }

    /**
     * Добавляет новую заметку, сохраняет и отправляет уведомление
     */
    private void addNote(String title, String content) {
        notes.add(new Note(title, content));
        adapter.notifyItemInserted(notes.size() - 1);
        saveNotes();
        sendNoteNotification(title, content);
    }

    /** Сохраняет список заметок в SharedPreferences */
    private void saveNotes() {
        String json = gson.toJson(notes);
        prefs.edit()
                .putString("notes", json)
                .apply();
    }

    /**
     * Отправляет локальное уведомление о новой заметке
     */
    private void sendNoteNotification(String title, String content) {
        // Проверяем разрешение (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle("Новая заметка: " + title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        int notificationId = (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
    }
}
