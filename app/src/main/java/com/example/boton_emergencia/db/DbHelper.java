package com.example.boton_emergencia.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DbHelper extends SQLiteOpenHelper {

    private static final int DATA_BASE_VERSION = 1;
    private static final String DATABASE_NOMBRE = "Boton_Emergencia_DB";

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTROL_NUMBER = "control_number";
    private static final String COLUMN_PASSWORD = "password_hash";
    private static final String COLUMN_SALT = "salt";

    // Contacts table (each contacto cercano linked to a user)
    private static final String TABLE_CONTACTS = "contacts";
    private static final String COLUMN_CONTACT_ID = "contact_id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_LABEL = "label";
    private static final String COLUMN_CREATED_AT = "created_at";

    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_CONTROL_NUMBER + " TEXT UNIQUE NOT NULL, "
        + COLUMN_PASSWORD + " TEXT NOT NULL, "
        + COLUMN_SALT + " TEXT NOT NULL" + ")";

    private static final String CREATE_TABLE_CONTACTS = "CREATE TABLE " + TABLE_CONTACTS + " ("
        + COLUMN_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_USER_ID + " INTEGER NOT NULL, "
        + COLUMN_PHONE + " TEXT NOT NULL, "
        + COLUMN_LABEL + " TEXT, "
        + COLUMN_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP, "
        + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ")"
        + ")";

    // Alerts table to record sent alerts
    private static final String TABLE_ALERTS = "alerts";
    private static final String COLUMN_ALERT_ID = "alert_id";
    private static final String COLUMN_ALERT_USER_ID = "user_id";
    private static final String COLUMN_ALERT_CONTACT_ID = "contact_id";
    private static final String COLUMN_ALERT_MESSAGE = "message";
    private static final String COLUMN_ALERT_CREATED_AT = "created_at";

    private static final String CREATE_TABLE_ALERTS = "CREATE TABLE " + TABLE_ALERTS + " ("
        + COLUMN_ALERT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_ALERT_USER_ID + " INTEGER NOT NULL, "
        + COLUMN_ALERT_CONTACT_ID + " INTEGER, "
        + COLUMN_ALERT_MESSAGE + " TEXT NOT NULL, "
        + COLUMN_ALERT_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP, "
        + "FOREIGN KEY(" + COLUMN_ALERT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "), "
        + "FOREIGN KEY(" + COLUMN_ALERT_CONTACT_ID + ") REFERENCES " + TABLE_CONTACTS + "(" + COLUMN_CONTACT_ID + ")"
        + ")";

    // Constructor that uses our DB name/version
    public DbHelper(@Nullable Context context) {
        super(context, DATABASE_NOMBRE, null, DATA_BASE_VERSION);
    }

    /**
     * Add a contact for a given user control number. Returns contact row id or -1.
     */
    public long addContact(String userControlNumber, String phone, String label) {
        int userId = getUserIdByControlNumber(userControlNumber);
        if (userId == -1) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_LABEL, label);
        long id = db.insert(TABLE_CONTACTS, null, values);
        db.close();
        return id;
    }

    /**
     * Get all contacts for a user (by control number). Returns a Cursor which the caller must close.
     */
    public Cursor getContactsForUser(String userControlNumber) {
        int userId = getUserIdByControlNumber(userControlNumber);
        if (userId == -1) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_USER_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(userId)};
        return db.query(TABLE_CONTACTS, null, selection, selectionArgs, null, null, COLUMN_CREATED_AT + " DESC");
    }

    /**
     * Update contact phone/label by contact id.
     */
    public int updateContact(long contactId, String phone, String label) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_LABEL, label);
        int rows = db.update(TABLE_CONTACTS, values, COLUMN_CONTACT_ID + "=?", new String[]{String.valueOf(contactId)});
        db.close();
        return rows;
    }

    /**
     * Delete contact by id.
     */
    public int deleteContact(long contactId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_CONTACTS, COLUMN_CONTACT_ID + "=?", new String[]{String.valueOf(contactId)});
        db.close();
        return rows;
    }

    /**
     * Helper to get user id from control number, or -1 if not found.
     */
    private int getUserIdByControlNumber(String controlNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_CONTROL_NUMBER + "=?";
        String[] selectionArgs = new String[]{controlNumber};
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID}, selection, selectionArgs, null, null, null);
        int id = -1;
        if (cursor != null && cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
        }
        if (cursor != null) cursor.close();
        db.close();
        return id;
    }

    /**
     * Public helper to check whether a user exists for a given control number.
     */
    public boolean userExists(String controlNumber) {
        return getUserIdByControlNumber(controlNumber) != -1;
    }

    /**
     * Add an alert record for auditing. contactId may be null (use -1).
     */
    public long addAlert(String userControlNumber, Long contactId, String message) {
        int userId = getUserIdByControlNumber(userControlNumber);
        if (userId == -1) return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ALERT_USER_ID, userId);
        if (contactId != null && contactId > 0) values.put(COLUMN_ALERT_CONTACT_ID, contactId);
        values.put(COLUMN_ALERT_MESSAGE, message);
        long id = db.insert(TABLE_ALERTS, null, values);
        db.close();
        return id;
    }

    /**
     * Get alerts for a user by control number. Returns Cursor; caller must close.
     */
    public Cursor getAlertsForUser(String userControlNumber) {
        int userId = getUserIdByControlNumber(userControlNumber);
        if (userId == -1) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_ALERT_USER_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(userId)};
        return db.query(TABLE_ALERTS, null, selection, selectionArgs, null, null, COLUMN_ALERT_CREATED_AT + " DESC");
    }

    /**
     * Get a single contact by its id. Caller must close the returned Cursor.
     */
    public Cursor getContactById(long contactId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(contactId)};
        return db.query(TABLE_CONTACTS, null, selection, selectionArgs, null, null, null);
    }

    // Keep compatibility constructor if other code used it
    public DbHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name == null ? DATABASE_NOMBRE : name, factory, version <= 0 ? DATA_BASE_VERSION : version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_CONTACTS);
        // Create default admin user (control_number: "admin", password: "admin")
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_CONTROL_NUMBER, "admin");
            byte[] salt = generateSalt();
            String saltHex = bytesToHex(salt);
            String hash = pbkdf2("admin", salt);
            values.put(COLUMN_PASSWORD, hash);
            values.put(COLUMN_SALT, saltHex);
            db.insert(TABLE_USERS, null, values);
        } catch (Exception e) {
            // ignore insertion errors here (e.g., hashing failures)
            e.printStackTrace();
        }
        // Create alerts table
        db.execSQL(CREATE_TABLE_ALERTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed and recreate. For production, use migrations.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    /**
     * Add a new user (control number + password). Password is stored as SHA-256 hash.
     * Returns row ID or -1 on failure (e.g., duplicate control number).
     */
    public long addUser(String controlNumber, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTROL_NUMBER, controlNumber);
        byte[] salt = generateSalt();
        String saltHex = bytesToHex(salt);
        String hash = pbkdf2(password, salt);
        values.put(COLUMN_PASSWORD, hash);
        values.put(COLUMN_SALT, saltHex);
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    /**
     * Check whether a user exists with given control number and password.
     */
    public boolean checkUser(String controlNumber, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_CONTROL_NUMBER + "=?";
        String[] selectionArgs = new String[]{controlNumber};
        Cursor cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID, COLUMN_PASSWORD, COLUMN_SALT}, selection, selectionArgs, null, null, null);
        boolean exists = false;
        if (cursor != null && cursor.moveToFirst()) {
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
            String saltHex = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SALT));
            byte[] salt = hexToBytes(saltHex);
            String computed = pbkdf2(password, salt);
            exists = storedHash.equals(computed);
        }
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    // PBKDF2 helpers
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32; // 256 bits
    private static final int PBKDF2_ITERATIONS = 10000;

    private byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        sr.nextBytes(salt);
        return salt;
    }

    private String pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // hex helpers
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
