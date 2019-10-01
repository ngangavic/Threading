package com.example.threading.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.threading.models.Word;


@Database(entities = {Word.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase mInstance;
    public abstract WordDao wordDataDao();
    public static final String DATABASE_NAME = "words_db";

    public static AppDatabase getDatabase(Context context) {
        if (mInstance == null) {
            mInstance =
                    Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            .build();
        }
        return mInstance;
    }

    public static void destroyInstance() {
        mInstance = null;
    }
}
