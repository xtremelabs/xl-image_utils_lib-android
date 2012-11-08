package com.xtremelabs.testactivity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import com.xtreme.testactivity.R;
import com.xtremelabs.imageutils.ImageLoader;

public class MainActivity extends Activity {

	private ImageLoader mImageLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mImageLoader = new ImageLoader(this);

		ListView list = (ListView) findViewById(R.id.list);
		list.setAdapter(new KittenAdapter(this, mImageLoader));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mImageLoader.destroy();
	}
}
