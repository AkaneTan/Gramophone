/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.development;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import org.akanework.gramophone.R;
import org.akanework.gramophone.SdScanner;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class MediaScannerActivity extends Activity {
	private TextView mTitle;
	private int mNumToInsert = 20;
	private int mArtists;
	private int mAlbums;
	private int mSongs;
	private ContentResolver mResolver;
	private Uri mAudioUri;
	ContentValues[] mValues = new ContentValues[10];
	Random mRandom = new Random();
	Handler mHandler = new Handler(Looper.getMainLooper());
	StringBuilder mBuilder = new StringBuilder();

	public MediaScannerActivity() {
	}

	/**
	 * Called when the activity is first created or resumed.
	 */
	@SuppressLint("UnspecifiedRegisterReceiverFlag")
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.media_scanner_activity);
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addDataScheme("file");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(mReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
		} else {
			registerReceiver(mReceiver, intentFilter);
		}
		EditText t = (EditText) findViewById(R.id.numsongs);
		t.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				String text = s.toString();
				try {
					mNumToInsert = Integer.parseInt(text);
				} catch (NumberFormatException ex) {
					mNumToInsert = 20;
				}
				setInsertButtonText();
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		mTitle = (TextView) findViewById(R.id.title);
		mResolver = getContentResolver();
		mAudioUri = Audio.Media.EXTERNAL_CONTENT_URI;
		for (int i = 0; i < 10; i++) {
			mValues[i] = new ContentValues();
		}
		setInsertButtonText();
	}

	/**
	 * Called when the activity going into the background or being destroyed.
	 */
	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiver);
		mInsertHandler.removeMessages(0);
		super.onDestroy();
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
				mTitle.setText("Media Scanner started scanning " + intent.getData().getPath());
			} else if (intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
				mTitle.setText("Media Scanner finished scanning " + intent.getData().getPath());
			}
		}
	};

	private void setInsertButtonText() {
		String label = getString(R.string.insertbutton, mNumToInsert);
		Button b = (Button) findViewById(R.id.insertbutton);
		b.setText(label);
	}

	public void insertItems(View v) {
		if (mInsertHandler.hasMessages(0)) {
			mInsertHandler.removeMessages(0);
			setInsertButtonText();
		} else {
			mInsertHandler.sendEmptyMessage(0);
		}
	}

	Handler mInsertHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			if (mNumToInsert-- > 0) {
				addAlbum();
				runOnUiThread(mDisplayUpdater);
				if (!isFinishing()) {
					sendEmptyMessage(0);
				}
			}
		}
	};
	Runnable mDisplayUpdater = new Runnable() {
		public void run() {
			mTitle.setText("Added " + mArtists + " artists, " + mAlbums + " albums, "
					+ mSongs + " songs.");
		}
	};

	// Add one more album (with 10 songs) to the database. This will be a compilation album,
	// with one album artist for the album, and a separate artist for each song.
	@SuppressLint("SdCardPath")
	private void addAlbum() {
		try {
			new File("/sdcard/bogus/").mkdir();
			InputStream bogusMp3 = getAssets().open("bogus.mp3");
			bogusMp3.mark(2147483647);
			String albumArtist = "Various Artists";
			String albumName = getRandomWord(3);
			int baseYear = 1969 + mRandom.nextInt(30);
			for (int i = 0; i < 10; i++) {
				mValues[i].clear();
				String artist = getRandomName();
				final ContentValues map = mValues[i];
				map.put(MediaStore.MediaColumns.DATA,
						"/sdcard/bogus/" + albumName + "/" + artist + "_" + i + ".mp3");
				File file=new File("/sdcard/bogus/" + albumName + "_" + artist + "_" + i + ".mp3");
				try (FileOutputStream f = new FileOutputStream(file)) {
					copy(bogusMp3, f);
				}
				bogusMp3.reset();
				bogusMp3.mark(2147483647);
				AudioFile f = AudioFileIO.read(file);
				Tag tag = f.getTag();
				tag.setField(FieldKey.TITLE,getRandomWord(4) + " " + getRandomWord(2) + " " + (i + 1));
				tag.setField(FieldKey.ARTIST,artist);
				tag.setField(FieldKey.ALBUM_ARTIST,albumArtist);
				tag.setField(FieldKey.ALBUM,albumName);
				tag.setField(FieldKey.TRACK, String.valueOf(i + 1));
				tag.setField(FieldKey.YEAR, String.valueOf(baseYear + mRandom.nextInt(10)));
				f.commit();
			}
			bogusMp3.close();
			mSongs += 10;
			mAlbums++;
			mArtists += 11;
		} catch (SQLiteConstraintException ex) {
			Log.d("@@@@", "insert failed", ex);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (CannotWriteException e) {
			throw new RuntimeException(e);
		} catch (CannotReadException e) {
			throw new RuntimeException(e);
		} catch (FieldDataInvalidException e) {
			throw new RuntimeException(e);
		} catch (TagException e) {
			throw new RuntimeException(e);
		} catch (InvalidAudioFrameException e) {
			throw new RuntimeException(e);
		} catch (ReadOnlyFileException e) {
			throw new RuntimeException(e);
		}
	}

	public void startScan(View v) {
		/*SdScanner.Companion.scan(getApplicationContext(),
				new File("/storage/emulated/0//"),
				false,
				prog -> {
			android.util.Log.e("test", prog.getStep() + " " + prog.getPercentage() + "% " + prog.getPath());
			mHandler.post(() -> {
				if (prog.getStep() == SdScanner.SimpleProgress.Step.DONE) {
					Toast.makeText(getApplicationContext(), "Done scanning :)", Toast.LENGTH_LONG).show();
				}
				if (prog.getStep() == SdScanner.SimpleProgress.Step.SCAN) {
					Toast.makeText(getApplicationContext(), "Scan progress " + prog.getPercentage(), Toast.LENGTH_SHORT).show();
				}
			});
				});*/
		startActivity(new Intent(Intent.ACTION_RUN)
				.setClassName("com.gmail.jerickson314.sdscanner",
						"com.gmail.jerickson314.sdscanner.MainActivity"));
	}

	/**
	 * Some code to generate random names. This just strings together random
	 * syllables, and randomly inserts a modifier between the first
	 * and last name.
	 */
	private String[] elements = new String[]{
			"ab", "am",
			"bra", "bri",
			"ci", "co",
			"de", "di", "do",
			"fa", "fi",
			"ki",
			"la", "li",
			"ma", "me", "mi", "mo",
			"na", "ni",
			"pa",
			"ta", "ti",
			"vi", "vo"
	};

	private String getRandomWord(int len) {
		int max = elements.length;
		mBuilder.setLength(0);
		for (int i = 0; i < len; i++) {
			mBuilder.append(elements[mRandom.nextInt(max)]);
		}
		char c = mBuilder.charAt(0);
		c = Character.toUpperCase(c);
		mBuilder.setCharAt(0, c);
		return mBuilder.toString();
	}

	private String getRandomName() {
		boolean longfirst = mRandom.nextInt(5) < 3;
		String first = getRandomWord(longfirst ? 3 : 2);
		String last = getRandomWord(3);
		switch (mRandom.nextInt(6)) {
			case 1:
				if (!last.startsWith("Di")) {
					last = "di " + last;
				}
				break;
			case 2:
				last = "van " + last;
				break;
			case 3:
				last = "de " + last;
				break;
		}
		return first + " " + last;
	}

	// buffer size used for reading and writing
	private static final int BUFFER_SIZE = 8192;

	/**
	 * Reads all bytes from an input stream and writes them to an output stream.
	 */
	private static long copy(InputStream source, OutputStream sink) throws IOException {
		long nread = 0L;
		byte[] buf = new byte[BUFFER_SIZE];
		int n;
		while ((n = source.read(buf)) > 0) {
			sink.write(buf, 0, n);
			nread += n;
		}
		return nread;
	}
}
