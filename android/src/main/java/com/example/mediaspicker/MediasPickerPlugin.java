package com.example.mediaspicker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

/**
 * MediasPickerPlugin
 */
public class MediasPickerPlugin implements MethodCallHandler, ActivityResultListener, RequestPermissionsResultListener {
	/**
	 * Plugin registration.
	 */

	private Activity activity;
	private Result   result;
	private int      maxWidth, maxHeight, quality;

	private MediasPickerPlugin(Activity activity) {
		this.activity = activity;
	}

	public static void registerWith(Registrar registrar) {
		final MethodChannel channel = new MethodChannel(registrar.messenger(), "media_picker");
		MediasPickerPlugin plugin = new MediasPickerPlugin(registrar.activity());
		channel.setMethodCallHandler(plugin);

		registrar.addActivityResultListener(plugin);
		registrar.addRequestPermissionsResultListener(plugin);
    }

	@Override
	public void onMethodCall(MethodCall call, Result result) {
		this.result = result;
		switch (call.method) {
			case "pickImages":
				pickImages(call);
				break;
			case "pickVideos":
				pickVideos(call);
				break;
			case "deleteAllTempFiles":
				deleteAllTempFiles();
				break;
			case "compressImages":
				compressImages(call);
				break;
			case "checkPermission":
				result.success(checkPermission());
				break;
			case "requestPermission":
				requestPermission();
				break;
			default:
				result.notImplemented();
				this.result = null;
				break;
		}
	}

	private void pickImages(MethodCall call) {
		int quantity = 0;
		boolean withVideo = false;
		if (call.hasArgument("quantity")) {
			quantity = call.argument("quantity");
		}
		if (call.hasArgument("withVideo")) {
			withVideo = call.argument("withVideo");
		}
		maxWidth = call.argument("maxWidth");
		maxHeight = call.argument("maxHeight");
		quality = call.argument("quality");

		FilePickerBuilder filePickerBuilder = FilePickerBuilder.getInstance();
		if (quantity > 0) {
			filePickerBuilder = filePickerBuilder.setMaxCount(quantity);
		}
		filePickerBuilder.enableVideoPicker(withVideo)
				.enableImagePicker(true)
				.pickPhoto(activity);
	}

	private void pickVideos(MethodCall call) {
		int quantity = 0;
		if (call.hasArgument("quantity")) {
			quantity = call.argument("quantity");
		}
		FilePickerBuilder filePickerBuilder = FilePickerBuilder.getInstance();
		if (quantity > 0) {
			filePickerBuilder = filePickerBuilder.setMaxCount(quantity);
		}
		filePickerBuilder.enableVideoPicker(true)
				.enableImagePicker(false)
				.pickPhoto(activity);
	}

	private void compressImages(MethodCall call) {
		maxWidth = call.argument("maxWidth");
		maxHeight = call.argument("maxHeight");
		quality = call.argument("quality");
		List<String> imgPaths = call.argument("imgPaths");
		List<String> newImgPaths = new ArrayList<>();

		for (String path : imgPaths) {
			String newPath = CompressImage(path, maxWidth, maxHeight, quality);

			if (newPath != null && newPath != "") {
				newImgPaths.add(newPath);
			}

		}
		result.success(newImgPaths);
	}

	private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		final float totalPixels = width * height;
		final float totalReqPixelsCap = reqWidth * reqHeight * 2;
		while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
			inSampleSize++;
		}
		return inSampleSize;
	}


	@SuppressWarnings("deprecation")
    private String CompressImage(String filename, int maxWidth, int maxHeight, int quality) {
		Bitmap scaledBitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(new File(filename).getAbsolutePath(), options);
		int actualHeight = options.outHeight;
		int actualWidth = options.outWidth;

		float imgRatio = (float) actualWidth / (float) actualHeight;
		float maxRatio = (float) maxWidth / (float) maxHeight;

		if (actualHeight > maxHeight || actualWidth > maxWidth) {
			if (imgRatio < maxRatio) {
				imgRatio = (float) maxHeight / actualHeight;
				actualWidth = (int) (imgRatio * actualWidth);
				actualHeight = maxHeight;
			} else if (imgRatio > maxRatio) {
				imgRatio = (float) maxWidth / actualWidth;
				actualHeight = (int) (imgRatio * actualHeight);
				actualWidth = maxWidth;
			}
		}

		options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);
		options.inJustDecodeBounds = false;
		options.inDither = false;
		options.inPurgeable = true;
		options.inInputShareable = true;
		options.inTempStorage = new byte[16 * 1024];

		Bitmap bmp = null;
		try {
			bmp = BitmapFactory.decodeFile(filename, options);
		} catch (OutOfMemoryError exception) {
			exception.printStackTrace();
		}
		try {
			scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.RGB_565);
		} catch (OutOfMemoryError exception) {
			exception.printStackTrace();
		}

		float ratioX = actualWidth / (float) options.outWidth;
		float ratioY = actualHeight / (float) options.outHeight;
		float middleX = actualWidth / 2.0f;
		float middleY = actualHeight / 2.0f;

		Matrix scaleMatrix = new Matrix();
		scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);
		Canvas canvas = new Canvas(scaledBitmap);
		canvas.setMatrix(scaleMatrix);
		canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        bmp.recycle();

        ExifInterface exif;

		try {
			exif = new ExifInterface(filename);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
			Matrix matrix = new Matrix();
			if (orientation == 6) {
				matrix.postRotate(90);
			} else if (orientation == 3) {
				matrix.postRotate(180);
			} else if (orientation == 8) {
				matrix.postRotate(270);
			}
			scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		// compress to the format you want, JPEG, PNG...
		// 70 is the 0-100 quality percentage
		scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream);

		String tempDirPath = Environment.getExternalStorageDirectory() + File.separator + "TempImgs" + File.separator;
		String path = tempDirPath + UUID.randomUUID().toString() + ".jpg";

		File tempDir = new File(tempDirPath);

		File file = new File(path);
		try {
			if (!tempDir.exists()) {
				tempDir.mkdirs();
			}

			file.createNewFile();

			//write the bytes in file
			FileOutputStream fo = new FileOutputStream(file);
			fo.write(outStream.toByteArray());
			// remember close de FileOutput
			fo.close();

			return path;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return filename;
	}

	private void deleteAllTempFiles() {
		String tempDirPath = Environment.getExternalStorageDirectory() + File.separator + "TempImgs" + File.separator;
		File tempDir = new File(tempDirPath);
		if (tempDir.exists()) {
			String[] children = tempDir.list();
			for (int i = 0; i < children.length; i++) {
				new File(tempDir, children[i]).delete();
			}

			if (tempDir.delete()) {
				result.success(true);
			} else {
				result.success(false);
			}
		} else {
			result.success(true);
		}
	}

	private void requestPermission() {
		String[] perm = {Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.CAMERA};
		ActivityCompat.requestPermissions(activity, perm, 0);
	}

	private boolean checkPermission() {
		return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				&& PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
    }

	@Override
	public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		boolean res = false;

		if (requestCode == 0 && grantResults.length > 0) {
			res = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    res = false;
                }
            }

			result.success(res);
		}
		return res;
	}

	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == FilePickerConst.REQUEST_CODE_PHOTO) {
			List<String> docPaths = new ArrayList<>();
			if (intent != null) {
				docPaths = intent.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA);
			}
			result.success(docPaths);
			return true;
		}
		return false;
	}
}
