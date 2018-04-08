package com.ilinkdownload;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

// in this file we are using Download manager to download the files.
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String downLoadLink = "https://eoimages.gsfc.nasa.gov/images/imagerecords/73000/73751/world.topo.bathy.200407.3x5400x2700.jpg";
    LinearLayout llProgress;
    EditText edtFileLink;
    EditText edtNo1;
    EditText edtNo2;
    Button btnDownLoadFile;
    Button btnAddNum;
    TextView tvSumResult;
    TextView tvProgRes;
    ProgressBar pbFileProg;
    private DownloadManager downloadManager;
    double dl_progress;
    long reference;

    //keys to maintain the stet in bundle on orientation change
    String PB_PROG = "progress";
    String PB_VISIBILITY = "progress_visible";
    String EDT_LINK = "link";
    String EDT_No1 = "no1";
    String EDT_No2 = "no2";
    String SUM_RESULT = "sum_result";
    String REF_ID = "reference";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isStoragePermissionGranted();
        initViews();
        showRetainedStateData(savedInstanceState);
    }

    private void initViews() {
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        llProgress = findViewById(R.id.ll_prog);
        edtFileLink = findViewById(R.id.edt_file_link);
        edtFileLink.setText(downLoadLink);
        edtNo1 = findViewById(R.id.edt_no1);
        edtNo2 = findViewById(R.id.edt_no2);
        btnDownLoadFile = findViewById(R.id.btn_download_file);
        btnAddNum = findViewById(R.id.btn_sum);
        tvSumResult = findViewById(R.id.tv_result);
        tvProgRes = findViewById(R.id.tv_show_progress);
        pbFileProg = findViewById(R.id.pb_file_progress);

        btnDownLoadFile.setOnClickListener(this);
        btnAddNum.setOnClickListener(this);
    }

    //showing the retained data in view after changing the orientation
    private void showRetainedStateData(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.getInt(PB_VISIBILITY) == 1) {
                llProgress.setVisibility(View.VISIBLE);
            } else {
                llProgress.setVisibility(View.GONE);
            }
            double prog = savedInstanceState.getDouble(PB_PROG);
            pbFileProg.setProgress((int) prog);
            tvProgRes.setText("" + prog + " %");
            downLoadLink = savedInstanceState.getString(EDT_LINK);
            edtFileLink.setText(downLoadLink);
            edtNo1.setText(savedInstanceState.getString(EDT_No1));
            edtNo2.setText(savedInstanceState.getString(EDT_No2));
            tvSumResult.setText(savedInstanceState.getString(SUM_RESULT));
            reference = savedInstanceState.getLong(REF_ID);
            if (reference > 0)
                publishProgress(reference);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (llProgress.getVisibility() == View.VISIBLE) {
            outState.putInt(PB_VISIBILITY, 1);
        }
        outState.putDouble(PB_PROG, dl_progress);
        outState.putString(EDT_LINK, edtFileLink.getText().toString());
        outState.putString(EDT_No1, edtNo1.getText().toString());
        outState.putString(EDT_No2, edtNo1.getText().toString());
        outState.putString(SUM_RESULT, tvSumResult.getText().toString());
        outState.putLong(REF_ID, reference);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_download_file:
                startDownLoad();
                break;
            case R.id.btn_sum:
                showSumResult();
                break;
        }
    }

    //method to state the file download
    private void startDownLoad() {
        if (isStoragePermissionGranted()) {
            Utils.hideKeyBoard(this, edtFileLink);
            downLoadLink = edtFileLink.getText().toString();
            if (downLoadLink != null && !downLoadLink.isEmpty()) {
                Uri uri = Uri.parse(downLoadLink);
                //configuring the request
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                request.setTitle("iLink Software");
                request.setDescription("Downloading");
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
                reference = downloadManager.enqueue(request);
                llProgress.setVisibility(View.VISIBLE);
                publishProgress(reference);
            } else {
                Toast.makeText(this, "Please enter a file link to download", Toast.LENGTH_SHORT).show();
            }

        }
    }

    // method to publis the progress on progressbar
    private void publishProgress(final long reference) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;
                while (downloading) {
                    try {
                        DownloadManager.Query q = new DownloadManager.Query();
                        q.setFilterById(reference);
                        Cursor cursor = downloadManager.query(q);
                        cursor.moveToFirst();
                        //getting the byes downloaded so far
                        int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;
                        }
                        dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pbFileProg.setProgress((int) dl_progress);
                                tvProgRes.setText("" + dl_progress + " %");
                            }
                        });
                        Log.d("MainActivity", statusMessage(cursor));
                        cursor.close();
                    } catch (CursorIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }

                }

            }
        }).start();
    }

    private void showSumResult() {
        String no1 = edtNo1.getText().toString();
        String no2 = edtNo2.getText().toString();
        if (no1 != null && no2 != null && !no1.isEmpty() && !no2.isEmpty()) {
            int res = Integer.parseInt(no1) + Integer.parseInt(no2);
            tvSumResult.setText("Sum of " + no1 + " & " + no2 + " is= " + res);
        } else {
            Toast.makeText(this, "Please provide both numbers", Toast.LENGTH_SHORT).show();
        }
    }


    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission granted
        }
    }

    private String statusMessage(Cursor c) {
        String msg = "???";

        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Download failed!";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg = "Download paused!";
                break;

            case DownloadManager.STATUS_PENDING:
                msg = "Download pending!";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg = "Download in progress!";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Download complete!";
                break;

            default:
                msg = "Download is nowhere in sight";
                break;
        }

        return (msg);
    }

}
