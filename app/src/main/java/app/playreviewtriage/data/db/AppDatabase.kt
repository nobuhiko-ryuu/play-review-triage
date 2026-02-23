package app.playreviewtriage.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.playreviewtriage.data.db.dao.ReviewDao
import app.playreviewtriage.data.db.entity.ReviewEntity

@Database(entities = [ReviewEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}
