package com.firefishcomms.instagramlikegallery.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.firefishcomms.instagramlikegallery.R;
import com.firefishcomms.instagramlikegallery.activities.ImageEditingActivity;
import com.firefishcomms.instagramlikegallery.commons.ILGConstants;
import com.firefishcomms.instagramlikegallery.commons.ILGRequestCode;
import com.firefishcomms.instagramlikegallery.views.AutoFitTextureView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraFragment extends BaseFragment implements View.OnClickListener {
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSIONS = 1;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    public static final String TEMP_IMAGE_FILE_NAME = "temp_pic.jpg";

    private CameraManager mCameraManager;

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    private ImageButton btnCameraFacing;

    private enum CameraFacingMode {
        REAR,
        FRONT
    }

    /**
     * True if frontCamera is used
     */
    private CameraFacingMode currentCameraFacingMode = CameraFacingMode.REAR;

    /**
     * True if camera is already being previewed
     */
    private boolean cameraPreviewing = false;

    /**
     * This flag will be used to quick-fix a bug where the preview is stretched on camera being first opened
     * when the camera fragment is shown by finger-slide. View {@link #setUserVisibleHint(boolean)}
     */
    private boolean cameraFirstTimePreviewingFlag = true;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            getActivity().finish();
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    private ImageButton btnCameraFlashMode;

    private enum FlashMode {
        AUTO,
        ALWAYS,
        OFF
    }

    private int currentSelectedFlashMode = 0;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            // Do nothing
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            // Do nothing
        }

    };

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();

        for (Size option : choices) {
            if(option.getHeight() != option.getWidth()) continue;

            if (option.getWidth() >= textureViewWidth &&
                    option.getHeight() >= textureViewHeight) {
                bigEnough.add(option);
            } else {
                notBigEnough.add(option);
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * To choose optimal size for capture image
     * @param choices
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalImageSize(Size[] choices){
        // Collect the supported resolutions that are at least as big as the image size
        List<Size> suitableSizes = new ArrayList<>();

        for (Size option : choices) {
            if(option.getHeight() != option.getWidth()) continue;
            else suitableSizes.add(option);
        }

        // Pick the average of those available sizes. If there is no one available size, pick the default size in choices[]
        if (suitableSizes.size() == 1) {
            return suitableSizes.get(0);
        } else if(suitableSizes.size() > 1){
            return suitableSizes.get(suitableSizes.size()/2);
        } else {
            return choices[0];
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if(mBackgroundThread == null) return;

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * To calculate and get the range between the camera and the object for auto brightness
     * @return {@code Range}
     */
    private Range<Integer> getRange() {
        CameraCharacteristics chars = null;
        try {
            chars = mCameraManager.getCameraCharacteristics(mCameraId);
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Range<Integer> result = null;
            for (Range<Integer> range : ranges) {
                int upper = range.getUpper();
                // 10 - min range upper for my needs
                if (upper >= 10) {
                    if (result == null || upper < result.getUpper().intValue()) {
                        result = range;
                    }
                }
            }
            if (result == null) {
                result = ranges[0];
            }
            return result;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Opens the camera specified by .
     */
    private void openCamera(int width, int height) {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSIONS);
            return;
        }

        setUpCameraOutputs(width, height);
        configureTransform(width, height);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        try {
            if(mCameraId == null) throw new NullPointerException("No camera found on this device");

            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                throw new NullPointerException("No sizes available for camera preview");
            }

            // For still image captures, we use the largest available size.
            Size largest = chooseOptimalImageSize(map.getOutputSizes(ImageFormat.JPEG));
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getWidth(),
                    ImageFormat.JPEG, /*maxImages*/2);

            mImageReader.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler);

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            //noinspection ConstantConditions
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            Point displaySize = new Point();
            getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = displaySize.x;
            int maxPreviewHeight = displaySize.y;

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = displaySize.y;
                maxPreviewHeight = displaySize.x;
            }

            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            //TODO: show snackbar instead
            //showToast("Unsupported camera2")
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setCameraFlashMode(mPreviewRequestBuilder);
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,getRange());

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);

                                cameraPreviewing = true;
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            // TODO: Show snackbar instead
//                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);

            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setCameraFlashMode(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);

            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,getRange());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setCameraFlashMode(captureBuilder);

            /** This orientation is not in use as it does not work in all devices.
             * To do image orientation for captured image, {@link #processCapturedImage()} will do the work.**/
            /*
            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            */

            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                    processCapturedImage();

                    Intent intent = new Intent(getActivity(), ImageEditingActivity.class);
                    intent.putExtra(ILGConstants.EXTRA_SELECTED_IMAGE_FILE, mFile);

                    getActivity().startActivityForResult(intent, ILGRequestCode.IMAGE_EDIT_ADD_FILTER);
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void processCapturedImage(){
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(new FileInputStream(mFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if(bitmap == null) return;

        Matrix matrix = new Matrix();
        matrix.postRotate(CameraFacingMode.FRONT == currentCameraFacingMode ? -90 : 90);

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        try {
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(mFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void startCamera(){
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void stopCamera(){
        cameraPreviewing = false;
        closeCamera();
        stopBackgroundThread();
    }

    private void setUpCameraId(CameraFacingMode cameraFacingMode){
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

                // Check to use correct camera lens
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (CameraFacingMode.FRONT == cameraFacingMode && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    // Do nothing
                } else if (CameraFacingMode.REAR == cameraFacingMode && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    // Do nothing
                } else {
                    continue;
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setCameraFlashMode(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnCameraFlashMode.setVisibility(View.VISIBLE);
                }
            });

            switch (FlashMode.values()[currentSelectedFlashMode]){
                case ALWAYS: {
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    if(android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                        requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
                    }
                    break;
                }
                case OFF:{
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    if(android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                        requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
                        requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
                    }
                    break;
                }
                case AUTO:
                default:{
                    requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    if(android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                        requestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                        requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
                    }
                    break;
                }
            }
        } else{
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnCameraFlashMode.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            List<String> permissionList = Arrays.asList(permissions);
            boolean permissionsGranted = true;

            if(!permissionList.contains(Manifest.permission.CAMERA) || grantResults[permissionList.indexOf(Manifest.permission.CAMERA)] != PackageManager.PERMISSION_GRANTED){
                permissionsGranted &= false;
            }

            if(!permissionList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) || grantResults[permissionList.indexOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)] != PackageManager.PERMISSION_GRANTED){
                permissionsGranted &= false;
            }

            if(!permissionList.contains(Manifest.permission.READ_EXTERNAL_STORAGE) || grantResults[permissionList.indexOf(Manifest.permission.READ_EXTERNAL_STORAGE)] != PackageManager.PERMISSION_GRANTED){
                permissionsGranted &= false;
            }

            if(permissionsGranted){
                startCamera();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        mTextureView = view.findViewById(R.id.texture_camera_preview);
        btnCameraFlashMode = view.findViewById(R.id.btn_camera_flash_mode);
        btnCameraFlashMode.setOnClickListener(this);
        btnCameraFacing = view.findViewById(R.id.btn_camera_facing);
        btnCameraFacing.setOnClickListener(this);
        view.findViewById(R.id.btn_camera_capture).setOnClickListener(this);

        mFile = new File(getActivity().getExternalFilesDir(null), TEMP_IMAGE_FILE_NAME);

        // To check if device support both front and rear camera
        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = mCameraManager.getCameraIdList();

            // If the device has only one camera, then check if the camera's lens is facing front or back
            if(cameraList.length > 0 && cameraList.length == 1){
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraList[0]);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    currentCameraFacingMode = CameraFacingMode.FRONT;
                } else if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK){
                    currentCameraFacingMode = CameraFacingMode.REAR;
                } else{
                    currentCameraFacingMode = null;
                }

                btnCameraFacing.setVisibility(View.GONE);
            } else {
                currentCameraFacingMode = CameraFacingMode.REAR;
                btnCameraFacing.setVisibility(View.VISIBLE);
                btnCameraFacing.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_camera_front));
            }

            if(currentCameraFacingMode != null)
                setUpCameraId(currentCameraFacingMode);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return view;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onStart() {
        super.onStart();
        if(isMenuVisible())
            startCamera();
    }

    @Override
    public void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.btn_camera_capture) {
            captureStillPicture();
        } else if (i == R.id.btn_camera_flash_mode) {
            if (++currentSelectedFlashMode >= FlashMode.values().length) {
                currentSelectedFlashMode = 0;
            }
            switch (FlashMode.values()[currentSelectedFlashMode]) {
                case AUTO: {
                    btnCameraFlashMode.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_flash_auto));
                    break;
                }
                case ALWAYS: {
                    btnCameraFlashMode.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_flash_always));
                    break;
                }
                case OFF: {
                    btnCameraFlashMode.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_flash_off));
                    break;
                }
            }
            setCameraFlashMode(mPreviewRequestBuilder);

            // Auto brightness
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());

            // Reset capture session
            mPreviewRequest = mPreviewRequestBuilder.build();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                        mCaptureCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else if (i == R.id.btn_camera_facing) {
            currentCameraFacingMode = (CameraFacingMode.FRONT == currentCameraFacingMode) ? CameraFacingMode.REAR : CameraFacingMode.FRONT;
            switch (currentCameraFacingMode) {
                case FRONT: {
                    btnCameraFacing.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_camera_rear));
                    break;
                }
                case REAR:
                default: {
                    btnCameraFacing.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_action_camera_front));
                }
            }
            setUpCameraId(currentCameraFacingMode);
            stopCamera();
            startCamera();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !cameraPreviewing) {
            startCamera();

            if(cameraFirstTimePreviewingFlag) {
                cameraFirstTimePreviewingFlag = false;
                new AsyncTask<Void, Void, Void>(){

                    @Override
                    protected Void doInBackground(Void... voids) {
                        while(!cameraPreviewing);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopCamera();
                                startCamera();
                            }
                        });

                        return null;
                    }
                }.execute();
            }
        }
    }

    /***************************** CUSTOM CLASSES *****************************/
    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}