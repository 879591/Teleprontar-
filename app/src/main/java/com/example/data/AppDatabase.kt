package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.Script
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Script::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teleprompter_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialScripts(database.scriptDao())
                    }
                }
            }

            private suspend fun populateInitialScripts(dao: ScriptDao) {
                val hindiScript = """
नमस्ते दोस्तों! आज के इस विशेष वीडियो में आपका हार्दिक स्वागत है।
आज हम बात करने वाले हैं डिजिटल क्रिएशन और कैमरा प्रेजेंस के बारे में।
जब आप कैमरे के सामने बोलते हैं, तो सबसे महत्वपूर्ण बात है आपका आत्मविश्वास और सीधा आई-कॉन्टैक्ट।
एक अच्छे टेलीप्रॉम्प्टर के साथ आप बिना झिझके, बिना रुके अपने विचारों को सीधे दर्शकों तक पहुँचा सकते हैं।
अपने बोलने की गति को सामान्य रखें और चेहरे पर एक हल्की मुस्कान बनाए रखें।
यदि आपको यह वीडियो उपयोगी लगे, तो लाइक करें और अपने साथी क्रिएटर्स के साथ शेयर करना न भूलें।
धन्यवाद, जय हिन्द!
                """.trimIndent()

                val englishScript = """
Welcome creators to this masterclass on confident video delivery!
Whether you are recording YouTube shorts, educational courses, or keynote speeches, eye contact is everything.
By positioning your script right beside the camera lens, you speak naturally while keeping viewers engaged.
Remember to breathe at natural sentence pauses, vary your tone, and let your natural enthusiasm shine through.
Let's make creating high-quality content effortless and fun!
                """.trimIndent()

                val techDemoScript = """
Hindi-English Tech Review (टेक रिव्यू)
दोस्तों, आज हम अनबॉक्स करने वाले हैं नए स्मार्टफोन को।
इस फोन का कैमरा 4K 60FPS वीडियो रिकॉर्डिंग सपोर्ट करता है और बैटरी लाइफ पूरे दिन चलती है।
Display is stunning with 120Hz refresh rate and AMOLED clarity.
गेमिंग परफॉरमेंस भी काफी स्मूथ है।
Let me know your thoughts in the comments below!
                """.trimIndent()

                dao.insertScript(
                    Script(
                        title = "हिंदी वीडियो स्क्रिप्ट - कैमरा प्रजेंस",
                        content = hindiScript,
                        wordCount = Script.calculateWordCount(hindiScript),
                        estimatedDurationSec = Script.calculateDurationSec(Script.calculateWordCount(hindiScript)),
                        language = "hi",
                        isFavorite = true
                    )
                )

                dao.insertScript(
                    Script(
                        title = "Mastering Eye Contact & Delivery",
                        content = englishScript,
                        wordCount = Script.calculateWordCount(englishScript),
                        estimatedDurationSec = Script.calculateDurationSec(Script.calculateWordCount(englishScript)),
                        language = "en",
                        isFavorite = true
                    )
                )

                dao.insertScript(
                    Script(
                        title = "Tech Review (हिंदी + English Mixed)",
                        content = techDemoScript,
                        wordCount = Script.calculateWordCount(techDemoScript),
                        estimatedDurationSec = Script.calculateDurationSec(Script.calculateWordCount(techDemoScript)),
                        language = "mixed",
                        isFavorite = false
                    )
                )
            }
        }
    }
}
