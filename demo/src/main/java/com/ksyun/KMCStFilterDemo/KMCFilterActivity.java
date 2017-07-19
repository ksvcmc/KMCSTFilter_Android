package com.ksyun.KMCStFilterDemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.KMCStFilterDemo.utils.ApiHttpUrlConnection;
import com.ksyun.KMCStFilterDemo.widget.RecyclerViewAdatper;
import com.ksyun.KMCStFilterDemo.widget.SpacesItemDecoration;
import com.ksyun.media.kmcfilter.Constants;
import com.ksyun.media.kmcfilter.KMCArMaterial;
import com.ksyun.media.kmcfilter.KMCAuthManager;
import com.ksyun.media.kmcfilter.KMCFilter;
import com.ksyun.media.kmcfilter.KMCFilterManager;
import com.ksyun.media.streamer.capture.camera.CameraTouchHelper;
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyDenoiseFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsLogReport;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sujia on 2017/5/25.
 */

public class KMCFilterActivity extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "KMCFilterActivity";

    private GLSurfaceView mCameraPreviewView;
    //private TextureView mCameraPreviewView;
    private CameraHintView mCameraHintView;

    private View mSwitchCameraView;
    private View mActionStickerTypeView;
    private View mFaceStickerTypeView;
    private ImageView mActionStikcerImg;
    private ImageView mFaceStikcerImg;
    private TextView mActionStikcerTxt;;
    private TextView mFaceStikcerTxt;

    private ButtonObserver mObserverButton;

    private KSYStreamer mStreamer;
    private Handler mMainHandler;

    private KMCArMaterial mMaterial = null;
    private List<MaterialInfoItem> mFaceMaterialList = null;
    private List<MaterialInfoItem> mActionMaterialList = null;
    private List<MaterialInfoItem> mCurrentMaterialList = null;

    private RecyclerView mRecyclerView = null;
    private RecyclerViewAdatper mRecyclerViewAdatper = null;

    private final static int MSG_LOADTHUMB = 0;
    private final static int MSG_DOWNLOADSUCCESS = 1;
    private final static int MSG_STARTDOWNLOAD = 2;
    private final static int MSG_ENABLEPUSH = 3;
    private final static int MSG_GETLISTSIZE = 4;

    private int mListCount = 1;
    private KMCFilter mImgStickerFilter;

    private static int mMateriallist1SelectIndex = -1;
    private static int mMateriallist2SelectIndex = -1;
    private static int mCurrentMaterialIndex = -1;
    private static int mCurrentMaterialType = 0;

    private boolean mPause = false;
    private boolean mIsFirstFetchMaterialList = true;
    private Bitmap mNullBitmap = null;

    private final static int PERMISSION_REQUEST_CAMERA_AUDIOREC = 1;

    public final static int REPORT_APPDATA_MESSAGE = 100;
    public static String REPORT_DATA="reportdata";

    //auth
    //the uri of your appServer
    private boolean authorized = false;
    public static String AUTH_SERVER_URI = "http://kmc.api.ksyun.com";
    private HttpRequestTask mAuthTask;
    private HttpRequestTask.HttpResponseListener mAuthResponse;

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MSG_GETLISTSIZE:
                    initMaterialTabTypeView();
                    break;
                case MSG_LOADTHUMB:
                    mRecyclerViewAdatper.setItemState(msg.arg2,
                            RecyclerViewAdatper.STATE_DOWNLOADTHUMBNAIL);
                    updateListView(msg.arg2);
                    mRecyclerViewAdatper.notifyDataSetChanged();

                    break;
                case MSG_DOWNLOADSUCCESS:
                    mRecyclerViewAdatper.setItemState(msg.arg1,
                            RecyclerViewAdatper.STATE_DOWNLOADED);
                    updateListView(msg.arg1);
                    mRecyclerViewAdatper.notifyDataSetChanged();
                    updateCoolDownView();
                    break;
                case MSG_STARTDOWNLOAD:
                    mRecyclerViewAdatper.setItemState(msg.arg1,
                            RecyclerViewAdatper.STATE_DOWNLOADING);
                    updateListView(msg.arg1);
                    mRecyclerViewAdatper.notifyDataSetChanged();
                    break;
                case REPORT_APPDATA_MESSAGE:
                    Bundle bundle = msg.getData();
                    String jsonStr = bundle.getString(REPORT_DATA);
                    break;
                case MSG_ENABLEPUSH:
                    ImageView v = (ImageView) msg.obj;
                    v.setEnabled(true);
                    break;
                default:
                    Log.e(TAG, "Invalid message");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.kmcfilter_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraHintView = (CameraHintView) findViewById(R.id.kmc_camera_hint);
        mCameraPreviewView = (GLSurfaceView) findViewById(R.id.kmc_camera_preview);
        //mCameraPreviewView = (TextureView) findViewById(R.id.camera_preview);

        mObserverButton = new ButtonObserver();
        mSwitchCameraView = findViewById(R.id.kmc_switch_cam);
        mSwitchCameraView.setOnClickListener(mObserverButton);
        mActionStickerTypeView = findViewById(R.id.action_sticker_layout);
        mActionStickerTypeView.setOnClickListener(mObserverButton);
        mFaceStickerTypeView = findViewById(R.id.face_sticker_layout);
        mFaceStickerTypeView.setOnClickListener(mObserverButton);

        mFaceStikcerImg = (ImageView)findViewById(R.id.face_sticker_img);
        mFaceStikcerTxt = (TextView) findViewById(R.id.face_sticker_txt);
        mActionStikcerImg = (ImageView)findViewById(R.id.action_sticker_img);
        mActionStikcerTxt = (TextView) findViewById(R.id.action_sticker_txt);

        mMainHandler = new Handler();
        mRecyclerView = (RecyclerView) findViewById(R.id.kmc_recycler_view);
        //创建默认的线性LayoutManager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(10));
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        mRecyclerView.setHasFixedSize(true);

        //set para to  ksystreamer
        mStreamer = new KSYStreamer(this);
        mStreamer.setPreviewFps(15);
        mStreamer.setTargetFps(15);
        mStreamer.setVideoKBitrate(800);
        mStreamer.setAudioKBitrate(48);
        mStreamer.setPreviewResolution(StreamerConstants.VIDEO_RESOLUTION_720P);
        mStreamer.setTargetResolution(StreamerConstants.VIDEO_RESOLUTION_720P);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mStreamer.setDisplayPreview(mCameraPreviewView);
        mStreamer.setEnableStreamStatModule(true);
        mStreamer.enableDebugLog(true);
        mStreamer.setFrontCameraMirror(true);
        mStreamer.setMuteAudio(true);
        mStreamer.setEnableAudioPreview(false);
        mStreamer.setOnInfoListener(mOnInfoListener);
        mStreamer.setOnErrorListener(mOnErrorListener);
        mStreamer.setOnLogEventListener(mOnLogEventListener);

        mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE);
        mStreamer.setEnableImgBufBeauty(true);
        mStreamer.getImgTexFilterMgt().setOnErrorListener(new ImgTexFilterBase.OnErrorListener() {
            @Override
            public void onError(ImgTexFilterBase filter, int errno) {
                Toast.makeText(KMCFilterActivity.this, "当前机型不支持该滤镜",
                        Toast.LENGTH_SHORT).show();
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            }
        });

        // touch focus and zoom support
        CameraTouchHelper cameraTouchHelper = new CameraTouchHelper();
        cameraTouchHelper.setCameraCapture(mStreamer.getCameraCapture());
        mCameraPreviewView.setOnTouchListener(cameraTouchHelper);
        // set CameraHintView to show focus rect and zoom ratio
        cameraTouchHelper.setCameraHintView(mCameraHintView);

        doAuth();

        //view state
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraPreviewWithPermCheck();
        mPause = false;
        mStreamer.onResume();
        this.initMaterialsRecyclerView();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPause = true;
        mStreamer.onPause();
        mStreamer.stopCameraPreview();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //清空素材缓存
        mCurrentMaterialIndex = -1;
        mMateriallist1SelectIndex = -1;
        mMateriallist2SelectIndex = -1;
        mCurrentMaterialType = 0;
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        mStreamer.release();

        KMCAuthManager.getInstance().removeAuthResultListener(mCheckAuthResultListener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackoffClick();
                break;
            default:
                break;
        }
        return true;
    }

    // Example to handle camera related operation
    private void setCameraAntiBanding50Hz() {
        Camera.Parameters parameters = mStreamer.getCameraCapture().getCameraParameters();
        if (parameters != null) {
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
            mStreamer.getCameraCapture().setCameraParameters(parameters);
        }
    }

    private KSYStreamer.OnInfoListener mOnInfoListener = new KSYStreamer.OnInfoListener() {
        @Override
        public void onInfo(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                    setCameraAntiBanding50Hz();
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                    break;
                case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                    Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                    Toast.makeText(KMCFilterActivity.this, "Network not good!",
                            Toast.LENGTH_SHORT).show();
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_RAISE:
                    Log.d(TAG, "BW raise to " + msg1 / 1000 + "kbps");
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_DROP:
                    Log.d(TAG, "BW drop to " + msg1 / 1000 + "kpbs");
                    break;
                default:
                    Log.d(TAG, "OnInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                    break;
            }
        }
    };

    private KSYStreamer.OnErrorListener mOnErrorListener = new KSYStreamer.OnErrorListener() {
        @Override
        public void onError(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_DNS_PARSE_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_PUBLISH_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_BREAKED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC:
                    Log.d(TAG, "KSY_STREAMER_ERROR_AV_ASYNC " + msg1 + "ms");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_SERVER_DIED");
                    break;
                default:
                    Log.d(TAG, "what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
                    break;
            }
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    mStreamer.stopCameraPreview();
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startCameraPreviewWithPermCheck();
                        }
                    }, 5000);
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    break;
                default:
                    break;
            }
        }
    };

    private StatsLogReport.OnLogEventListener mOnLogEventListener =
            new StatsLogReport.OnLogEventListener() {
                @Override
                public void onLogEvent(StringBuilder singleLogContent) {
                    Log.i(TAG, "***onLogEvent : " + singleLogContent.toString());
                }
            };


    private void onSwitchCamera() {
        mStreamer.switchCamera();
        mCameraHintView.hideAll();
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(KMCFilterActivity.this).setCancelable(true)
                .setTitle("结束?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        KMCFilterActivity.this.finish();
                    }
                }).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA_AUDIOREC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStreamer.startCameraPreview();
                } else {
                    Log.e(TAG, "No CAMERA or AudioRecord permission");
                    Toast.makeText(this, "No CAMERA or AudioRecord permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void saveSelectedIndex(int position) {
        if (mCurrentMaterialList == mFaceMaterialList) {
            mMateriallist1SelectIndex = position;
            mMateriallist2SelectIndex = -1;
        } else {
            mMateriallist1SelectIndex = -1;
            mMateriallist2SelectIndex = position;
        }
        mCurrentMaterialIndex = position;
    }


    private void updateListView(int position) {
        mRecyclerViewAdatper.updateItemView(position);
    }

    private void updateCoolDownView() {
        mRecyclerViewAdatper.notifyDataSetChanged();
    }

    /**
     * 单独下载贴纸素材的回调对象
     */
    private KMCFilterManager.DownloadMaterialListener mDownloadListener = new KMCFilterManager.DownloadMaterialListener() {
        /**
         * 下载成功
         * @param material 下载成功的素材
         */
        @Override
        public void onSuccess(KMCArMaterial material) {
            int position = 0;

            for (int j = 0; j < mCurrentMaterialList.size(); j++) {
                String stickerid = mCurrentMaterialList.get(j).material.id;
                if (stickerid != null && stickerid.equals(material.id)) {
                    position = j;
                    mCurrentMaterialList.get(j).setHasDownload(true);
                }
            }
            Log.d(TAG, "download success for position " + position);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOADSUCCESS, position, 0));

        }

        /**
         * 下载失败
         * @param material 下载失败的素材
         * @param code 失败原因的错误代码
         * @param message 失败原因的解释
         */
        @Override
        public void onFailure(KMCArMaterial material, int code, String message) {
            if (code == Constants.AUTH_EXPIRED) {
                makeToast("鉴权信息过期，请重新鉴权!");
                doAuth();
            }
            mMaterial = null;

        }

        /**
         * 下载过程中的进度回调
         * @param material  正在下载素材
         * @param progress 当前下载的进度
         * @param size 已经下载素材的大小, 单位byte
         */
        @Override
        public void onProgress(KMCArMaterial material, float progress, int size) {
        }
    };

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.kmc_switch_cam:
                    onSwitchCamera();
                    break;
                case R.id.action_sticker_layout:
                    mActionStikcerImg.setImageResource(R.drawable.action_sticker_on);
                    mFaceStikcerImg.setImageResource(R.drawable.face_sticker_off);
                    mActionStikcerTxt.setTextColor(getResources().getColor(R.color.tab_text_on));
                    mFaceStikcerTxt.setTextColor(getResources().getColor(R.color.tab_text_off));
                    updateTabView(1);
                    break;
                case R.id.face_sticker_layout:
                    mActionStikcerImg.setImageResource(R.drawable.action_sticker_off);
                    mFaceStikcerImg.setImageResource(R.drawable.face_sticker_on);
                    mActionStikcerTxt.setTextColor(getResources().getColor(R.color.tab_text_off));
                    mFaceStikcerTxt.setTextColor(getResources().getColor(R.color.tab_text_on));
                    updateTabView(0);
                    break;
                default:
                    break;
            }
        }
    }


    private void doAuth() {
        String token = "98e1692a90e3c404201391208e0346f2";
        KMCAuthManager.getInstance().authorize(getApplicationContext(),
                token, mCheckAuthResultListener);
    }

    private KMCAuthManager.AuthResultListener mCheckAuthResultListener = new KMCAuthManager
            .AuthResultListener() {
        @Override
        public void onSuccess() {
            authorized = true;

            KMCAuthManager.getInstance().removeAuthResultListener(mCheckAuthResultListener);
            makeToast("鉴权成功，可以使用魔方贴纸功能");
        }

        @Override
        public void onFailure(int errCode) {
            KMCAuthManager.getInstance().removeAuthResultListener(mCheckAuthResultListener);
            makeToast("鉴权失败! 错误码: " + errCode);
        }
    };

    private void makeToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(KMCFilterActivity.this, str, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    private void startCameraPreviewWithPermCheck() {
        int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int audioPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (cameraPerm != PackageManager.PERMISSION_GRANTED ||
                audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No CAMERA or AudioRecord permission, please check");
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_CAMERA_AUDIOREC);
            }
        } else {
            mStreamer.startCameraPreview();
        }
    }

    /**
     * 获取贴纸列表, 从AR服务器获取到当前热门/或者符合主播属性的贴纸列表
     */
    protected void startGetMaterialList() {
        if (mActionMaterialList != null && mFaceMaterialList != null &&
                mActionMaterialList.size() > 1 && mFaceMaterialList.size() > 1) {
            return;
        }
        mFaceMaterialList = new ArrayList<MaterialInfoItem>();
        mActionMaterialList = new ArrayList<MaterialInfoItem>();

        if (mNullBitmap == null) {
            mNullBitmap = getNullEffectBitmap();
        }
        MaterialInfoItem nullSticker = new MaterialInfoItem(new KMCArMaterial(), mNullBitmap);
        nullSticker.setHasDownload(true);
        mFaceMaterialList.add(nullSticker);
        mActionMaterialList.add(nullSticker);

        fetchMaterial("SE_LIST");
    }

    private Bitmap getNullEffectBitmap() {
        mNullBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.close);
        return mNullBitmap;
    }

    private void initMaterialTabTypeView() {
        updateTabView(mCurrentMaterialType);
    }

    private void updateTabView(int position) {
        if (!authorized) {
            return;
        }

        mCurrentMaterialType = position;
        showMaterialLists();
        if (position == 0) {
            boolean isSelectedMaterial = (mMateriallist2SelectIndex != -1);
            mCurrentMaterialList = mFaceMaterialList;

            mCurrentMaterialIndex = mMateriallist1SelectIndex;
        } else {
            boolean isSelectedMaterial = (mMateriallist1SelectIndex != -1);
            mCurrentMaterialList = mActionMaterialList;
            mCurrentMaterialIndex = mMateriallist2SelectIndex;
        }

        initMaterialsRecyclerView();
        if(position == 0){
            mRecyclerViewAdatper.setSelectIndex(mMateriallist1SelectIndex);
        }else {
            mRecyclerViewAdatper.setSelectIndex(mMateriallist2SelectIndex);
        }
    }

    private void initMaterialsRecyclerView() {
        if (mCurrentMaterialList == null) {
            Log.e(TAG, "The material list is null");
            return;
        }

        mRecyclerViewAdatper = new RecyclerViewAdatper(mCurrentMaterialList,
                getApplicationContext());
        mRecyclerViewAdatper.setRecyclerView(mRecyclerView);
        mRecyclerView.setAdapter(mRecyclerViewAdatper);

        if(mCurrentMaterialType == 0){
            mRecyclerViewAdatper.setSelectIndex(mMateriallist1SelectIndex);
        }else{
            mRecyclerViewAdatper.setSelectIndex(mMateriallist2SelectIndex);
        }


        mRecyclerViewAdatper.setOnItemClickListener(new RecyclerViewAdatper.OnRecyclerViewListener() {
            @Override
            public boolean onItemLongClick(int position) {
//                onRecyclerViewItemClick(position);
                return false;
            }

            @Override
            public void onItemClick(int position) {
                onRecyclerViewItemClick(position);
            }
        });
    }

    private void onRecyclerViewItemClick(int position) {
        MaterialInfoItem adinfo = mCurrentMaterialList.get(position);

        if (position == 0) {
            mMaterial = null;
            mRecyclerViewAdatper.setSelectIndex(position);
            saveSelectedIndex(position);
            mRecyclerViewAdatper.notifyDataSetChanged();
            if (mImgStickerFilter != null) {
                mImgStickerFilter.startShowingMaterial(null);
            }
            closeMaterialsShowLayer();
            return;
        }

        mMaterial = adinfo.material;

        if(KMCFilterManager.getInstance().isMaterialDownloaded(getApplicationContext(), adinfo.material)) {
            closeMaterialsShowLayer();

            if (mCurrentMaterialList == mActionMaterialList) {
                makeToast(adinfo.material.actionTip);
            }
            if(mImgStickerFilter == null) {
                mImgStickerFilter = new KMCFilter(getApplicationContext(),
                        mStreamer.getGLRender());
                mImgStickerFilter.startShowingMaterial(mMaterial);
                List<ImgFilterBase> groupFilter = new LinkedList<>();
                //you can choose the beauty filter here
                groupFilter.add(new ImgBeautyDenoiseFilter(mStreamer.getGLRender()));
                //add sticker filter
                groupFilter.add(mImgStickerFilter);
                mStreamer.getImgTexFilterMgt().setFilter(groupFilter);
            } else {
                mImgStickerFilter.startShowingMaterial(mMaterial);
            }

            if(mRecyclerViewAdatper.getItemState(position) != MSG_DOWNLOADSUCCESS){
                mHandler.sendMessage(mHandler.obtainMessage(MSG_DOWNLOADSUCCESS, position, 0));
            }

            saveSelectedIndex(position);
            mRecyclerViewAdatper.setSelectIndex(position);
        } else {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_STARTDOWNLOAD, position, 0));
            KMCFilterManager.getInstance().
                    downloadMaterial(getApplicationContext(), adinfo.material, mDownloadListener);
        }
        return;
    }

    protected void reportError(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(KMCFilterActivity.this, info, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMaterial(String groupID) {
        // 从AR服务器获取贴纸列表, 并保存其信息
        KMCFilterManager.getInstance().fetchMaterials(getApplicationContext(),
                groupID, new KMCFilterManager.FetchMaterialListener() {
                    @Override
                    public void onSuccess(List<KMCArMaterial> list) {
                        List<KMCArMaterial> adlist = list;
                        for (int i = 0; i < adlist.size(); i++) {
                            KMCArMaterial material = adlist.get(i);
                            MaterialInfoItem adinfo = new MaterialInfoItem(material, null);

                            if (KMCFilterManager.getInstance().isMaterialDownloaded(getApplicationContext(),
                                    material)) {
                                adinfo.setHasDownload(true);
                            } else {
                                adinfo.setHasDownload(false);
                            }
                            if (material.actionId == 0) {
                                mFaceMaterialList.add(adinfo);
                            } else {
                                mActionMaterialList.add(adinfo);
                            }

                            Message msg = mHandler.obtainMessage(MSG_GETLISTSIZE);
//                            msg.arg1 = count;
                            mHandler.sendMessage(msg);
                        }


                        for (int i = 1; i < mFaceMaterialList.size(); i++) {
                            MaterialInfoItem adinfo = mFaceMaterialList.get(i);

                            String thumbnailurlStr = adinfo.material.thumbnailURL;
                            Bitmap thumbnail = null;
                            try {
                                thumbnail = ApiHttpUrlConnection.getImageBitmap(thumbnailurlStr);
                            } catch (Exception e) {
                                thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.love);
                                reportError("get material thumbnail failed");
                            }
                            adinfo.thumbnail = thumbnail;
                            mFaceMaterialList.set(i, adinfo);

                            Message msg = mHandler.obtainMessage(MSG_LOADTHUMB);
//                            msg.arg1 = count;
                            msg.arg2 = i+1;
                            mHandler.sendMessage(msg);
                        }

                        for (int i = 1; i < mActionMaterialList.size(); i++) {
                            MaterialInfoItem adinfo = mActionMaterialList.get(i);

                            String thumbnailurlStr = adinfo.material.thumbnailURL;
                            Bitmap thumbnail = null;
                            try {
                                thumbnail = ApiHttpUrlConnection.getImageBitmap(thumbnailurlStr);
                            } catch (Exception e) {
                                thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.love);
                                reportError("get material thumbnail failed");
                            }
                            adinfo.thumbnail = thumbnail;
                            mActionMaterialList.set(i, adinfo);

                            Message msg = mHandler.obtainMessage(MSG_LOADTHUMB);
//                            msg.arg1 = count;
                            msg.arg2 = i+1;
                            mHandler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void onFailure(int erroCode, String msg) {
                        if (erroCode == Constants.AUTH_EXPIRED) {
                            makeToast("鉴权信息过期，请重新鉴权!");
                            doAuth();
                        }
                        reportError("fetch material list failed");
                    }
                });
    }

    private void showMaterialLists() {
        if(authorized &&
                mIsFirstFetchMaterialList){
            startGetMaterialList();
            mIsFirstFetchMaterialList = false;
        }
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void closeMaterialsShowLayer() {
        if (mRecyclerViewAdatper != null) {
            mRecyclerViewAdatper.notifyDataSetChanged();
        }
        mRecyclerView.setVisibility(View.INVISIBLE);
    }

}
