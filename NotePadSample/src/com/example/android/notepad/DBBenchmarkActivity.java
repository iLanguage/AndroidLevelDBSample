package com.example.android.notepad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.code.p.leveldb.LevelDB;
import com.google.code.p.leveldb.provider.LevelDBProvider;
import com.google.code.p.leveldb.provider.NotePad.Notes;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;

public class DBBenchmarkActivity extends Activity {

  private static final String TAG = "DBbenchmark";

  private static final String[] PROJECTION = new String[] { "key", // 0
      "value" // 1
  };

  Cursor mCursor;
  Uri mUri;
  ContentValues mContentValues;
  TextView tv;
  ArrayList<String> mIndex;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    tv = new TextView(this);
  }

  @Override
  protected void onResume() {

    mIndex = new ArrayList<String>();
    FillDatabaseWithTextFile f = new FillDatabaseWithTextFile();
    f.setFilename("leipzig-tr.txt");// "leipzig-tr.txt"
    f.setIndex(mIndex);
    f.setTextView(tv);
    f.execute();

    /*
     * Query entries randomly
     */
    // queryEntriesRandomly();

    super.onResume();
  }

  public class FillDatabaseWithTextFile extends AsyncTask<Void, Void, String> {
    private String mFilename;
    ArrayList<String> mIndex;
    TextView tv;

    @Override
    protected String doInBackground(Void... params) {

      if (mFilename == null || mIndex == null) {
        return "missing file";
      }

      ContentResolver cr = getContentResolver();
      ContentValues mContentValues = new ContentValues();
      String toMakeIndex = "";
      long before = System.currentTimeMillis();

      try {
        AssetManager assetManager = getApplicationContext().getAssets();
        InputStream in = assetManager.open(mFilename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in,
            "UTF-8"));
        String lookingForASentence = "";
        Map<String, Integer> words = new HashMap<String, Integer>();
        int types = 0;
        int tokens = 0;
        for (String line = reader.readLine(); line != null; line = reader
            .readLine()) {
          String[] lineWords = line.split("\\s+");
          for (String word : lineWords) {
            int count = 1;
            Integer currentCount = words.get(word);
            if (currentCount != null) {
              count = currentCount + 1;
            }
            tokens++;
            words.put(word, count);
          }
          String key = System.currentTimeMillis() + "";
          toMakeIndex += "," + key;
          mContentValues.put("key", key);
          mContentValues.put("value", lookingForASentence);
          mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);

        }

        mContentValues.put("key", "words");
        mContentValues.put("value", (new JSONObject(words).toString()));
        mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);

        reader.close();
        long after = System.currentTimeMillis();

        mIndex = new ArrayList<String>(Arrays.asList((toMakeIndex.split(","))));
        types = words.size() - 1;
        int j = mIndex.size() - 1;
        String result = "entries " + j + " types/tokens: " + types + "/"
            + tokens + " : " + types / tokens + " : took " + (after - before)
            + " ms : word frequencies: " + (new JSONObject(words).toString());
        return result;

      } catch (IOException e) {
        return "File read problem" + e.getLocalizedMessage();
      }

    }

    protected void onPreExecute() {
    }

    protected void setFilename(String filename) {
      this.mFilename = filename;
    }

    protected void setIndex(ArrayList<String> index) {
      this.mIndex = index;
    }

    protected void setTextView(TextView tv) {
      this.tv = tv;
    }

    protected void onPostExecute(String result) {
      tv.setText(result);
      setContentView(tv);
    }
  }

  public class FillDatabaseWithJSONFile extends AsyncTask<Void, Void, String> {
    private String mFilename;
    ArrayList<String> mIndex;
    TextView tv;

    @Override
    protected String doInBackground(Void... params) {

      if (mFilename == null || mIndex == null) {
        return "missing file";
      }

      ContentResolver cr = getContentResolver();
      ContentValues mContentValues = new ContentValues();

      try {
        AssetManager assetManager = getApplicationContext().getAssets();
        InputStream in = assetManager.open(mFilename);
        BufferedReader sourcefile = new BufferedReader(new InputStreamReader(
            in, "UTF-8"));
        String contents = "";
        String line = "";
        while ((line = sourcefile.readLine()) != null) {
          contents = contents + "\n" + line;
        }
        sourcefile.close();
        JSONArray json = new JSONArray(contents);

        for (int i = 0; i < json.length(); ++i) {
          JSONObject rec = json.getJSONObject(i);
          String key = rec.getString("id");
          String value = rec.toString();

          mIndex.add(key);

          mContentValues.put("key", key);
          mContentValues.put("value", value);
          mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);
        }

        int j = json.length() - 1;
        String result = "element" + j;
        return result;

      } catch (IOException e) {
        return "File read problem" + e.getLocalizedMessage();
      } catch (JSONException e) {
        return "Json problem" + e.getLocalizedMessage();
      }

    }

    protected void onPreExecute() {
    }

    protected void setFilename(String filename) {
      this.mFilename = filename;
    }

    protected void setIndex(ArrayList<String> index) {
      this.mIndex = index;
    }

    protected void setTextView(TextView tv) {
      this.tv = tv;
    }

    protected void onPostExecute(String result) {
      tv.setText(result);
      setContentView(tv);
    }
  }

  public long queryEntriesRandomly() {
    ContentResolver cr = getContentResolver();

    long startime = System.currentTimeMillis();
    int maxkey = mIndex.size() - 1;
    int querycount = 1000;
    Random randomGenerator = new Random();
    for (int k = 0; k < querycount; k++) {
      int randomKey = randomGenerator.nextInt(maxkey);
      mCursor = cr.query(
          Uri.withAppendedPath(LevelDBProvider.CONTENT_URI,
              mIndex.get(randomKey)), null, null, null, null);
      if (mCursor != null) {
        mCursor.moveToFirst();
        String value = mCursor.getString(1);
        tv.setText(value);
        setContentView(tv);
        mCursor.close();
      }
    }
    long endtime = System.currentTimeMillis();
    long querytime = (endtime - startime);
    tv.setText("Random quering of " + querycount
        + " entries took this many miliseconds: " + querytime);
    setContentView(tv);
    return querytime;
  }

  private String readInFile(String filePath) throws IOException {
    File currentFile = new File(filePath);
    BufferedReader sourcefile = new BufferedReader(new InputStreamReader(
        new FileInputStream(currentFile), "UTF-8"));
    String contents = "";
    String line = "";

    while ((line = sourcefile.readLine()) != null) {

      contents = contents + "\n" + line;

    }
    sourcefile.close();
    return contents;
  }

  /*
   * Methods which wrap LevelDB calls, see jni/main.cc for details
   */
  // public native String dbOpen(String dbpath);
  //
  // public native String dbClose(String dbpath);
  //
  // public native String dbPut(String key1, String value1);
  //
  // public native String dbGet(String key1);
  //
  // public native String dbDelete(String key1);
  //
  // public native String dbDestroy(String dbpath);

  /*
   * A native method that is implemented by the 'hello-jni' native library,
   * which is packaged with this application.
   */
  // public native String stringFromJNI();

  /*
   * This is another native method declaration that is *not* implemented by
   * 'leveldb'. This is simply to show that you can declare as many native
   * methods in your Java code as you want, their implementation is searched in
   * the currently loaded native libraries only the first time you call them.
   * 
   * Trying to call this function will result in a
   * java.lang.UnsatisfiedLinkError exception !
   */
  // public native String unimplementedStringFromJNI();

  /*
   * this is used to load the 'leveldb' library on application startup. The
   * library has already been unpacked into
   * /data/data/com.example.HelloJni/lib/libleveldb.so at installation time by
   * the package manager.
   */
  // static {
  // System.loadLibrary("leveldb");
  // }

  @Override
  protected void onPause() {
    super.onPause();
    /*
     * Close the db in the onPause to not waste memory
     */
    // db.dbClose(mDBdir);
  }

  public void testDatabaseFunctions() {
    // Get content provider and cursor
    ContentResolver cr = getContentResolver();

    // Compare normal Content_uri to the leveldb uri
    Log.d(TAG, Settings.System.CONTENT_URI.toString());
    Log.d(TAG, LevelDBProvider.CONTENT_URI.toString());

    // Compare the cursor returned by a query on normal Content_uri and the
    // leveldb_uri
    Cursor cursor = cr.query(Settings.System.CONTENT_URI, null, null, null,
        null);
    if (cursor != null) {
      Log.d(TAG, cursor.getColumnNames()[0]);
    }
    Cursor c = cr.query(LevelDBProvider.CONTENT_URI, null, null, null, null);
    if (c != null) {
      Log.d(TAG, c.getColumnNames()[0]);
    }

    ContentValues cv = new ContentValues();
    cv.put("key", "thisisthekey");
    cv.put("value", "somestuff");
    Uri result = cr.insert(LevelDBProvider.CONTENT_URI, cv);
    Log.d(TAG, result.toString());

    int deletedcount = cr.delete(result, null, null);
    Log.d(TAG, "Deleted :" + deletedcount);

    deletedcount = cr.delete(result.withAppendedPath(result, "1"), null, null);
    Log.d(TAG, "Deleted :" + deletedcount);

    Cursor cdeleted = cr.query(LevelDBProvider.CONTENT_URI, null, null, null,
        null);
    if (cdeleted != null) {
      cdeleted.moveToFirst();
      String deletedvalue = cdeleted.getString(1);
      if (deletedvalue != null && !"".equals(deletedvalue)) {
        Log.d(TAG, "Value:" + deletedvalue + ":");
      } else {
        Log.d(TAG, "No value");
      }
      Log.e(TAG, "The cursor was not null, and it was supposed to be null.");
    }

    mContentValues = new ContentValues();
    mContentValues.put("key", "thisisanewkey");
    mContentValues.put("value", "somestuff");
    mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);
    mCursor = cr.query(mUri, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
      String value = mCursor.getString(1);
      Log.d(TAG, "Successfully inserted and queried: " + value);
    } else {
      Log.e(TAG, "The cursor was null, and its not supposed to be null.");
    }

    mContentValues.put("value", "updatedvalue");
    int updatedcount = cr.update(LevelDBProvider.CONTENT_URI, mContentValues,
        null, null);

    mCursor = cr.query(mUri, null, null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
      String value = mCursor.getString(1);
      Log.d(TAG, "Successfully updated and queried: " + value);
    } else {
      Log.e(TAG, "The cursor was null, and its not supposed.");
    }

    mContentValues = new ContentValues();
    mContentValues.put("key", "firstkey");
    mContentValues.put("value", "this is the value of the first key");
    mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);

    mContentValues.put("key", "secondkey");
    mContentValues.put("value", "this is the value of the second key");
    mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);

    mContentValues.put("key", "keyToDelete");
    mContentValues.put("value",
        "this is the value of the key that i want to delete");
    mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);

    mContentValues.put("key", "fourthkey");
    mContentValues.put("value", "this is the value of the fourth key");
    mUri = cr.insert(LevelDBProvider.CONTENT_URI, mContentValues);

    deletedcount = cr.delete(mUri, null, null);

    mCursor = cr.query(
        Uri.withAppendedPath(LevelDBProvider.CONTENT_URI, "fourthkey"), null,
        null, null, null);
    if (mCursor != null) {
      mCursor.moveToFirst();
      String value = mCursor.getString(1);
      tv.setText(value);
      setContentView(tv);
    }
  }
}