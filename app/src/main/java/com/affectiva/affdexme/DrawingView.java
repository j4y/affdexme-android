package com.affectiva.affdexme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.affectiva.android.affdex.sdk.detector.Face;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class contains a SurfaceView and its own thread that draws to it.
 * It is used to display the facial tracking dots over a user's face.
 */
public class DrawingView extends SurfaceView implements SurfaceHolder.Callback {

    private static String LOG_TAG = "AffdexMe";
    //Static references to bitmaps drawn on the surface
    private static Bitmap appearanceMarkerBitmap_genderMale;
    private static Bitmap appearanceMarkerBitmap_genderFemale;
    private static Bitmap appearanceMarkerBitmap_glassesOn;
    private static Map<String, Bitmap> emojiMarkerBitmapToEmojiTypeMap;
    final float MARGIN = 2;
    //Class variables of DrawingView class
    private SurfaceHolder surfaceHolder;
    private DrawingThread drawingThread; //DrawingThread object
    private DrawingViewConfig drawingViewConfig;

    //three constructors required of any custom view
    public DrawingView(Context context) {
        super(context);
        initView(null);
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(attrs);
    }

    private static int getDrawable(@NonNull Context context, @NonNull String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }

    void initView(AttributeSet attrs) {
        surfaceHolder = getHolder(); //The SurfaceHolder object will be used by the thread to request canvas to draw on SurfaceView
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT); //set to Transparent so this surfaceView does not obscure the one it is overlaying (the one displaying the camera).
        surfaceHolder.addCallback(this); //become a Listener to the three events below that SurfaceView generates

        drawingViewConfig = new DrawingViewConfig();

        //default values
        int textSize = 15;

        Paint measurementTextPaint = new Paint();
        measurementTextPaint.setStyle(Paint.Style.FILL);
        measurementTextPaint.setTextAlign(Paint.Align.CENTER);

        Paint dropShadow = new Paint();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setStyle(Paint.Style.STROKE);
        dropShadow.setTextAlign(Paint.Align.CENTER);

