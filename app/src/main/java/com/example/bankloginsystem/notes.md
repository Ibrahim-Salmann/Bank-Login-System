SQL vs. NON-SQL in my code:

SQL (Relational Database):
[DatabaseHelper.k]
Defining tables with fixed columns and data types: TABLE_USERS HAS COLUMN_FIRST_NAME as TEXT, COLUMN_ID as INTEGER
The SQLiteOpenHelper class has that written code define my structure.
The SQL language: SELECT * FROM users WHERE email = ?, is to interact with the data
The data is connected through a "foreign key" linking 'user_library' to 'users' and 'books'

NoSQL or NON-SQL (Non-Relational Database):
Creating a bigger cabinet-esq database storing documents (JSON).
No pre-defined structure. Adding new fields and documents without having to update the schema.
[FirebaseDatabase.getInstance() in FirebaseManager.k]
Data is organised in a tree-like structure with paths: { /users/{user_id}/books/{books_id} }
retrieving data through listening: database.child("books").child(userId)
