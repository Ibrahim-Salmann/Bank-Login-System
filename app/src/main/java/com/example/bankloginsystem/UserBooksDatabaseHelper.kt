package com.example.bankloginsystem


import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Data class to represent a record from the user_books table
data class UserBook(
    val id: Int,
    val userId: Int,
    val bookId: Int?,
    val name: String,
    val author: String,
    val category: String,
    val genre: String,
    val coverUri: String?,
    val status: String?
)

class UserBooksDatabaseHelper(context: Context)
    : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "User_Books.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "user_books"
        const val COLUMN_ID = "id"                       // primary key
        const val COLUMN_USER_ID = "user_id"             // from BankUsers.db via session
        const val COLUMN_BOOK_ID = "book_id"             // from BookData.db (AddBookPage)
        const val COLUMN_BOOK_NAME = "book_name"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_GENRE = "genre"
        const val COLUMN_COVER_URI = "cover_uri"         // image URI or path
        const val COLUMN_STATUS = "status"               // “read”, “bookmark”, “dropped”, or null
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER,
                $COLUMN_BOOK_ID INTEGER,
                $COLUMN_BOOK_NAME TEXT,
                $COLUMN_AUTHOR TEXT,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_GENRE TEXT,
                $COLUMN_COVER_URI TEXT,
                $COLUMN_STATUS TEXT,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES users(id)
            );
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    //  Insert a book for a user
    fun insertUserBook(
        userId: Int,
        bookId: Int?,
        name: String,
        author: String,
        category: String,
        genre: String,
        coverUri: String?,
        status: String?
    ): Boolean {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_USER_ID, userId)
                if (bookId != null) {
                    put(COLUMN_BOOK_ID, bookId)
                } else {
                    putNull(COLUMN_BOOK_ID)
                }
                put(COLUMN_BOOK_NAME, name)
                put(COLUMN_AUTHOR, author)
                put(COLUMN_CATEGORY, category)
                put(COLUMN_GENRE, genre)
                put(COLUMN_COVER_URI, coverUri)
                put(COLUMN_STATUS, status)
            }
            db.insert(TABLE_NAME, null, values) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }

    //  Get all books for the logged-in user
    fun getBooksByUser(userId: Int): List<UserBook> {
        val bookList = mutableListOf<UserBook>()
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT * FROM $TABLE_NAME WHERE $COLUMN_USER_ID = ?",
                arrayOf(userId.toString())
            )
            if (cursor.moveToFirst()) {
                do {
                    val bookIdIndex = cursor.getColumnIndex(COLUMN_BOOK_ID)
                    val bookId = if (bookIdIndex != -1 && !cursor.isNull(bookIdIndex)) cursor.getInt(bookIdIndex) else null

                    val coverUriIndex = cursor.getColumnIndex(COLUMN_COVER_URI)
                    val coverUri = if (coverUriIndex != -1 && !cursor.isNull(coverUriIndex)) cursor.getString(coverUriIndex) else null

                    val statusIndex = cursor.getColumnIndex(COLUMN_STATUS)
                    val status = if (statusIndex != -1 && !cursor.isNull(statusIndex)) cursor.getString(statusIndex) else null

                    val book = UserBook(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                        bookId = bookId,
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOK_NAME)),
                        author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        genre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)),
                        coverUri = coverUri,
                        status = status
                    )
                    bookList.add(book)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db.close()
        }
        return bookList
    }

    //  Update book status (e.g. read/bookmarked/dropped)
    fun updateBookStatus(bookId: Int, userId: Int, status: String): Boolean {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_STATUS, status)
            }
            val rows = db.update(
                TABLE_NAME,
                values,
                "$COLUMN_BOOK_ID = ? AND $COLUMN_USER_ID = ?",
                arrayOf(bookId.toString(), userId.toString())
            )
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }

    // Update (for "Change Cover Image")
    fun updateBookCover(bookId: Int, newImagePath: String): Int {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_COVER_URI, newImagePath)
            }
            db.update(TABLE_NAME, values, "$COLUMN_ID=?", arrayOf(bookId.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        } finally {
            db.close()
        }
    }

    //  Delete a book
    fun deleteBook(bookId: Int, userId: Int): Boolean {
        val db = this.writableDatabase
        return try {
            val rows = db.delete(
                TABLE_NAME,
                "$COLUMN_BOOK_ID = ? AND $COLUMN_USER_ID = ?",
                arrayOf(bookId.toString(), userId.toString())
            )
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }
}