        //load and parse XML attributes
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.drawing_view_attributes, 0, 0);
            measurementTextPaint.setColor(a.getColor(R.styleable.drawing_view_attributes_measurements_color, Color.WHITE));
            dropShadow.setColor(a.getColor(R.styleable.drawing_view_attributes_measurements_text_border_color, Color.BLACK));
            dropShadow.setStrokeWidth(a.getInteger(R.styleable.drawing_view_attributes_measurements_text_border_thickness, 5));
            textSize = a.getDimensionPixelSize(R.styleable.drawing_view_attributes_measurements_text_size, textSize);
            measurementTextPaint.setTextSize(textSize);
            dropShadow.setTextSize(textSize);
            a.recycle();
        }

        drawingViewConfig.setMeasurementMetricConfigs(measurementTextPaint, dropShadow);

        drawingThread = new DrawingThread(surfaceHolder, drawingViewConfig);

        //statically load the emoji bitmaps on-demand and cache
        emojiMarkerBitmapToEmojiTypeMap = new HashMap<>();
    }

    public void setTypeface(Typeface face) {
        drawingViewConfig.textPaint.setTypeface(face);
        drawingViewConfig.textBorderPaint.setTypeface(face);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (drawingThread.isStopped()) {
            drawingThread = new DrawingThread(surfaceHolder, drawingViewConfig);
        }
        drawingThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //command thread to stop, and wait until it stops
        boolean retry = true;
        drawingThread.stopThread();
        while (retry) {
            try {
                drawingThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
        cleanup();
    }

    public boolean isDimensionsNeeded() {
        return drawingViewConfig.isDimensionsNeeded;
    }

    public void invalidateDimensions() {
        drawingViewConfig.isDimensionsNeeded = true;
    }

    public void updateViewDimensions(int surfaceViewWidth, int surfaceViewHeight, int imageWidth, int imageHeight) {
        try {
            drawingViewConfig.updateViewDimensions(surfaceViewWidth, surfaceViewHeight, imageWidth, imageHeight);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public void setThickness(int t) {
        drawingViewConfig.setDrawThickness(t);
        try {
            drawingThread.setThickness(t);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public boolean getDrawPointsEnabled() {
        return drawingViewConfig.isDrawPointsEnabled;
    }

    public void setDrawPointsEnabled(boolean b) {
        drawingViewConfig.isDrawPointsEnabled = b;
    }

    public boolean getDrawAppearanceMarkersEnabled() {
        return drawingViewConfig.isDrawAppearanceMarkersEnabled;
    }

    public void setDrawAppearanceMarkersEnabled(boolean b) {
        drawingViewConfig.isDrawAppearanceMarkersEnabled = b;
    }

    public boolean getAlwaysShowDominantMarkersEnabled() {
        return drawingViewConfig.isAlwaysShowDominantMarkersEnabled;
    }

    public void getAlwaysShowDominantMarkersEnabled(boolean b) {
        drawingViewConfig.isAlwaysShowDominantMarkersEnabled = b;
    }

    public void updatePoints(List<Face> faces, boolean isPointsMirrored) {
        drawingThread.updatePoints(faces, isPointsMirrored);
    }

    public void invalidatePoints() {
        drawingThread.invalidatePoints();
    }

    /**
     * To be called when this view element is potentially being destroyed
     * I.E. when the Activity's onPause() gets called.
     */
    public void cleanup() {
        if (emojiMarkerBitmapToEmojiTypeMap != null) {
            for (Bitmap bitmap : emojiMarkerBitmapToEmojiTypeMap.values()) {
                bitmap.recycle();
            }
            emojiMarkerBitmapToEmojiTypeMap.clear();
        }

        if (appearanceMarkerBitmap_genderMale != null) {
            appearanceMarkerBitmap_genderMale.recycle();
        }
        if (appearanceMarkerBitmap_genderFemale != null) {
            appearanceMarkerBitmap_genderFemale.recycle();
        }
        if (appearanceMarkerBitmap_glassesOn != null) {
            appearanceMarkerBitmap_glassesOn.recycle();
        }
    }

    class FacesSharer {
        boolean isPointsMirrored;
        List<Face> facesToDraw;

        public FacesSharer() {
            isPointsMirrored = false;
            facesToDraw = new ArrayList<>();
        }
    }

    //Inner Thread class
    class DrawingThread extends Thread {
        private final FacesSharer sharer;
        private final SurfaceHolder mSurfaceHolder;
        private Paint circlePaint;
        private Paint boxPaint;
        private volatile boolean stopFlag = false; //boolean to indicate when thread has been told to stop
        private DrawingViewConfig config;

        public DrawingThread(SurfaceHolder surfaceHolder, DrawingViewConfig con) {
            mSurfaceHolder = surfaceHolder;

            //statically load the Appearance marker bitmaps so they only have to load once
            appearanceMarkerBitmap_genderMale = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gender_male_white_22x48dp);
            appearanceMarkerBitmap_genderFemale = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gender_female_white_22x48dp);
            appearanceMarkerBitmap_glassesOn = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.glasses_outline_48x17dp);

            circlePaint = new Paint();
            circlePaint.setColor(Color.WHITE);
            boxPaint = new Paint();
            boxPaint.setColor(Color.WHITE);
            boxPaint.setStyle(Paint.Style.STROKE);

            config = con;
            sharer = new FacesSharer();

            setThickness(config.drawThickness);
        }

        void setValenceOfBoundingBox(float valence) {
            //prepare the color of the bounding box using the valence score. Red for -100, White for 0, and Green for +100, with linear interpolation in between.
            if (valence > 0) {
                float colorScore = ((100f - valence) / 100f) * 255;
                boxPaint.setColor(Color.rgb((int) colorScore, 255, (int) colorScore));
            } else {
                float colorScore = ((100f + valence) / 100f) * 255;
                boxPaint.setColor(Color.rgb(255, (int) colorScore, (int) colorScore));
            }
        }

        public void stopThread() {
            stopFlag = true;
        }

        public boolean isStopped() {
            return stopFlag;
        }

        //Updates thread with latest faces returned by the onImageResults() event.
        public void updatePoints(List<Face> faces, boolean isPointsMirrored) {
            synchronized (sharer) {
                sharer.facesToDraw.clear();
                if (faces != null) {
                    sharer.facesToDraw.addAll(faces);
                }
                sharer.isPointsMirrored = isPointsMirrored;
            }
        }

        void setThickness(int thickness) {
            boxPaint.setStrokeWidth(thickness);
        }

        //Inform thread face detection has stopped, so pending faces are no longer valid.
        public void invalidatePoints() {
            synchronized (sharer) {
                sharer.facesToDraw.clear();
            }
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            while (!stopFlag) {

                /**
                 * We use SurfaceHolder.lockCanvas() to get the canvas that draws to the SurfaceView.
                 * After we are done drawing, we let go of the canvas using SurfaceHolder.unlockCanvasAndPost()
                 * **/
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas();

                    if (c != null) {
                        synchronized (mSurfaceHolder) {
                            c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //clear previous dots
                            draw(c);
                        }
                    }
                } finally {
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }

            config = null; //nullify object to avoid memory leak
        }

        void draw(Canvas c) {
            Face nextFaceToDraw;
            boolean mirrorPoints;
            boolean multiFaceMode;
            int index = 0;

            synchronized (sharer) {
                mirrorPoints = sharer.isPointsMirrored;
                multiFaceMode = sharer.facesToDraw.size() > 1;

                if (sharer.facesToDraw.isEmpty()) {
                    nextFaceToDraw = null;
                } else {
                    nextFaceToDraw = sharer.facesToDraw.get(index);
                    index++;
                }
            }

            while (nextFaceToDraw != null) {

                drawFaceAttributes(c, nextFaceToDraw, mirrorPoints, multiFaceMode);

                synchronized (sharer) {
                    mirrorPoints = sharer.isPointsMirrored;

                    if (index < sharer.facesToDraw.size()) {
                        nextFaceToDraw = sharer.facesToDraw.get(index);
                        index++;
                    } else {
                        nextFaceToDraw = null;
                    }
                }
            }
        }

        private void drawFaceAttributes(Canvas c, Face face, boolean mirrorPoints, boolean isMultiFaceMode) {
            //Coordinates around which to draw bounding box.
            //Default to an 'inverted' box, where the absolute max and min values of the surface view are inside-out
            Rect boundingRect = new Rect(config.surfaceViewWidth, config.surfaceViewHeight, 0, 0);

            for (PointF point : face.getFacePoints()) {
                //transform from the camera coordinates to our screen coordinates
                //The camera preview is displayed as a mirror, so X pts have to be mirrored back.
                float x;
                if (mirrorPoints) {
                    x = (config.imageWidth - point.x) * config.screenToImageRatio;
                } else {
                    x = (point.x) * config.screenToImageRatio;
                }
                float y = (point.y) * config.screenToImageRatio;

                //For some reason I needed to add each point twice to make sure that all the
                //points get properly registered in the bounding box.
                boundingRect.union(Math.round(x), Math.round(y));
                boundingRect.union(Math.round(x), Math.round(y));

                //Draw facial tracking dots.
                if (config.isDrawPointsEnabled) {
                    c.drawCircle(x, y, config.drawThickness, circlePaint);
                }
            }

            //Draw the bounding box.
            if (config.isDrawPointsEnabled) {
                drawBoundingBox(c, face, boundingRect);
            }

            //Draw the Appearance markers (gender / glasses)
            if (config.isDrawAppearanceMarkersEnabled) {
                drawAppearanceMarkers(c, face, boundingRect);
            }

            //Only draw the emotion or emoji on the bounding box if we are in multiface mode, or the setting is set to always show the dominant metrics.
            if (isMultiFaceMode || config.isAlwaysShowDominantMarkersEnabled) {
                drawDominantEmoji(c, face, boundingRect);
                drawDominantEmotion(c, face, boundingRect);
            }
        }

        private void drawBoundingBox(Canvas c, Face f, Rect boundingBox) {
            setValenceOfBoundingBox(f.emotions.getValence());
            c.drawRect(boundingBox.left,
                    boundingBox.top,
                    boundingBox.right,
                    boundingBox.bottom,
                    boxPaint);
        }

        private void drawAppearanceMarkers(Canvas c, Face f, Rect boundingBox) {
            float markerPoxX = boundingBox.left - MARGIN;
            float markerPosY = boundingBox.top;  //start aligned to the top of the box

            //GLASSES
            if (Face.GLASSES.YES.equals(f.appearance.getGlasses())) {
                if (!appearanceMarkerBitmap_glassesOn.isRecycled()) {
                    c.drawBitmap(appearanceMarkerBitmap_glassesOn, markerPoxX - appearanceMarkerBitmap_glassesOn.getWidth(), markerPosY, boxPaint);
                    markerPosY += appearanceMarkerBitmap_glassesOn.getHeight() + MARGIN;
                }
            }

            //GENDER
            if (Face.GENDER.MALE.equals(f.appearance.getGender())) {
                if (!appearanceMarkerBitmap_genderMale.isRecycled()) {
                    c.drawBitmap(appearanceMarkerBitmap_genderMale, markerPoxX - appearanceMarkerBitmap_genderMale.getWidth(), markerPosY, boxPaint);
                }
            } else if (Face.GENDER.FEMALE.equals(f.appearance.getGender())) {
                if (!appearanceMarkerBitmap_genderFemale.isRecycled()) {
                    c.drawBitmap(appearanceMarkerBitmap_genderFemale, markerPoxX - appearanceMarkerBitmap_genderFemale.getWidth(), markerPosY, boxPaint);
                }
            }
        }

        private void drawDominantEmoji(Canvas c, Face f, Rect boundingBox) {
            drawEmojiFromCache(c, f.emojis.getDominantEmoji().name(), boundingBox.right + MARGIN, boundingBox.top);
        }

        private void drawDominantEmotion(Canvas c, Face f, Rect boundingBox) {
            Pair<String, Float> dominantMetric = findDominantEmotion(f);

            if (dominantMetric.first.isEmpty()) {
                return;
            }

            String emotionTextToDraw = dominantMetric.first + ": " + Math.round(dominantMetric.second) + "%";
            Rect textBounds = new Rect();
            config.textPaint.getTextBounds(emotionTextToDraw, 0, emotionTextToDraw.length(), textBounds);

            c.drawText(emotionTextToDraw, boundingBox.exactCenterX(), boundingBox.bottom + MARGIN + textBounds.height(), config.textBorderPaint);
            c.drawText(emotionTextToDraw, boundingBox.exactCenterX(), boundingBox.bottom + MARGIN + textBounds.height(), config.textPaint);
        }

        private Pair<String, Float> findDominantEmotion(Face f) {
            String dominantMetricName = "";
            Float dominantMetricValue = Float.MIN_VALUE;

            if (f.emotions.getAnger() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.ANGER);
                dominantMetricValue = f.emotions.getAnger();
            }
            if (f.emotions.getContempt() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.CONTEMPT);
                dominantMetricValue = f.emotions.getContempt();
            }
            if (f.emotions.getDisgust() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.DISGUST);
                dominantMetricValue = f.emotions.getDisgust();
            }
            if (f.emotions.getEngagement() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.ENGAGEMENT);
                dominantMetricValue = f.emotions.getEngagement();
            }
            if (f.emotions.getFear() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.FEAR);
                dominantMetricValue = f.emotions.getFear();
            }
            if (f.emotions.getJoy() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.JOY);
                dominantMetricValue = f.emotions.getJoy();
            }
            if (f.emotions.getSadness() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.SADNESS);
                dominantMetricValue = f.emotions.getSadness();
            }
            if (f.emotions.getSurprise() > dominantMetricValue) {
                dominantMetricName = MetricsManager.getCapitalizedName(MetricsManager.Emotions.SURPRISE);
                dominantMetricValue = f.emotions.getSurprise();
            }

            return new Pair<>(dominantMetricName, dominantMetricValue);
        }

        void drawEmojiFromCache(Canvas c, String emojiName, float markerPosX, float markerPosY) {
            Bitmap emojiBitmap;

            try {
                emojiBitmap = getEmojiBitmapByName(emojiName);
            } catch (FileNotFoundException e) {
                Log.e(LOG_TAG, "Error, file not found!", e);
                return;
            }

            c.drawBitmap(emojiBitmap, markerPosX, markerPosY, boxPaint);
        }

        Bitmap getEmojiBitmapByName(String emojiName) throws FileNotFoundException {
            String emojiResourceName = emojiName.trim().replace(' ', '_').toLowerCase(Locale.US).concat("_emoji");
            String emojiFileName = emojiResourceName + ".png";

            //Try to get the emoji from the cache
            Bitmap desiredEmojiBitmap = emojiMarkerBitmapToEmojiTypeMap.get(emojiFileName);

            if (desiredEmojiBitmap != null) {
                //emoji bitmap found in the cache
                return desiredEmojiBitmap;
            }

            //Cache miss, try and load the bitmap from disk
            desiredEmojiBitmap = ImageHelper.loadBitmapFromInternalStorage(getContext(), emojiFileName);

            if (desiredEmojiBitmap != null) {
                //emoji bitmap found in the app storage


                //Bitmap loaded, add to cache for subsequent use.
                emojiMarkerBitmapToEmojiTypeMap.put(emojiFileName, desiredEmojiBitmap);

                return desiredEmojiBitmap;
            }

            Log.d(LOG_TAG, "Emoji not found on disk: " + emojiFileName);

            //Still unable to find the file, try to locate the emoji resource
            final int resourceId = getDrawable(getContext(), emojiFileName);

            if (resourceId == 0) {
                //unrecognised emoji file name
                throw new FileNotFoundException("Resource not found for file named: " + emojiFileName);
            }

            desiredEmojiBitmap = BitmapFactory.decodeResource(getResources(), resourceId);

            if (desiredEmojiBitmap == null) {
                //still unable to load the resource from the file
                throw new FileNotFoundException("Resource id [" + resourceId + "] but could not load bitmap: " + emojiFileName);
            }

            //Bitmap loaded, add to cache for subsequent use.
            emojiMarkerBitmapToEmojiTypeMap.put(emojiFileName, desiredEmojiBitmap);

            return desiredEmojiBitmap;
        }
    }

    class DrawingViewConfig {
        private int imageWidth = 1;
        private int surfaceViewWidth = 0;
        private int surfaceViewHeight = 0;
        private float screenToImageRatio = 0;
        private int drawThickness = 0;
        private boolean isDrawPointsEnabled = true; //by default, have the drawing thread draw tracking dots
        private boolean isDimensionsNeeded = true;
        private boolean isDrawAppearanceMarkersEnabled = false; //by default, do not draw the gender and glasses markers
        private boolean isAlwaysShowDominantMarkersEnabled = false; //by default, do not draw emoji

        private Paint textPaint;
        private Paint textBorderPaint;

        public void setMeasurementMetricConfigs(Paint textPaint, Paint dropShadowPaint) {
            this.textPaint = textPaint;
            this.textBorderPaint = dropShadowPaint;
        }

        public void updateViewDimensions(int surfaceViewWidth, int surfaceViewHeight, int imageWidth, int imageHeight) {
            if (surfaceViewWidth <= 0 || surfaceViewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0) {
                throw new IllegalArgumentException("All dimensions submitted to updateViewDimensions() must be positive");
            }
            this.imageWidth = imageWidth;
            this.surfaceViewWidth = surfaceViewWidth;
            this.surfaceViewHeight = surfaceViewHeight;
            screenToImageRatio = (float) surfaceViewWidth / imageWidth;
            isDimensionsNeeded = false;
        }

        public void setDrawThickness(int t) {

            if (t <= 0) {
                throw new IllegalArgumentException("Thickness must be positive.");
            }

            drawThickness = t;
        }
    }
}