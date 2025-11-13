package com.example.bankloginsystem

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// Data class to represent a book record joined with user-specific data
data class UserBook(
    val id: Int, // This will be the ID from the user_library table
    val userId: Int,
    val bookId: Int,
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
        private const val DATABASE_VERSION = 2 // Incremented version for onUpgrade call

        // Table for all unique books
        const val BOOKS_TABLE_NAME = "books"
        const val COLUMN_BOOK_ID = "book_id" // Primary Key
        const val COLUMN_BOOK_NAME = "name"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_GENRE = "genre"
        const val COLUMN_COVER_URI = "cover_uri"

        // Table to link users to books (the user's library)
        const val USER_LIBRARY_TABLE_NAME = "user_library"
        const val COLUMN_LIBRARY_ID = "id" // Primary Key for the relation
        const val COLUMN_USER_ID_FK = "user_id"
        const val COLUMN_BOOK_ID_FK = "book_id"
        const val COLUMN_STATUS = "status"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createBooksTableQuery = """
            CREATE TABLE $BOOKS_TABLE_NAME (
                $COLUMN_BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BOOK_NAME TEXT NOT NULL,
                $COLUMN_AUTHOR TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_GENRE TEXT,
                $COLUMN_COVER_URI TEXT,
                UNIQUE ($COLUMN_BOOK_NAME, $COLUMN_AUTHOR)
            );
        """.trimIndent()

        val createUserLibraryTableQuery = """
            CREATE TABLE $USER_LIBRARY_TABLE_NAME (
                $COLUMN_LIBRARY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID_FK INTEGER NOT NULL,
                $COLUMN_BOOK_ID_FK INTEGER NOT NULL,
                $COLUMN_STATUS TEXT,
                FOREIGN KEY ($COLUMN_USER_ID_FK) REFERENCES users(id),
                FOREIGN KEY ($COLUMN_BOOK_ID_FK) REFERENCES $BOOKS_TABLE_NAME($COLUMN_BOOK_ID),
                UNIQUE ($COLUMN_USER_ID_FK, $COLUMN_BOOK_ID_FK)
            );
        """.trimIndent()

        db?.execSQL(createBooksTableQuery)
        db?.execSQL(createUserLibraryTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("DROP TABLE IF EXISTS user_books") // old table
            onCreate(db)
        }
    }

    //  Insert a book for a user
    @SuppressLint("UseKtx")
    fun insertUserBook(
        userId: Int,
        name: String,
        author: String,
        category: String,
        genre: String,
        coverUri: String?,
        status: String?
    ): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            // Step 1: Find or Create the book in the central `books` table.
            var bookId = findBook(db, name, author)
            if (bookId == -1L) {
                val bookValues = ContentValues().apply {
                    put(COLUMN_BOOK_NAME, name)
                    put(COLUMN_AUTHOR, author)
                    put(COLUMN_CATEGORY, category)
                    put(COLUMN_GENRE, genre)
                    put(COLUMN_COVER_URI, coverUri)
                }
                bookId = db.insert(BOOKS_TABLE_NAME, null, bookValues)
            }

            if (bookId == -1L) return false // Failed to find or create the book

            // Step 2: Add the book to the user's personal library.
            val libraryValues = ContentValues().apply {
                put(COLUMN_USER_ID_FK, userId)
                put(COLUMN_BOOK_ID_FK, bookId)
                put(COLUMN_STATUS, status)
            }
            // use insertWithOnConflict to ignore if the user already has the book.
            val result = db.insertWithOnConflict(USER_LIBRARY_TABLE_NAME, null, libraryValues, SQLiteDatabase.CONFLICT_IGNORE)

            db.setTransactionSuccessful()
            return result != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    private fun findBook(db: SQLiteDatabase, name: String, author: String): Long {
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                BOOKS_TABLE_NAME,
                arrayOf(COLUMN_BOOK_ID),
                "$COLUMN_BOOK_NAME = ? AND $COLUMN_AUTHOR = ?",
                arrayOf(name, author),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_BOOK_ID))
            }
        } finally {
            cursor?.close()
        }
        return -1L
    }

    //  Get all books for the logged-in user by joining the two tables
    fun getBooksByUser(userId: Int): List<UserBook> {
        val bookList = mutableListOf<UserBook>()
        val db = this.readableDatabase
        // A JOIN query to combine data from both tables
        val query = """
            SELECT
                lib.$COLUMN_LIBRARY_ID, lib.$COLUMN_USER_ID_FK, lib.$COLUMN_BOOK_ID_FK, lib.$COLUMN_STATUS,
                b.$COLUMN_BOOK_NAME, b.$COLUMN_AUTHOR, b.$COLUMN_CATEGORY, b.$COLUMN_GENRE, b.$COLUMN_COVER_URI
            FROM
                $USER_LIBRARY_TABLE_NAME lib
            JOIN
                $BOOKS_TABLE_NAME b ON lib.$COLUMN_BOOK_ID_FK = b.$COLUMN_BOOK_ID
            WHERE
                lib.$COLUMN_USER_ID_FK = ?
        """.trimIndent()

        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(query, arrayOf(userId.toString()))
            if (cursor.moveToFirst()) {
                do {
                    val coverUriIndex = cursor.getColumnIndexOrThrow(COLUMN_COVER_URI)
                    val coverUri = if (cursor.isNull(coverUriIndex)) null else cursor.getString(coverUriIndex)

                    val statusIndex = cursor.getColumnIndexOrThrow(COLUMN_STATUS)
                    val status = if (cursor.isNull(statusIndex)) null else cursor.getString(statusIndex)

                    val book = UserBook(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIBRARY_ID)),
                        userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID_FK)),
                        bookId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOOK_ID_FK)),
                        status = status,
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOK_NAME)),
                        author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        genre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)),
                        coverUri = coverUri
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
    fun updateBookStatus(libraryId: Int, status: String?): Boolean {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                if (status != null) {
                    put(COLUMN_STATUS, status)
                } else {
                    putNull(COLUMN_STATUS)
                }
            }
            val rows = db.update(
                USER_LIBRARY_TABLE_NAME,
                values,
                "$COLUMN_LIBRARY_ID = ?",
                arrayOf(libraryId.toString())
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
    fun updateBookCover(bookId: Int, newCoverUri: String?): Boolean {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(COLUMN_COVER_URI, newCoverUri)
            }
            val rows = db.update(
                BOOKS_TABLE_NAME,
                values,
                "$COLUMN_BOOK_ID = ?",
                arrayOf(bookId.toString())
            )
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.close()
        }
    }

    //  Delete a book from a user's library
    fun deleteBookFromLibrary(libraryId: Int): Boolean {
        val db = this.writableDatabase
        return try {
            val rows = db.delete(
                USER_LIBRARY_TABLE_NAME,
                "$COLUMN_LIBRARY_ID = ?",
                arrayOf(libraryId.toString())
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
