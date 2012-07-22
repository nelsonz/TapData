/*
 * Copyright 2011, The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mai.whack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Browser;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class StickyNotesActivity extends Activity {
  private static final String TAG = "stickynotes";
  private boolean mResumed = false;
  private boolean mWriteMode = false;
  private HttpGetThread mHttpGetThread;
  private HttpPostThread mHttpPostThread;

  NfcAdapter mNfcAdapter;

  PendingIntent mNfcPendingIntent;
  IntentFilter[] mWriteTagFilters;
  IntentFilter[] mNdefExchangeFilters;

  AlertDialog writeDialog;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

    setContentView(R.layout.main);
    findViewById(R.id.write_tag).setOnClickListener(mTagWriter);

    // Handle all of our received NFC intents in this activity.
    mNfcPendingIntent =
      PendingIntent.getActivity(this, 0,
        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    // Intent filters for reading a note from a tag or exchanging over p2p.
    IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
    try {
      ndefDetected.addDataType("text/plain");
    } catch (MalformedMimeTypeException e) {
    }
    mNdefExchangeFilters = new IntentFilter[] { ndefDetected };

    // Intent filters for writing to a tag
    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
    mWriteTagFilters = new IntentFilter[] { tagDetected };
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Sticky notes received from Android
    if (!mResumed && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
      onDataRead(getIntent());
      mResumed = true;
    }
    enableNdefExchangeMode();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mResumed = false;
    mNfcAdapter.disableForegroundNdefPush(this);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    // NDEF exchange mode
    if (!mWriteMode && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
      onDataRead(intent);
    }

    // Tag writing mode
    if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
      onDataWrite(intent);
    }
  }

  private void onDataRead(Intent intent) {
    // Parse the intent
    NdefMessage[] msgs = null;
    String action = intent.getAction();
    byte[] tagId = null;
    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
      || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
      Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
      tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
      if (rawMsgs != null) {
        msgs = new NdefMessage[rawMsgs.length];
        for (int i = 0; i < rawMsgs.length; i++) {
          msgs[i] = (NdefMessage) rawMsgs[i];
        }
      } else {
        // Unknown tag type
        byte[] empty = new byte[] {};
        NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
        NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
        msgs = new NdefMessage[] { msg };
      }
    } else {
      Log.d(TAG, "Unknown intent.");
      finish();
    }

    String msg = new String(msgs[0].getRecords()[0].getPayload());
    mHttpGetThread = new HttpGetThread("http://192.168.1.192/store/" + toHex(tagId));
    mHttpGetThread.start();
    // mHttpPostThread = new HttpPostThread("aaaaa", "bbbbb", "sdgsdfdsfs");
    // mHttpPostThread.start();
  }

  public static String toHex(byte[] bytes) {
    BigInteger bi = new BigInteger(1, bytes);
    return String.format("%0" + (bytes.length << 1) + "X", bi);
  }

  private void onDataWrite(Intent intent) {
    byte[] tagId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
    Cursor mCur =
      this.managedQuery(Browser.BOOKMARKS_URI, Browser.HISTORY_PROJECTION, null, null, "DATE");
    // mCur.moveToFirst();
    if (mCur.moveToLast() && mCur.getCount() > 0) {
      String url = mCur.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
      mHttpPostThread = new HttpPostThread(toHex(tagId), "tabs", url);
      mHttpPostThread.start();
    }
  }

  public void handleData(String data) {
    JSONObject jsonObject;
    JSONArray jsonTabs = null;
    try {
      mHttpGetThread = null;
      jsonObject = new JSONObject(data);
      if (jsonObject.getString("type").equals("tabs")) {
        jsonTabs = jsonObject.getJSONArray("tabs");
        for (int i = 0; i < jsonTabs.length(); i++) {
          openBrowser(jsonTabs.getString(i));
        }
      } else if (jsonObject.getString("type").equals("files")) {
        jsonTabs = jsonObject.getJSONArray("files");
        String[] files = new String[jsonTabs.length()];
        for (int i = 0; i < jsonTabs.length(); i++) {
          files[i] = jsonTabs.getString(i);
        }
        onFileList(files);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void onMirror(String data) {
    JSONObject jsonObject;
    JSONArray jsonTabs = null;
    try {
      mHttpGetThread = null;
      jsonObject = new JSONObject(data);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void openBrowser(String url) {
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
      url = "http://" + url;
    }
    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    v.vibrate(300);

    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    startActivity(browserIntent);
  }

  private void onFileList(String[] files) {
    Intent intent = new Intent(StickyNotesActivity.this, FileListActivity.class);
    intent.putExtra("listOfFiles", files);
    startActivity(intent);
  }

  private TextWatcher mTextWatcher = new TextWatcher() {
    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

    }

    @Override
    public void afterTextChanged(Editable arg0) {
      if (mResumed) {
        mNfcAdapter.enableForegroundNdefPush(StickyNotesActivity.this, getNoteAsNdef());
      }
    }
  };

  private View.OnClickListener mTagWriter = new View.OnClickListener() {
    @Override
    public void onClick(View arg0) {
      // Write to a tag for as long as the dialog is shown.
      disableNdefExchangeMode();
      enableTagWriteMode();

      writeDialog =
        new AlertDialog.Builder(StickyNotesActivity.this)
          .setTitle("Whack your phone to a sticker to save your browser tab!!")
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              disableTagWriteMode();
              enableNdefExchangeMode();
            }
          }).create();

      writeDialog.show();
    }
  };

  private NdefMessage getNoteAsNdef() {
    byte[] textBytes = (new String("")).getBytes();
    NdefRecord textRecord =
      new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(), new byte[] {}, textBytes);
    return new NdefMessage(new NdefRecord[] { textRecord });
  }

  private void enableNdefExchangeMode() {
    mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mNdefExchangeFilters, null);
  }

  private void disableNdefExchangeMode() {
    mNfcAdapter.disableForegroundNdefPush(this);
    mNfcAdapter.disableForegroundDispatch(this);
  }

  private void enableTagWriteMode() {
    mWriteMode = true;
    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
    mWriteTagFilters = new IntentFilter[] { tagDetected };
    mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
  }

  private void disableTagWriteMode() {
    mWriteMode = false;
    mNfcAdapter.disableForegroundDispatch(this);
  }

  boolean writeTag(NdefMessage message, Tag tag) {
    int size = message.toByteArray().length;

    try {
      Ndef ndef = Ndef.get(tag);
      if (ndef != null) {
        ndef.connect();

        if (!ndef.isWritable()) {
          toast("Tag is read-only.");
          return false;
        }
        if (ndef.getMaxSize() < size) {
          toast("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.");
          return false;
        }

        ndef.writeNdefMessage(message);
        toast("Wrote message to pre-formatted tag.");
        return true;
      } else {
        NdefFormatable format = NdefFormatable.get(tag);
        if (format != null) {
          try {
            format.connect();
            format.format(message);
            toast("Formatted tag and wrote message");
            return true;
          } catch (IOException e) {
            toast("Failed to format tag.");
            return false;
          }
        } else {
          toast("Tag doesn't support NDEF.");
          return false;
        }
      }
    } catch (Exception e) {
      toast("Failed to write tag");
    }

    return false;
  }

  private void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  public class HttpGetThread extends Thread {
    String mUri;

    public HttpGetThread(String uri) {
      this.mUri = uri;
    }

    @Override
    public void run() {
      HttpClient client = new DefaultHttpClient();
      HttpGet request = new HttpGet();
      try {
        request.setURI(new URI(mUri));
      } catch (URISyntaxException e) {
        StickyNotesActivity.this.handleData("");
      }
      HttpResponse response;
      StringBuilder sb = new StringBuilder();
      try {
        response = client.execute(request);
        InputStream ips;
        ips = response.getEntity().getContent();
        BufferedReader buf = null;
        buf = new BufferedReader(new InputStreamReader(ips, "UTF-8"));

        String s = null;
        while (true) {
          s = buf.readLine();
          if (s == null || s.length() == 0) {
            break;
          }
          sb.append(s);
        }
        buf.close();
        ips.close();
      } catch (ClientProtocolException e) {
        StickyNotesActivity.this.handleData("");
      } catch (UnsupportedEncodingException e) {
        StickyNotesActivity.this.handleData("");
      } catch (IOException e) {
        StickyNotesActivity.this.handleData("");
      } catch (IllegalStateException e) {
        StickyNotesActivity.this.handleData("");
      }
      StickyNotesActivity.this.handleData(sb.toString());
    }
  }

  public class HttpPostThread extends Thread {
    String mHexId;
    String mType;
    String mData;

    public HttpPostThread(String hexId, String type, String data) {
      this.mData = data;
      this.mHexId = hexId;
      this.mType = type;
    }

    @Override
    public void run() {
      HttpClient client = new DefaultHttpClient();
      HttpPost post = new HttpPost();
      try {
        // Add your data
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("type", mType));
        nameValuePairs.add(new BasicNameValuePair("tabs[]", mData));
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        post.setURI(new URI("http://192.168.1.192/store/" + mHexId));
      } catch (UnsupportedEncodingException e) {
      } catch (URISyntaxException e) {
        StickyNotesActivity.this.handleData("");
      }
      HttpResponse response;
      StringBuilder sb = new StringBuilder();
      try {
        response = client.execute(post);
        InputStream ips;
        ips = response.getEntity().getContent();
        BufferedReader buf = null;
        buf = new BufferedReader(new InputStreamReader(ips, "UTF-8"));

        String s = null;
        while (true) {
          s = buf.readLine();
          if (s == null || s.length() == 0) {
            break;
          }
          sb.append(s);
        }
        buf.close();
        ips.close();

        writeDialog.cancel();
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(300);
      } catch (ClientProtocolException e) {
        StickyNotesActivity.this.handleData("");
      } catch (UnsupportedEncodingException e) {
        StickyNotesActivity.this.handleData("");
      } catch (IOException e) {
        StickyNotesActivity.this.handleData("");
      } catch (IllegalStateException e) {
        StickyNotesActivity.this.handleData("");
      }
    }
  }
}
