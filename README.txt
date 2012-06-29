Xtreme Image Cacher README

Table of Contents:

1. Basic implementation

2. Advanced ImageLoad options
2.1 Manage the bitmap manually
2.2 Using the Options object

3. Optimizing performance
3.1 Controlling the Memory Cache
3.2 Precaching

4. Supporting API versions lower than 12

*****************************************
*                                       *
*    SECTION 1: Basic Implementation    *
*                                       *
*****************************************

FOR ACTIVITIES:

public class YourActivity extends Activity {
	private ImageLoader mImageLoader;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		...
		
		mImageLoader = new ImageLoader(this);
	}
	
	@Override
	public void onDestroy() {
		
		...
		
		mImageLoader.onDestroy();
	}
}

FOR FRAGMENTS:

public class YourFragment extends Fragment {
	private ImageLoader mImageLoader;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		...
		
		mImageLoader = new ImageLoader(this);
		
		...
		
	}
	
	@Override
	public void onDestroyView() {
		
		...
		
		mImageLoader.onDestroy();
	}
}

Here's how it works:

You pass in your ImageView and URL to the ImageLoader, then it loads the image from the URL into your ImageView. That's all you need for a basic image call.

mImageLoader.loadImage(imageView, url, null);

The ImageCacher will handle almost everything for you, including network calls, disk caching, RAM caching, and releasing references to your Activities and Fragments when they're destroyed.



***********************************************
*                                             *
*    SECTION 2: Advanced ImageLoad Options    *
*                                             *
***********************************************

Beyond the basic loadImage call, there are several ways you can customize your image loading.

------------------------------------------
-    2.1 Managing the bitmap manually    -
------------------------------------------

If you don't want the ImageLoader to actually put your bitmap into your ImageView (for example, if you wanted to animate it in yourself), you can access the override loadImage call and have the Bitmap passed back to you.

mImageLoader.loadImage(imageView, url, null, new ImageLoaderListener() {
	public void onImageAvailable(ImageView imageView, Bitmap bitmap, boolean isFromMemoryCache) {
		// You can animate your ImageView here!
		// Or, just do things that you need to do once the image is available.
	}
	
	public void onImageLoadError() {
		// In here, you can handle what happens if the bitmap fails to load.
	}
});

--------------------------------------
-    2.1 Using the Options object    -
--------------------------------------

You can provide a number of options into the loadImage call that can modify how the image is requested. You do this by creating an Options object.

ImageLoader.Options options = new ImageLoader.Options();

// Set options here

mImageLoader.loadImage(imageView, url, options);
// OR:
mImageLoader.loadImage(imageView, url, options, new ImageLoaderListener() { ... });

If no options are passed in, the ImageLoader uses the default Options set which includes some optimizations for image loading.

First: The image loader will auto-detect the size of the ImageView you are passing in and, if possible, optimize the size of the bitmap returned to minimize memory consumption.

Second: The image loader will automatically find memory savings if the image is much larger than the size of the screen by reducing the sample size during the decode process.

AVAILABLE OPTIONS:

overrideSampleSize - Force the ImageLoader to decode the image using this sample size.

heightBounds - Manually enter the minimum acceptable height of the returned bitmap. If the bitmap's height is higher than the value specified, the bitmap may be shrunk down in order to conserve memory, but it will be guaranteed that the bitmap is still higher than the value specified.

widthBounds - Manually enter the minimum acceptable width of the returned bitmap. If the bitmap's width is wider than the value specified, the bitmap may be shrunk down in order to conserve memory, but it will be guaranteed that the bitmap is still wider than the value specified.

autoDetectBounds - The ImageLoader will automatically detect whether to lower the size of the image returned based on the dimensions of the ImageView.

useScreenSizeAsBounds - If the image is more than four times larger than the screen, the app will automatically lower the returned bitmap size to conserve memory.

placeholderImageResourceId - The ImageLoader will enter this placeholder image into the ImageView before getting the bitmap.

unsuccessfulLoadResourceId - If the bitmap fails to load, the ImageLoader will put this resource into the ImageView.


*******************************************
*                                         *
*    SECTION 3: Optimizing Performance    *
*                                         *
*******************************************

------------------------------------------
-    2.1 Controlling the Memory Cache    -
------------------------------------------

You may encounter performance issues while using the ImageLoader, and likely, if using SDK version 11 and under, OutOfMemoryErrors.

You will have to customize the properties of the memory cache in order to optimize memory consumption.

There are three calls you can make to customize the memory cache:

In SDK Versions 11 and under, you can control the number of images that are retained in the image cache. The more images you maintain, the higher the performance, but the higher the chance of a memory leak. This call is imageLoader.setMemCacheSize(int numImages).

If you are using APK version 12+, the ImageLoader gives you the ability to specify the amount of memory you would like the image cacher to utilize. This call is the following:

imageLoader.setMaximumMemCacheSize(long, numBytes)

Depending on your target devices, or the different kinds of phones you would like to support, you can configure different memcache sizes in order to maximize performance and optimize memory consumption.

The final call is imageLoader.clearMemCache. This call is most usefull for SDK versions 11 and under. In the event that you are moving from screens that support a small number of large images to a screen that supports a large number of small images, you should clear the memcache first and then increase the number of images stored by the memcache system. By doing so, you reduce the possibility of your activities that use many small images from causing an out of memory exception.

------------------------
-    2.1 Precaching    -
------------------------

There is one more tool that the ImageLoader provides that can allow you to optimize performance. Using the precaching methods, the Image Loader will preemtively load the images your app requires into memory.