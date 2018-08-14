package com.uploadprescription.rspl.uploadprescription;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadPrescription extends AppCompatActivity {


    ImageView imageView;
    File output;
    private String response = "No connection";
    private static final int CAMERA_REQUEST = 1888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        invoiceno();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (isStoragePermissionGranted()) {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

            output = new File(dir, "10.jpeg");
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));
            startActivityForResult(i, CAMERA_REQUEST);
        }

        this.imageView = (ImageView)this.findViewById(R.id.imageView1);
        Button photoButton = (Button) this.findViewById(R.id.button1);
        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String upLoadServerUri = "http://35.194.196.229:8080/Download/FileUpload.jsp";// uploading the file to server
                BackgroundUploader  uploader =new BackgroundUploader(upLoadServerUri,output);
                uploader.execute();
            }

        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }
    class BackgroundUploader extends AsyncTask<Void, Integer, Void> implements DialogInterface.OnCancelListener {
        private String url;
        private File file;
        ProgressDialog progressDialog ;
        public BackgroundUploader(String url, File file) {
            this.url = url;
            this.file = file;
        }
        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(UploadPrescription.this);
            progressDialog.setTitle("Showing progress...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.show();
        }
        @Override
        protected Void doInBackground(Void... v) {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection connection = null;
            String fileName = file.getName();
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                String boundary = "---------------------------boundary";
                String tail = "\r\n--" + boundary + "--\r\n";
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setDoOutput(true);

                String metadataPart = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"metadata\"\r\n\r\n"
                        + "" + "\r\n";

                String fileHeader1 = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\""
                        + fileName + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n"
                        + "Content-Transfer-Encoding: binary\r\n";

                long fileLength = file.length() + tail.length();
                String fileHeader2 = "Content-length: " + fileLength + "\r\n";
                String fileHeader = fileHeader1 + fileHeader2 + "\r\n";
                String stringData = metadataPart + fileHeader;

                long requestLength = stringData.length() + fileLength;
                connection.setRequestProperty("Content-length", "" + requestLength);
                connection.setFixedLengthStreamingMode((int) requestLength);
                connection.connect();

                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                out.writeBytes(stringData);
                out.flush();

                int progress = 0;
                int bytesRead = 0;
                fileLength = file.length();
                byte buf[] = new byte[1024];
                BufferedInputStream bufInput = new BufferedInputStream(new FileInputStream(file));
                while ((bytesRead = bufInput.read(buf)) != -1) {
                    // write output
                    out.write(buf, 0, bytesRead);
                    out.flush();
                    progress += bytesRead;
                    // update progress bar
                    publishProgress((int) ((progress / (float) fileLength) * 100));
                }
                // Write closing boundary and close stream
                out.writeBytes(tail);
                out.flush();
                out.close();
                // Get server response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = "";
                StringBuilder builder = new StringBuilder();
                while((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                Log.d("*******", String.valueOf(connection.getResponseCode()));
                response = String.valueOf(connection.getResponseCode());
                Log.d("************",connection.getResponseMessage());

            } catch (Exception e) {
                e.printStackTrace();
                response = String.valueOf(e);
                // Exception
            } finally {
                if (connection != null) connection.disconnect();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // updating progress bar value
            progressDialog.setProgress(progress[0]);
            Log.d("*********",String.valueOf(progress[0])+" %");
            // Log.d("********", String.valueOf(progress[0]));
        }
        @Override
        protected void onPostExecute(Void v) {
            //  progressDialog.setVisibility(View.GONE);
            if(response.equals("200")) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(),"Prescription Uploaded",Toast.LENGTH_LONG).show();

               // LoyaltyApplication.getInstance().setPrescriptionImage("http://35.194.196.229:8080/Download/Image/"+LoyaltyApplication.getInstance().getOrderId()+".jpeg");
                Toast.makeText(getApplicationContext(),response,Toast.LENGTH_LONG).show();
                finish();

            }
            else {
                Toast.makeText(getApplicationContext(),"Please Try Again",Toast.LENGTH_LONG).show();
              //  LoyaltyApplication.getInstance().setPrescriptionImage(String.valueOf(Uri.fromFile(output)));
                progressDialog.dismiss();
                Log.d("*********",response);
                // Toast.makeText(getApplicationContext(),response,Toast.LENGTH_LONG).show();
                finish();
                }

        }

        @Override
        public void onCancel(DialogInterface dialog) {
            cancel(true);
            dialog.dismiss();

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
              /*  Intent i=new Intent(Intent.ACTION_VIEW);

                i.setDataAndType(Uri.fromFile(output), "image/jpeg");
                startActivity(i);
                finish();*/
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.fromFile(output));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //use the bitmap as you like
                imageView.setImageBitmap(bitmap);
                saveImage(bitmap,invoiceno());

            }
            else finish();
        }
           /* Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);*/

    }

    private void saveImage(Bitmap finalBitmap, String image_name) {

        String root =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "/"+image_name+ ".jpeg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {

            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
            {

                Log.v("KKJ","Permission is granted");
                return true;
            } else
            {

                Log.v("kjn","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("jkj","Permission is granted");
            return true;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED)
        {
            Log.v("kjn","Permission: "+permissions[0]+ "was "+grantResults[0]);
            finish();
            Intent intent = new Intent(getApplicationContext(),UploadPrescription.class);
            startActivity(intent);
        }
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
    public String invoiceno() {
        Long Value = System.currentTimeMillis();
        String result = Long.toString(Value);
       // LoyaltyApplication.getInstance().setOrderId(result);
        Log.d("sfkjbsjkvblsdjbvlsdhf",result);
        return result;

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
